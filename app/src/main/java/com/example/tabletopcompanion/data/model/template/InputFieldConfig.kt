package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class InputFieldConfig(
    val id: String,
    val label: String,
    val type: String, // "text", "number"
    @SerializedName("target_variable") val targetVariable: String?, // Made nullable as it might not always target a variable directly
    @SerializedName("target_action_parameter_key") val targetActionParameterKey: String? = null,
    @SerializedName("default_value") val defaultValue: String? = null,
    @SerializedName("display_properties") val displayProperties: Map<String, String>?
)
