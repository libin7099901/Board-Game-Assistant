package com.example.tabletopcompanion.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tabletopcompanion.R
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.model.GameTemplateInfo
import com.example.tabletopcompanion.util.ManifestParsingException
import com.example.tabletopcompanion.util.TemplateManifestParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class TemplateManagerActivity : AppCompatActivity() {

    private lateinit var importTemplateButton: Button
    private lateinit var templatesRecyclerView: RecyclerView
    private lateinit var templateAdapter: TemplateAdapter
    private lateinit var templateRepository: TemplateRepository

    private val importTemplateLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleImportedZip(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_manager)

        templateRepository = TemplateRepository(this)

        importTemplateButton = findViewById(R.id.importTemplateButton)
        templatesRecyclerView = findViewById(R.id.templatesRecyclerView)

        setupRecyclerView()

        importTemplateButton.setOnClickListener {
            importTemplateLauncher.launch("application/zip")
        }

        loadTemplates()
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter(emptyList()) { template ->
            // Handle template deletion
            templateRepository.deleteTemplate(template.id)
            // Also delete the unzipped folder
            val unzippedDir = File(template.unzippedPath)
            if (unzippedDir.exists()) {
                unzippedDir.deleteRecursively()
            }
            // And the copied zip file if it exists
            template.originalZipName?.let {
                val zipDir = File(filesDir, "templates_zip")
                val originalZipFile = File(zipDir, it) // Assuming originalZipName was used for copied file
                 if (originalZipFile.exists()) {
                    originalZipFile.delete()
                }
            }
            loadTemplates()
            Toast.makeText(this, "Template '${template.name}' deleted", Toast.LENGTH_SHORT).show()
        }
        templatesRecyclerView.layoutManager = LinearLayoutManager(this)
        templatesRecyclerView.adapter = templateAdapter
    }

    private fun loadTemplates() {
        val templates = templateRepository.getTemplates()
        templateAdapter.updateTemplates(templates)
    }

    private fun getOriginalFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun handleImportedZip(uri: Uri) {
        val originalFileName = getOriginalFileName(uri) ?: "${UUID.randomUUID()}.zip"

        // 1. Copy to internal storage (recommended)
        val zipsDir = File(filesDir, "templates_zip")
        if (!zipsDir.exists()) zipsDir.mkdirs()
        val copiedZipFile = File(zipsDir, originalFileName)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(copiedZipFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error copying zip file: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Unzip Logic
        val templateId = UUID.randomUUID().toString()
        val unzippedTargetDir = File(filesDir, "unzipped_templates/$templateId")
        if (!unzippedTargetDir.exists()) unzippedTargetDir.mkdirs()

        try {
            unzip(copiedZipFile, unzippedTargetDir) // Use the copied file for unzipping
        } catch (e: Exception) {
            Toast.makeText(this, "Error unzipping: ${e.message}", Toast.LENGTH_LONG).show
            unzippedTargetDir.deleteRecursively() // Clean up failed unzip attempt
            copiedZipFile.delete() // Clean up copied zip
            return
        }

        // 3. Manifest Parsing
        val manifestFile = File(unzippedTargetDir, "game_info.json")
        if (!manifestFile.exists()) {
            Toast.makeText(this, "game_info.json not found in zip.", Toast.LENGTH_LONG).show()
            unzippedTargetDir.deleteRecursively()
            copiedZipFile.delete()
            return
        }

        try {
            val manifestContent = manifestFile.readText()
            val parsedData = TemplateManifestParser.parseManifest(manifestContent)

            val templateInfo = GameTemplateInfo(
                id = templateId,
                name = parsedData.name,
                description = parsedData.description,
                version = parsedData.version,
                unzippedPath = unzippedTargetDir.absolutePath,
                originalZipName = originalFileName // Store the name of the copied zip
            )
            templateRepository.addTemplate(templateInfo)
            loadTemplates()
            Toast.makeText(this, "Template '${parsedData.name}' imported successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: ManifestParsingException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            unzippedTargetDir.deleteRecursively()
            copiedZipFile.delete()
        } catch (e: Exception) { // Catch other general exceptions
            Toast.makeText(this, "An unexpected error occurred during import: ${e.message}", Toast.LENGTH_LONG).show()
            unzippedTargetDir.deleteRecursively()
            copiedZipFile.delete()
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var zipEntry = zis.nextEntry
            while (zipEntry != null) {
                val newFile = File(targetDirectory, zipEntry.name)
                val f = File(targetDirectory, zipEntry.name)
                // Basic path traversal protection
                if (!f.canonicalPath.startsWith(targetDirectory.canonicalPath + File.separator)) {
                    throw SecurityException("Zip path traversal attempt detected: ${zipEntry.name}")
                }

                if (zipEntry.isDirectory) {
                    if (!f.isDirectory && !f.mkdirs()) {
                        throw Exception("Failed to create directory ${f.absolutePath}")
                    }
                } else {
                    // Ensure parent directory exists
                    val parent = f.parentFile
                    if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
                        throw Exception("Failed to create parent directory ${parent.absolutePath}")
                    }
                    FileOutputStream(f).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                zipEntry = zis.nextEntry
            }
        }
    }
}
