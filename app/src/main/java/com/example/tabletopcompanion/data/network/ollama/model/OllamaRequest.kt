package com.example.tabletopcompanion.data.network.ollama.model

import com.google.gson.annotations.SerializedName

data class OllamaRequest(
    @SerializedName("model") val model: String,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("system") val system: String? = null,
    @SerializedName("stream") val stream: Boolean = false,
    @SerializedName("options") val options: Map<String, Any>? = null
)
