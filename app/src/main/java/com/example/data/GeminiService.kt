package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun getAiResponse(userPrompt: String, history: List<ChatHistory> = emptyList()): String {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            "MY_GEMINI_API_KEY"
        }
        if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key.contains("PLACEHOLDER")) {
            return "Gemini API key is not configured. Please add your GEMINI_API_KEY to your Secrets panel/env in AI Studio."
        }

        // Build request contents from history + current prompt
        val contents = mutableListOf<GeminiContent>()
        for (chat in history) {
            val role = if (chat.sender == "user") "user" else "model"
            contents.add(
                GeminiContent(parts = listOf(GeminiPart(text = chat.message)))
            )
        }
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = userPrompt))))

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = "You are the AI companion inside AR, a self-development assistant. AR blends religious/spiritual practice (Namaz, Quran, prayer requests) with modern productivity features (study clock, habits, calendar, diary). Help the user track progress, stay mindful, and coordinate their spiritual and daily life. Keep responses warm, concise, and motivational."
                )
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction
        )

        return try {
            val response = apiService.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response from AI companion. Please check your network and try again."
        } catch (e: Exception) {
            "Error calling Gemini: ${e.localizedMessage}"
        }
    }
}
