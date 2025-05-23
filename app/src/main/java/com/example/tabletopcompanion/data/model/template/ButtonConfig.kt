package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class ButtonConfig(
    val id: String,
    val text: String,
    @SerializedName("action_id") val actionId: String,
    @SerializedName("display_properties") val displayProperties: Map<String, String>?
)
