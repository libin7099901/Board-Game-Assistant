package com.example.tabletopcompanion.features.templatemanagement

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.model.GameTemplateMetadata
import com.example.tabletopcompanion.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val templateRepository = TemplateRepository(application)

    private val _templates = MutableStateFlow<List<GameTemplateMetadata>>(emptyList())
    val templates: StateFlow<List<GameTemplateMetadata>> = _templates.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    init {
        viewModelScope.launch {
            templateRepository.getTemplateMetadataListFlow().collect {
                _templates.value = it
            }
        }
    }

    fun importTemplate(uri: Uri) {
        viewModelScope.launch {
            val result = templateRepository.importTemplateFromUri(uri)
            if (result is Result.Error) {
                _importError.value = result.exception.message ?: "Failed to import template."
                println("Error importing template: ${result.exception.message}")
            } else {
                _importError.value = null // Clear previous error
            }
        }
    }

    fun clearImportError() {
        _importError.value = null
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            val result = templateRepository.deleteTemplate(templateId)
            if (result is Result.Error) {
                _deleteError.value = result.exception.message ?: "Failed to delete template."
                println("Error deleting template: ${result.exception.message}")
            } else {
                _deleteError.value = null // Clear previous error
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }
}
