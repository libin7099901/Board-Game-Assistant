package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class PhaseTurnDefinition(
    val id: String,
    val name: String,
    val order: Int,
    val conditions: String?,
    @SerializedName("player_actions") val playerActions: List<String>? // List of action IDs
)
