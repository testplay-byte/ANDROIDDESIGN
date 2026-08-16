package com.confused.agenttech.agent.tools

import kotlinx.serialization.Serializable

/**
 * Input to a tool — a free-form map of named arguments.
 *
 * The map values are restricted to primitives JSON-encodes natively
 * (String / Int / Long / Float / Double / Boolean), so the runtime can
 * serialize the whole map to a JSON string for persistence.
 */
@Serializable
data class ToolInput(
    val arguments: Map<String, String> = emptyMap(),
) {
    fun require(key: String): String =
        arguments[key] ?: error("Missing required argument '$key'")

    fun optional(key: String): String? = arguments[key]
}
