package com.example.tabletopcompanion.data.network.ollama

import com.example.tabletopcompanion.data.network.ollama.model.OllamaRequest
import com.example.tabletopcompanion.data.network.ollama.model.OllamaResponse
import retrofit2.Response // Important: Use retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("api/generate") // Ollama's non-streaming endpoint
    suspend fun generate(@Body request: OllamaRequest): Response<OllamaResponse>
}
