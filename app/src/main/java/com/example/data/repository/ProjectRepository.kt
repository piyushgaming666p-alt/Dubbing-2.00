package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectTypeConverters
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepository(context: Context) {
    private val projectDao: ProjectDao = AppDatabase.getDatabase(context).projectDao()
    private val converters = ProjectTypeConverters()

    val projectsFlow: Flow<List<Project>> = projectDao.getAllProjectsFlow()
        .map { entities -> entities.map { it.toDomain(converters) } }
        .flowOn(Dispatchers.IO)

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)?.toDomain(converters)
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val updated = project.copy(lastModifiedAt = System.currentTimeMillis())
        projectDao.insertOrUpdateProject(updated.toEntity(converters))
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }
}
