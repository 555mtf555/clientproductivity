package com.example.clientproductivity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clientproductivity.data.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<BackupEvent>()
    val events = _events.asSharedFlow()

    fun exportData(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportData()
                onJsonReady(json)
                _events.emit(BackupEvent.Success("Data exported successfully"))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Error("Export failed: ${e.message}"))
            }
        }
    }

    fun importData(jsonData: String) {
        viewModelScope.launch {
            try {
                repository.importData(jsonData)
                _events.emit(BackupEvent.Success("Data imported successfully"))
            } catch (e: Exception) {
                _events.emit(BackupEvent.Error("Import failed: ${e.message}"))
            }
        }
    }

    sealed class BackupEvent {
        data class Success(val message: String) : BackupEvent()
        data class Error(val message: String) : BackupEvent()
    }
}
