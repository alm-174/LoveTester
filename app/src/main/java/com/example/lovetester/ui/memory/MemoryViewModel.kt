package com.example.lovetester.ui.memory

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.example.lovetester.data.local.database.MemoryEntity
import com.example.lovetester.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


sealed interface MemoryUiState {

    data object Loading : MemoryUiState

    data class Success(
        val memories: List<MemoryEntity>
    ) : MemoryUiState

    data class Error(
        val message: String
    ) : MemoryUiState
}


sealed interface MemorySaveState {
    data object Idle : MemorySaveState
    data object Saving : MemorySaveState
    data object Success : MemorySaveState
    data object Error : MemorySaveState
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: MemoryRepository
) : ViewModel() {

    val uiState: StateFlow<MemoryUiState> =
        repository.observeMemories()
            .map<List<MemoryEntity>, MemoryUiState> { memories ->
                MemoryUiState.Success(memories)
            }
            .catch { exception ->
                emit(
                    MemoryUiState.Error(
                        exception.message ?: "Cannot load memories"
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MemoryUiState.Loading
            )

    private val _saveState = MutableStateFlow<MemorySaveState>(MemorySaveState.Idle)
    val saveState = _saveState.asStateFlow()

    fun saveMemory(title: String, date: String, imageUri: Uri) {
        if (_saveState.value == MemorySaveState.Saving ||
            _saveState.value == MemorySaveState.Success
        ) return

        _saveState.value = MemorySaveState.Saving
        viewModelScope.launch {
            try {
                repository.addMemory(title, date, imageUri)
                _saveState.value = MemorySaveState.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _saveState.value = MemorySaveState.Error
            }
        }
    }

    fun clearSaveResult() {
        if (_saveState.value != MemorySaveState.Saving) {
            _saveState.value = MemorySaveState.Idle
        }
    }
}
