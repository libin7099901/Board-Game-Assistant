package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class IndicatorConfig(
    val id: String,
    val label: String,
    @SerializedName("initial_value") val initialValue: String,
    @SerializedName("update_rules") val updateRules: String?,
    @SerializedName("display_properties") val displayProperties: Map<String, String>?,
    @SerializedName("value_source_type") val valueSourceType: String? = null, // e.g., "initial", "variable"
    @SerializedName("value_source_id") val valueSourceId: String? = null // e.g., the ID of the game variable
)
