package com.example.tabletopcompanion.features.roommanagement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tabletopcompanion.data.TemplateRepository
import com.example.tabletopcompanion.data.network.ollama.OllamaService

class RoomViewModelFactory(
    private val application: Application,
    private val templateRepository: TemplateRepository,
    private val ollamaService: OllamaService // Added OllamaService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass OllamaService to RoomViewModel constructor
            return RoomViewModel(application, templateRepository, ollamaService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
