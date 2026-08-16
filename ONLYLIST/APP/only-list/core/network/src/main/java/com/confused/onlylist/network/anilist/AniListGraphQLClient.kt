package com.confused.onlylist.network.anilist

import com.confused.onlylist.common.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * AniList GraphQL client — POSTs queries to https://graphql.anilist.co.
 * Per CORE_RULES §14: rate-limited (target 60/min), single-flight, respects Retry-After.
 * Per R-2: AniList is POST-only, no HTTP caching, must cache in Room.
 */
class AniListGraphQLClient(
    private val tokenProvider: suspend () -> String? = { null },
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(this@AniListGraphQLClient.json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        defaultRequest {
            url(AniListConfig.API_URL)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
        }
    }

    /**
     * Executes a GraphQL query.
     * @param query the GraphQL query string
     * @param variables the query variables (key → value)
     * @return the response data as a JsonObject, or an exception on error
     */
    suspend fun query(
        query: String,
        variables: Map<String, Any?> = emptyMap(),
    ): Result<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val token = tokenProvider()
            val requestBody = buildJsonObject {
                put("query", query)
                if (variables.isNotEmpty()) {
                    put("variables", buildJsonObject {
                        variables.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Number -> put(key, value)
                                is Boolean -> put(key, value)
                                null -> put(key, JsonNull)
                                else -> put(key, JsonPrimitive(value.toString()))
                            }
                        }
                    })
                }
            }

            Logger.d("AniList", "GraphQL query: ${query.take(60).replace("\n", " ")}...")

            val response: JsonObject = client.post {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
                setBody(requestBody.toString())
            }.body()

            // Check for GraphQL errors
            if (response.containsKey("errors")) {
                val errors = response["errors"].toString()
                Logger.w("AniList", "GraphQL errors: $errors")
                Result.failure(GraphQLException(errors))
            } else {
                Result.success(response["data"]?.jsonObject ?: JsonObject(emptyMap()))
            }
        } catch (e: Exception) {
            Logger.w("AniList", "GraphQL request failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun close() = client.close()
}

class GraphQLException(message: String) : Exception(message)
