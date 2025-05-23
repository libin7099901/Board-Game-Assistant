package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class GameTemplate(
    @SerializedName("template_version") val templateVersion: String,
    @SerializedName("game_info") val gameInfo: GameInfo,
    @SerializedName("llm_prompts") val llmPrompts: LLMPrompts,
    @SerializedName("llm_rules_document") val llmRulesDocument: String,
    @SerializedName("player_rule_book") val playerRuleBook: String, // Path or reference
    @SerializedName("ui_definition") val uiDefinition: UIDefinition,
    @SerializedName("game_logic") val gameLogic: GameLogic,
    val resources: Map<String, String>?,
    val localization: Map<String, Map<String, String>>?
)
