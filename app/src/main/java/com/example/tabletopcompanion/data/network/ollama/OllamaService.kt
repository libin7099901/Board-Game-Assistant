package com.example.tabletopcompanion.data.network.ollama

import com.example.tabletopcompanion.data.network.ollama.model.OllamaRequest
import com.example.tabletopcompanion.data.network.ollama.model.OllamaResponse
import com.example.tabletopcompanion.util.Result // Your Result class
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OllamaService {
    private val ollamaApi: OllamaApi

    // TODO: Make baseUrl configurable later via settings
    private val baseUrl = "http://10.0.2.2:11434/" // Default for Android emulator

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Or Level.BASIC for less verbosity
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Example timeout
            .readTimeout(60, TimeUnit.SECONDS)    // Example timeout
            .writeTimeout(60, TimeUnit.SECONDS)   // Example timeout
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        ollamaApi = retrofit.create(OllamaApi::class.java)
    }

    suspend fun generateResponse(
        model: String,
        prompt: String,
        systemPrompt: String? = null,
        options: Map<String, Any>? = null
    ): Result<OllamaResponse> {
        return try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                system = systemPrompt,
                options = options,
                stream = false // Explicitly false for this function
            )
            val response = ollamaApi.generate(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.Error(Exception("Ollama API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
