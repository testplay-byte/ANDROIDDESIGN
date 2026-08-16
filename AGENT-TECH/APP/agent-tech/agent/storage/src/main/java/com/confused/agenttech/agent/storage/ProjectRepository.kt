package com.confused.agenttech.agent.storage

import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.dao.ProjectDao
import com.confused.agenttech.database.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * ProjectRepository — CRUD for projects.
 * Each project corresponds to a SAF-scoped folder the agent operates within.
 */
class ProjectRepository(private val dao: ProjectDao) {

    fun observeAll(): Flow<List<ProjectEntity>> = dao.observeAll()

    suspend fun getById(id: String): ProjectEntity? = dao.getById(id)

    suspend fun count(): Int = dao.count()

    suspend fun create(name: String, folderUri: String): ProjectEntity {
        val now = System.currentTimeMillis()
        val id = "proj_${now}_${(0..9999).random().toString().padStart(4, '0')}"
        val project = ProjectEntity(
            id = id,
            name = name,
            folderUri = folderUri,
            createdAt = now,
            lastActiveAt = now,
        )
        dao.upsert(project)
        Logger.i("ProjectRepo", "Created project $id ($name) → $folderUri")
        return project
    }

    suspend fun rename(id: String, newName: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(name = newName))
    }

    suspend fun touch(id: String) {
        dao.touch(id, System.currentTimeMillis())
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        Logger.i("ProjectRepo", "Deleted project $id")
    }
}
