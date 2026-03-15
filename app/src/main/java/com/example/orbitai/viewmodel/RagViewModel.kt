package com.example.orbitai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.rag.RagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RagRepository(application)

    val documents: StateFlow<List<RagDocument>> = repository.documents

    fun addDocument(uri: Uri) {
        repository.addDocument(uri)   // already launches on IO inside repository
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(id)
        }
    }
}
