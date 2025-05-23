package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class VariableDefinition(
    val id: String,
    val type: String, // "integer", "string", "boolean", "list<string>", "list<integer>"
    @SerializedName("initial_value") val initialValue: String, // Store as string, parse based on type
    val scope: String // "global", "player"
)
