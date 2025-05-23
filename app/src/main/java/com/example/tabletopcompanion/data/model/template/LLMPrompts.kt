package com.example.tabletopcompanion.data.model.template

import com.google.gson.annotations.SerializedName

data class LLMPrompts(
    @SerializedName("system_prompt") val systemPrompt: String,
    @SerializedName("user_query_template") val userQueryTemplate: String,
    @SerializedName("model_name") val modelName: String? = null
)
