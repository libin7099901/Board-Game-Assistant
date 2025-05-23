package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class IndicatorConfig(
    val id: String,
    val label: String,
    @SerializedName("initial_value") val initialValue: String,
    @SerializedName("update_rules") val updateRules: String?,
    @SerializedName("display_properties") val displayProperties: Map<String, String>?
)
