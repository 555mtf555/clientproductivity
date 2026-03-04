package com.example.clientproductivity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clientproductivity.data.ClientRepository
import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(private val repository: ClientRepository) : ViewModel() {

    fun getClients(): Flow<List<ClientEntity>> = repository.getClients()

    fun getClient(id: Long): Flow<ClientEntity?> = repository.getClient(id)

    fun getProject(id: Long): Flow<ProjectEntity?> = repository.getProject(id)

    fun addClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.addClient(client)
        }
    }

    fun updateClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.updateClient(client)
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun getProjects(clientId: Long): Flow<List<ProjectEntity>> = repository.getProjects(clientId)

    fun addProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.addProject(project)
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    fun getAllProjects(): Flow<List<ProjectEntity>> = repository.getAllProjects()

    fun getAllClients(): Flow<List<ClientEntity>> = repository.getAllClients()

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun getActiveProjectCount(clientId: Long): Flow<Int> {
        return repository.getProjects(clientId).combine(repository.getClients()) { projects, _ ->
            projects.size
        }
    }
}