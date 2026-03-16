package com.example.orbitai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.SpaceRepository
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SpacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpaceRepository(application)

    val spaces: StateFlow<List<Space>> = repository.spaces

    fun observeDocumentsInSpace(spaceId: String): Flow<List<RagDocument>> =
        repository.observeDocumentsInSpace(spaceId)

    fun createSpace(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createSpace(name)
        }
    }

    fun deleteSpace(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSpace(id)
        }
    }

    fun addDocumentToSpace(uri: Uri, spaceId: String) {
        repository.addDocumentToSpace(uri, spaceId)
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(id)
        }
    }
}
