package com.confused.agenttech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A configured LLM provider (OpenAI / Anthropic / Ollama / LM Studio / Custom).
 *
 * Per user request: API key is stored in PLAINTEXT (NOT encrypted). This app is
 * a local-first developer tool — the threat model assumes the device is trusted.
 * Future versions could add EncryptedSharedPreferences for the key.
 *
 * `baseUrl` defaults to the provider's official URL but is configurable for
 * OpenAI-compatible endpoints (Ollama, LM Studio, OpenRouter, etc.).
 */
@Entity(
    tableName = "provider",
    indices = [Index(value = ["name"], unique = true)],
)
data class ProviderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val apiKey: String,           // PLAINTEXT per user request
    val baseUrl: String,
    val modelName: String,
    val contextWindow: Long = 8_192L,
    val maxTokens: Long = 4_096L,
    val temperature: Float = 0.7f,
    val inputPricePer1K: Float = 0f,   // user's currency per 1K input tokens
    val outputPricePer1K: Float = 0f, // user's currency per 1K output tokens
    val isActive: Boolean = false,
    val createdAt: Long = 0L,
)
