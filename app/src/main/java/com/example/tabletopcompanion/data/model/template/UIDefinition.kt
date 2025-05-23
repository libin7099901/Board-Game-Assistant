package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class UIDefinition(
    val indicators: List<IndicatorConfig>,
    val buttons: List<ButtonConfig>,
    @SerializedName("input_fields") val inputFields: List<InputFieldConfig>,
    @SerializedName("custom_components") val customComponents: List<CustomComponentConfig>?
)
