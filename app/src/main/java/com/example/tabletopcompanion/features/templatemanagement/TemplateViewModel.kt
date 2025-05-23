package com.example.tabletopcompanion.features.templatemanagement

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class TemplateViewModel : ViewModel() {
    // Observable list of template names
    val templates = mutableStateListOf("Template A", "Template B", "Template C (Default)")

    fun importTemplate(name: String) {
        if (!templates.contains(name)) {
            templates.add(name)
        }
    }

    fun deleteTemplate(name: String) {
        templates.remove(name)
    }
}
