package com.example.tabletopcompanion.data.model

data class GameTemplateMetadata(
    val templateId: String,
    val templateVersion: String,
    val gameName: String,
    val author: String?,
    val description: String?,
    val minPlayers: Int,
    val maxPlayers: Int
)
