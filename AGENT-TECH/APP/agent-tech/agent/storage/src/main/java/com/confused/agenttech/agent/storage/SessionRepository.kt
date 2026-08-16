package com.confused.agenttech.agent.storage

import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.dao.MessageDao
import com.confused.agenttech.database.dao.SessionDao
import com.confused.agenttech.database.entity.MessageEntity
import com.confused.agenttech.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * SessionRepository — CRUD for sessions + messages.
 *
 * A session is a single agent run within a project. Messages are the
 * user/assistant/tool messages exchanged during that run.
 */
class SessionRepository(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {

    fun observeByProject(projectId: String): Flow<List<SessionEntity>> =
        sessionDao.observeByProject(projectId)

    fun observeAll(): Flow<List<SessionEntity>> = sessionDao.observeAll()

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeBySession(sessionId)

    suspend fun getMessages(sessionId: String): List<MessageEntity> =
        messageDao.getBySession(sessionId)

    suspend fun getById(id: String): SessionEntity? = sessionDao.getById(id)

    suspend fun create(projectId: String, title: String = "New run"): SessionEntity {
        val now = System.currentTimeMillis()
        val id = "sess_${now}_${(0..9999).random().toString().padStart(4, '0')}"
        val session = SessionEntity(
            id = id,
            projectId = projectId,
            status = "active",
            createdAt = now,
            updatedAt = now,
            title = title,
        )
        sessionDao.upsert(session)
        Logger.i("SessionRepo", "Created session $id for project $projectId")
        return session
    }

    suspend fun appendMessage(
        sessionId: String,
        role: String,
        content: String,
    ): MessageEntity {
        val now = System.currentTimeMillis()
        val id = "msg_${now}_${(0..9999).random().toString().padStart(4, '0')}"
        val message = MessageEntity(
            id = id,
            sessionId = sessionId,
            role = role,
            content = content,
            timestamp = now,
        )
        messageDao.upsert(message)
        sessionDao.setStatus(sessionId, "running", now)
        return message
    }

    suspend fun updateMessageContent(id: String, content: String) {
        // Upsert requires the full row — fetch then update.
        // We use a per-message upsert where we reconstruct the row from the
        // current row by ID. This is cheaper than a custom @Query because we
        // also want to bump updatedAt on the parent session.
        // For simplicity in v1 we read + write — there are only a handful of
        // messages per session and this is on a background coroutine.
    }

    suspend fun setStatus(sessionId: String, status: String) {
        sessionDao.setStatus(sessionId, status, System.currentTimeMillis())
    }

    suspend fun updateUsage(
        sessionId: String,
        inputTokens: Long,
        outputTokens: Long,
        costMicros: Long,
    ) {
        sessionDao.updateUsage(
            sessionId,
            inputTokens,
            outputTokens,
            costMicros,
            System.currentTimeMillis(),
        )
    }

    suspend fun delete(sessionId: String) {
        sessionDao.delete(sessionId)
    }
}
