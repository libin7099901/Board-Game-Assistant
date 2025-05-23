package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class ActionDefinition(
    val id: String,
    val name: String?,
    val description: String?,
    @SerializedName("trigger_conditions") val triggerConditions: String?,
    val effects: List<EffectDefinition>
)
