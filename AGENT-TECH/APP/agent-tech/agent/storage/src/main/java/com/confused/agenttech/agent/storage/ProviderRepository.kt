package com.confused.agenttech.agent.storage

import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.dao.ProviderDao
import com.confused.agenttech.database.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

/**
 * ProviderRepository — CRUD for LLM providers (OpenAI / Anthropic / Ollama /
 * LM Studio / any OpenAI-compatible endpoint).
 *
 * Only one provider may be `isActive = true` at a time; calling [setActive]
 * flips all others off.
 */
class ProviderRepository(private val dao: ProviderDao) {

    fun observeAll(): Flow<List<ProviderEntity>> = dao.observeAll()

    fun observeActive(): Flow<ProviderEntity?> = dao.observeActive()

    suspend fun getById(id: String): ProviderEntity? = dao.getById(id)

    suspend fun getActive(): ProviderEntity? = dao.getActive()

    suspend fun upsert(provider: ProviderEntity) {
        dao.upsert(provider)
        Logger.i("ProviderRepo", "Saved provider ${provider.name} (${provider.modelName})")
    }

    suspend fun setActive(id: String) {
        dao.setActive(id)
        Logger.i("ProviderRepo", "Activated provider $id")
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        Logger.i("ProviderRepo", "Deleted provider $id")
    }

    /** Built-in defaults — used to pre-seed the Provider config screen options. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.getAll().isNotEmpty()) return
        val now = System.currentTimeMillis()
        listOf(
            ProviderEntity(
                id = "prov_openai",
                name = "OpenAI",
                apiKey = "",
                baseUrl = "https://api.openai.com/v1",
                modelName = "gpt-4o",
                contextWindow = 128_000L,
                maxTokens = 4_096L,
                temperature = 0.7f,
                inputPricePer1K = 0.005f,
                outputPricePer1K = 0.015f,
                isActive = true,
                createdAt = now,
            ),
            ProviderEntity(
                id = "prov_anthropic",
                name = "Anthropic",
                apiKey = "",
                baseUrl = "https://api.anthropic.com/v1",
                modelName = "claude-3-5-sonnet-20241022",
                contextWindow = 200_000L,
                maxTokens = 4_096L,
                temperature = 0.7f,
                inputPricePer1K = 0.003f,
                outputPricePer1K = 0.015f,
                isActive = false,
                createdAt = now + 1,
            ),
            ProviderEntity(
                id = "prov_ollama",
                name = "Ollama (local)",
                apiKey = "ollama",
                baseUrl = "http://localhost:11434/v1",
                modelName = "llama3.1:8b",
                contextWindow = 8_192L,
                maxTokens = 2_048L,
                temperature = 0.7f,
                inputPricePer1K = 0f,
                outputPricePer1K = 0f,
                isActive = false,
                createdAt = now + 2,
            ),
        ).forEach { dao.upsert(it) }
        Logger.i("ProviderRepo", "Seeded default providers")
    }
}
