package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class EffectDefinition(
    val type: String, // e.g., "MODIFY_VARIABLE", "SHOW_MESSAGE", "NEXT_TURN"
    val target: String?, // e.g., variable_id for MODIFY_VARIABLE
    @SerializedName("value_expression") val valueExpression: String? // e.g., "current_value + 1", or a message string
)
