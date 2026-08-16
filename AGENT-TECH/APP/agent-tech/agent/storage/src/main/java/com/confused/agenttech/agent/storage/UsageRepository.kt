package com.confused.agenttech.agent.storage

import com.confused.agenttech.database.dao.UsageLogDao
import com.confused.agenttech.database.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * UsageRepository — tracks per-request token usage + cost, and exposes
 * aggregate stats for the Usage screen.
 *
 * Cost is stored in micros (1e-6 of the user's currency unit) to avoid
 * float precision loss across long runs.
 */
class UsageRepository(private val dao: UsageLogDao) {

    fun observeRecent(limit: Int = 200): Flow<List<UsageLogEntity>> = dao.observeRecent(limit)

    fun observeByProvider(providerId: String): Flow<List<UsageLogEntity>> =
        dao.observeByProvider(providerId)

    fun observeBySession(sessionId: String): Flow<List<UsageLogEntity>> =
        dao.observeBySession(sessionId)

    fun observeTotalInputTokens(): Flow<Long> = dao.observeTotalInputTokens()

    fun observeTotalOutputTokens(): Flow<Long> = dao.observeTotalOutputTokens()

    fun observeTotalCostMicros(): Flow<Long> = dao.observeTotalCostMicros()

    fun observeRunCount(): Flow<Int> = dao.observeRunCount()

    suspend fun log(
        providerId: String,
        sessionId: String,
        inputTokens: Long,
        outputTokens: Long,
        costMicros: Long,
    ): UsageLogEntity {
        val now = System.currentTimeMillis()
        val id = "usage_${now}_${(0..9999).random().toString().padStart(4, '0')}"
        val entry = UsageLogEntity(
            id = id,
            providerId = providerId,
            sessionId = sessionId,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            costMicros = costMicros,
            timestamp = now,
        )
        dao.upsert(entry)
        return entry
    }
}
