package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class InputFieldConfig(
    val id: String,
    val label: String,
    val type: String, // "text", "number"
    @SerializedName("target_variable") val targetVariable: String,
    @SerializedName("display_properties") val displayProperties: Map<String, String>?
)
