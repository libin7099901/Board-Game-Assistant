package com.example.tabletopcompanion.features.templatemanagement

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.model.GameTemplateMetadata
import com.example.tabletopcompanion.util.Result
import kotlinx.coroutines.launch

// For now, we'll instantiate the repository directly.
// In a larger app, use dependency injection (e.g., Hilt).
class TemplateViewModel(private val templateRepository: TemplateRepository = TemplateRepository()) : ViewModel() {

    val templates = mutableStateListOf<GameTemplateMetadata>()
    // TODO: Add a StateFlow or LiveData for error messages to be observed by the UI

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        templates.clear()
        templates.addAll(templateRepository.getTemplateMetadataList())
    }

    fun importTemplate(context: Context, uri: Uri) {
        viewModelScope.launch {
            when (val result = templateRepository.importTemplateFromUri(context, uri)) {
                is Result.Success -> {
                    // Check if template with same ID already exists to prevent duplicates if re-importing
                    val existingIndex = templates.indexOfFirst { it.templateId == result.data.templateId }
                    if (existingIndex != -1) {
                        templates[existingIndex] = result.data // Update if exists
                    } else {
                        templates.add(result.data)
                    }
                }
                is Result.Error -> {
                    // TODO: Handle error (e.g., log, show toast via StateFlow)
                    println("Error importing template: ${result.exception.message}")
                }
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            when (templateRepository.deleteTemplate(templateId)) {
                is Result.Success -> {
                    templates.removeAll { it.templateId == templateId }
                }
                is Result.Error -> {
                    // TODO: Handle error
                    println("Error deleting template: ${result.exception.message}")
                }
            }
        }
    }
}
