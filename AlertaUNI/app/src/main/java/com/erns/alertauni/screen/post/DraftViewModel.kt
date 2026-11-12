package com.erns.alertauni.screen.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.data.repository.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftViewModel @Inject constructor(private val repository: DraftRepository) : ViewModel() {
    val drafts: StateFlow<List<DraftEntity>> =
        repository.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun save(draft: DraftEntity) = viewModelScope.launch { repository.save(draft) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
