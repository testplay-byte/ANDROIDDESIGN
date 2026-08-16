package com.confused.agenttech.agent.llm

import com.confused.agenttech.common.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.coroutineContext

/**
 * OpenAiCompatibleProvider — implements [LlmProvider] via the OpenAI
 * /v1/chat/completions streaming API.
 *
 * Works for:
 *   - OpenAI           (https://api.openai.com/v1)
 *   - Anthropic        (via OpenAI-compatible endpoint)
 *   - Ollama           (http://localhost:11434/v1)
 *   - LM Studio        (http://localhost:1234/v1)
 *   - OpenRouter, Groq, Together, etc.
 *
 * Sends `stream: true` and parses SSE `data:` lines incrementally.
 */
class OpenAiCompatibleProvider(
    override val displayName: String,
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Float = 0.7f,
    private val maxTokens: Long = 4096L,
) : LlmProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun stream(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val body = buildJsonObject {
                put("model", modelName)
                put("stream", true)
                put("temperature", temperature.toDouble())
                put("max_tokens", maxTokens)
                put(
                    "messages",
                    JsonArray(
                        messages.map { msg ->
                            buildJsonObject {
                                put("role", msg.role)
                                put("content", msg.content)
                            }
                        }
                    )
                )
            }
            Logger.d("Llm", "→ POST $url model=$modelName")

            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(body.toString())
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsChannel().readUTF8LineWhileAvailable()
                throw RuntimeException("HTTP ${response.status.value}: $errorText")
            }

            val channel = response.bodyAsChannel()
            val sb = StringBuilder()
            while (!channel.isClosedForRead) {
                coroutineContext.ensureActive()
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val delta = parseDelta(payload)
                if (delta.isNotEmpty()) {
                    sb.append(delta)
                    onToken(delta)
                }
            }
            sb.toString()
        }.onFailure { Logger.w("Llm", "stream failed: ${it.message}", it) }
    }

    /** Parse the `choices[0].delta.content` field out of one SSE payload. */
    private fun parseDelta(payload: String): String {
        return try {
            val element = json.parseToJsonElement(payload) as JsonObject
            val choices = element["choices"] as? JsonArray ?: return ""
            val first = choices.firstOrNull() as? JsonObject ?: return ""
            val delta = first["delta"] as? JsonObject ?: return ""
            val content = delta["content"] as? kotlinx.serialization.json.JsonPrimitive ?: return ""
            content.content
        } catch (e: Exception) {
            ""
        }
    }
}

/** Drains whatever's available on the channel into a string (used for error bodies). */
private suspend fun io.ktor.utils.io.ByteReadChannel.readUTF8LineWhileAvailable(): String {
    val sb = StringBuilder()
    while (!isClosedForRead) {
        val line = readUTF8Line() ?: break
        sb.appendLine(line)
        if (sb.length > 4096) break
    }
    return sb.toString()
}
