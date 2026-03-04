package com.example.clientproductivity.data

import com.example.clientproductivity.data.dao.ClientDao
import com.example.clientproductivity.data.dao.ProjectDao
import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ClientRepository(
    private val clientDao: ClientDao,
    private val projectDao: ProjectDao
) {
    fun getClients(): Flow<List<ClientEntity>> = clientDao.getAllClients()

    fun getClient(id: Long): Flow<ClientEntity?> = clientDao.getClientById(id)

    suspend fun addClient(client: ClientEntity) = clientDao.insert(client)

    suspend fun updateClient(client: ClientEntity) = clientDao.update(client)

    suspend fun deleteClient(client: ClientEntity) = clientDao.delete(client)

    fun getProjects(clientId: Long): Flow<List<ProjectEntity>> =
        projectDao.getProjectsForClient(clientId)

    fun getProject(id: Long): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun addProject(project: ProjectEntity) = projectDao.insert(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.update(project)

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getAllClients(): Flow<List<ClientEntity>> = clientDao.getAllClients()

    suspend fun deleteProject(project: ProjectEntity) = projectDao.delete(project)
}