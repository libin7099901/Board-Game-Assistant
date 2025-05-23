package com.example.tabletopcompanion.data.model.template

data class ScoringRuleDefinition(
    val id: String,
    val description: String,
    val items: List<ScoringItem>,
    val formula: String, // e.g., "item1 + item2 * 2"
    val trigger: String // e.g., "end_of_round", "end_of_game"
)
