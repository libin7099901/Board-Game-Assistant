package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class ScoringItem(
    val id: String,
    val name: String,
    @SerializedName("value_source") val valueSource: String // e.g., variable_id, "player_input:<input_field_id>"
)
