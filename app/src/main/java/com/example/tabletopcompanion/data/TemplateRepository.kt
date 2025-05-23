package com.example.tabletopcompanion.data

import android.app.Application
import android.net.Uri
import com.example.tabletopcompanion.data.database.AppDatabase
import com.example.tabletopcompanion.data.database.toDomainModel
import com.example.tabletopcompanion.data.database.toEntityModel
import com.example.tabletopcompanion.data.model.GameTemplateMetadata
import com.example.tabletopcompanion.data.model.template.GameTemplate
import com.example.tabletopcompanion.util.Result
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.UUID

class TemplateRepository(application: Application) {

    private val gson = Gson() // Reusable Gson instance
    private val templateDao = AppDatabase.getDatabase(application).gameTemplateMetadataDao()
    private val appContext = application.applicationContext

    fun getTemplateMetadataListFlow(): Flow<List<GameTemplateMetadata>> =
        templateDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    suspend fun importTemplateFromUri(uri: Uri): Result<GameTemplateMetadata> = withContext(Dispatchers.IO) {
        var templateIdInternal: String? = null // Declare templateId here to be accessible in catch
        var destinationDir: File? = null // To be used in catch block for cleanup

        try {
            val templateId = UUID.randomUUID().toString()
            templateIdInternal = templateId

            val templatesDir = File(appContext.filesDir, "templates_unzipped")
            templatesDir.mkdirs()
            destinationDir = File(templatesDir, templateId)
            destinationDir.mkdirs()

            // Copy and Unzip
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipFileInMemory = File(appContext.cacheDir, "temp_${templateId}.zip") // Use app context for cache
                FileOutputStream(zipFileInMemory).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                ZipFile(zipFileInMemory).use { zip ->
                    zip.entries.asSequence().forEach { entry ->
                        val outputFile = File(destinationDir, entry.name) // Use destinationDir
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
            val manifestFile = File(destinationDir, "template.json") // Use destinationDir
            if (!manifestFile.exists()) {
                destinationDir?.deleteRecursively() // Clean up if manifest is not found
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
                filePath = manifestFile.relativeTo(destinationDir).path, // Use destinationDir
                unzippedDirectoryPath = destinationDir.absolutePath // Use destinationDir
            )

            // Store Metadata in Database
            val entity = metadata.toEntityModel()
            templateDao.insert(entity)
            Result.Success(metadata)

        } catch (e: Exception) {
            // Clean up partially created directory if an error occurs
            destinationDir?.deleteRecursively() // Use the destinationDir variable
            Result.Error(e)
        }
    }

    suspend fun getParsedGameTemplate(templateId: String): Result<GameTemplate> = withContext(Dispatchers.IO) {
        try {
            val entity = templateDao.getById(templateId)
                ?: return@withContext Result.Error(FileNotFoundException("Metadata not found for $templateId in DB"))

            val manifestFile = File(entity.unzippedDirectoryPath, entity.filePath)
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
            val entity = templateDao.getById(templateId)
            entity?.let {
                File(it.unzippedDirectoryPath).deleteRecursively()
                templateDao.deleteById(templateId)
                Result.Success(Unit)
            } ?: Result.Error(Exception("Template with id $templateId not found in DB"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
