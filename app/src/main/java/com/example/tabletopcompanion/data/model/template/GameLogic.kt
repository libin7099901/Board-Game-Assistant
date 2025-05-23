package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class GameLogic(
    @SerializedName("phases_turns") val phasesTurns: List<PhaseTurnDefinition>,
    val actions: List<ActionDefinition>,
    @SerializedName("scoring_rules") val scoringRules: List<ScoringRuleDefinition>?,
    @SerializedName("state_transitions") val stateTransitions: List<StateTransitionDefinition>?,
    val variables: Map<String, VariableDefinition>?
)
