package com.example.clientproductivity.data.backup

import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import com.example.clientproductivity.data.entity.TaskEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupModel(
    val clients: List<ClientEntity>,
    val projects: List<ProjectEntity>,
    val tasks: List<TaskEntity>
)
