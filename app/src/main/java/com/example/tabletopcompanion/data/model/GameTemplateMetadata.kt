package com.example.tabletopcompanion.data.model

import java.io.Serializable

data class GameTemplateMetadata(
    val templateId: String,
    val templateVersion: String,
    val name: String, // Changed from gameName
    val author: String?,
    val description: String?,
    val playerCountDescription: String, // Changed from minPlayers and maxPlayers
    val filePath: String, // Added
    val unzippedDirectoryPath: String // Added
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
