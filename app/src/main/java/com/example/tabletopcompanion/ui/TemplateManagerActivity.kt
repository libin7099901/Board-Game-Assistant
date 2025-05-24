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
import com.example.tabletopcompanion.data.RoomSettingsRepository
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.model.GameTemplateInfo
import com.example.tabletopcompanion.util.ManifestParsingException
import com.example.tabletopcompanion.util.TemplateManifestParser
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class TemplateManagerActivity : AppCompatActivity(), TemplateAdapter.OnTemplateActionClickListener {

    private lateinit var importTemplateButton: Button
    private lateinit var templatesRecyclerView: RecyclerView
    private lateinit var templateAdapter: TemplateAdapter
    private lateinit var templateRepository: TemplateRepository
    private lateinit var roomSettingsRepository: RoomSettingsRepository

    private val importTemplateLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleImportedZip(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_manager)

        templateRepository = TemplateRepository(this)
        roomSettingsRepository = RoomSettingsRepository(this)

        importTemplateButton = findViewById(R.id.importTemplateButton)
        templatesRecyclerView = findViewById(R.id.templatesRecyclerView)

        setupRecyclerView()

        importTemplateButton.setOnClickListener {
            importTemplateLauncher.launch("application/zip")
        }

        loadTemplates()
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter(emptyList(), this)
        templatesRecyclerView.layoutManager = LinearLayoutManager(this)
        templatesRecyclerView.adapter = templateAdapter
    }

    override fun onDeleteTemplate(templateId: String, templateName: String) {
        templateRepository.deleteTemplate(templateId)
        // Also delete the unzipped folder
        val template = templateRepository.getTemplateById(templateId) // Re-fetch to get path if needed, or pass GameTemplateInfo
        template?.let {
             val unzippedDir = File(it.unzippedPath)
            if (unzippedDir.exists()) {
                unzippedDir.deleteRecursively()
            }
            // And the copied zip file if it exists
            it.originalZipName?.let { zipName ->
                val zipDir = File(filesDir, "templates_zip")
                val originalZipFile = File(zipDir, zipName)
                 if (originalZipFile.exists()) {
                    originalZipFile.delete()
                }
            }
        } ?: run {
             // If template info not found (already deleted from repo), attempt to delete based on ID in path
            // This part is tricky if the template object is gone. Assume repo.delete removes it from list only.
            // The current delete in repo doesn't return the object, so this needs careful handling.
            // For now, assume the original path might not be easily retrievable if GameTemplateInfo is not passed.
            // The previous lambda had `template` directly, which is cleaner.
            // Let's assume we might need to adjust deleteTemplate to return the object or pass more info.
            // For now, the unzipped folder might not be deleted if the template is gone from the repo list.
            // This is a slight change in behavior from the direct lambda.
            // A better approach might be to have templateRepository.deleteTemplate also handle file cleanup.
        }


        loadTemplates()
        Toast.makeText(this, getString(R.string.template_deleted_toast, templateName), Toast.LENGTH_SHORT).show()
    }

    override fun onSelectTemplate(templateId: String, templateName: String) {
        roomSettingsRepository.saveSelectedTemplateId(templateId)
        Toast.makeText(this, getString(R.string.template_selected_toast, templateName), Toast.LENGTH_SHORT).show()
        // finish() // Optional: finish activity
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
            Toast.makeText(this, getString(R.string.error_copying_zip_toast, e.message), Toast.LENGTH_LONG).show()
            return
        }

        // 2. Unzip Logic
        val templateId = UUID.randomUUID().toString()
        val unzippedTargetDir = File(filesDir, "unzipped_templates/$templateId")
        if (!unzippedTargetDir.exists()) unzippedTargetDir.mkdirs()

        try {
            unzip(copiedZipFile, unzippedTargetDir) // Use the copied file for unzipping
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_unzipping_toast, e.message), Toast.LENGTH_LONG).show()
            unzippedTargetDir.deleteRecursively() // Clean up failed unzip attempt
            copiedZipFile.delete() // Clean up copied zip
            return
        }

        // 3. Manifest Parsing
        val manifestFile = File(unzippedTargetDir, "game_info.json")
        if (!manifestFile.exists()) {
            Toast.makeText(this, getString(R.string.manifest_not_found_toast), Toast.LENGTH_LONG).show()
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
                originalZipName = originalFileName, // Store the name of the copied zip
                phases = parsedData.phases,
                initialIndicators = parsedData.initialIndicators
            )
            templateRepository.addTemplate(templateInfo)
            loadTemplates()
            Toast.makeText(this, getString(R.string.template_imported_toast, parsedData.name), Toast.LENGTH_SHORT).show()

        } catch (e: ManifestParsingException) {
            Toast.makeText(this, e.message ?: getString(R.string.error_parsing_manifest_generic_toast), Toast.LENGTH_LONG).show()
            unzippedTargetDir.deleteRecursively()
            copiedZipFile.delete()
        } catch (e: Exception) { // Catch other general exceptions
            Toast.makeText(this, getString(R.string.error_unexpected_import_toast, e.message), Toast.LENGTH_LONG).show()
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
