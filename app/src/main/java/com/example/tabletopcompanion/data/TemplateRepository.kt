package com.example.tabletopcompanion.data

import android.content.Context
import android.net.Uri
import com.example.tabletopcompanion.data.model.GameTemplateMetadata
import com.example.tabletopcompanion.data.model.template.GameTemplate
import com.example.tabletopcompanion.util.Result
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.UUID

class TemplateRepository {

    private val gson = Gson() // Reusable Gson instance
    private val templatesMetadataList = mutableListOf<GameTemplateMetadata>()

    fun getTemplateMetadataList(): List<GameTemplateMetadata> {
        return templatesMetadataList.toList()
    }

    suspend fun importTemplateFromUri(context: Context, uri: Uri): Result<GameTemplateMetadata> = withContext(Dispatchers.IO) {
        var templateIdInternal: String? = null // Declare templateId here to be accessible in catch
        try {
            val templateId = UUID.randomUUID().toString()
            templateIdInternal = templateId // Assign to the outer scope variable
            val cacheDir = File(context.cacheDir, "templates")
            cacheDir.mkdirs()

            val unzippedTemplateDir = File(cacheDir, templateId)
            unzippedTemplateDir.mkdirs()

            // Copy and Unzip
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipFileInMemory = File(context.cacheDir, "temp_${templateId}.zip")
                FileOutputStream(zipFileInMemory).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                ZipFile(zipFileInMemory).use { zip ->
                    zip.entries.asSequence().forEach { entry ->
                        val outputFile = File(unzippedTemplateDir, entry.name)
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outputFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
                zipFileInMemory.delete() // Clean up temp zip
            } ?: return@withContext Result.Error(Exception("Failed to open input stream for URI"))


            // Find Manifest (template.json)
            val manifestFile = File(unzippedTemplateDir, "template.json")
            if (!manifestFile.exists()) {
                unzippedTemplateDir.deleteRecursively() // Clean up if manifest is not found
                return@withContext Result.Error(Exception("template.json not found in zip"))
            }

            // Parse Manifest
            val gameTemplate: GameTemplate = InputStreamReader(manifestFile.inputStream()).use { reader ->
                Gson().fromJson(reader, GameTemplate::class.java)
            }

            // Create Metadata
            val playerCountDesc = when {
                gameTemplate.gameInfo.playerCount.isEmpty() -> "N/A"
                gameTemplate.gameInfo.playerCount.size == 1 -> "Exactly ${gameTemplate.gameInfo.playerCount[0]} player(s)"
                gameTemplate.gameInfo.playerCount[0] == gameTemplate.gameInfo.playerCount[1] -> "Exactly ${gameTemplate.gameInfo.playerCount[0]} players"
                else -> "${gameTemplate.gameInfo.playerCount[0]}-${gameTemplate.gameInfo.playerCount[1]} players"
            }

            val metadata = GameTemplateMetadata(
                templateId = templateId,
                templateVersion = gameTemplate.templateVersion,
                name = gameTemplate.gameInfo.name,
                author = gameTemplate.gameInfo.author,
                description = gameTemplate.gameInfo.description,
                playerCountDescription = playerCountDesc,
                filePath = manifestFile.relativeTo(unzippedTemplateDir).path,
                unzippedDirectoryPath = unzippedTemplateDir.absolutePath
            )

            // Store Metadata
            templatesMetadataList.add(metadata)
            Result.Success(metadata)

        } catch (e: Exception) {
            // Clean up partially created directory if an error occurs
            templateIdInternal?.let { // Only attempt cleanup if templateId was set
                val unzippedTemplateDir = File(File(context.cacheDir, "templates"), it)
                if (unzippedTemplateDir.exists()) {
                    unzippedTemplateDir.deleteRecursively()
                }
            }
            Result.Error(e)
        }
    }

    suspend fun loadGameTemplate(templateId: String): Result<GameTemplate> = withContext(Dispatchers.IO) {
        try {
            val metadata = templatesMetadataList.find { it.templateId == templateId }
                ?: return@withContext Result.Error(FileNotFoundException("Metadata not found for $templateId"))

            val manifestFile = File(metadata.unzippedDirectoryPath, metadata.filePath)
            if (!manifestFile.exists()) {
                return@withContext Result.Error(FileNotFoundException("Template file not found at ${manifestFile.absolutePath}"))
            }

            val jsonContent = manifestFile.readText()
            val gameTemplate = gson.fromJson(jsonContent, GameTemplate::class.java)
            Result.Success(gameTemplate)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val metadataToRemove = templatesMetadataList.find { it.templateId == templateId }
            if (metadataToRemove != null) {
                templatesMetadataList.remove(metadataToRemove)
                val templateDir = File(metadataToRemove.unzippedDirectoryPath)
                if (templateDir.exists()) {
                    templateDir.deleteRecursively()
                }
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Template with id $templateId not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
