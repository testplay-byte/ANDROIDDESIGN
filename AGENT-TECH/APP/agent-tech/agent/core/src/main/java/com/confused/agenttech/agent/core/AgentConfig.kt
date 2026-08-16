package com.confused.agenttech.agent.core

import com.confused.agenttech.database.entity.ProviderEntity

/**
 * AgentConfig — runtime configuration for the agent loop.
 *
 * Defaults:
 *   - maxIterations = 25 (configurable per-session)
 *   - autoApprove = true (per SCREEN-PLAN.md "Auto-Approve toggle — on by default.
 *     All actions are performed in the dedicated project folder.")
 *   - tokenCap / priceCap = 0 (disabled) — set per-run from the Usage screen
 */
data class AgentConfig(
    val provider: ProviderEntity?,
    val maxIterations: Int = 25,
    val autoApprove: Boolean = true,
    val tokenCap: Long = 0L,
    val priceCapMicros: Long = 0L,
)
