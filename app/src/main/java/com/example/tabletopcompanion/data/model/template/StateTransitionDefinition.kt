package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class StateTransitionDefinition(
    @SerializedName("from_state") val fromState: String,
    @SerializedName("to_state") val toState: String,
    @SerializedName("action_id") val actionId: String?,
    val condition: String?
)
