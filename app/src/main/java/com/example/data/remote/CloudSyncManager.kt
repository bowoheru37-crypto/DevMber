package com.example.data.remote

import android.util.Log
import com.example.data.local.ProjectEntity
import com.example.data.local.StudioRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val pendingItemsCount: Int = 0,
    val statusMessage: String = "Cloud Ready (Offline First Active)",
    val isOnline: Boolean = true
)

class CloudSyncManager(private val repository: StudioRepository) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Firestore instance not ready, operating offline: ${e.message}")
        }
    }

    fun syncAll(userId: String) {
        scope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                statusMessage = "Synchronizing with Firestore cloud storage..."
            )

            try {
                // Fetch local unsynced projects once atomically
                val projects = repository.allProjects.first()
                val unsynced = projects.filter { !it.isSynced }
                _syncState.value = _syncState.value.copy(pendingItemsCount = unsynced.size)

                val db = firestore
                if (db != null && unsynced.isNotEmpty()) {
                    for (project in unsynced) {
                        val map = hashMapOf(
                            "id" to project.id,
                            "title" to project.title,
                            "category" to project.category,
                            "language" to project.language,
                            "codeContent" to project.codeContent,
                            "description" to project.description,
                            "updatedAt" to project.updatedAt,
                            "userId" to userId
                        )
                        db.collection("users").document(userId)
                            .collection("projects").document(project.id.toString())
                            .set(map, SetOptions.merge())
                            .await()

                        repository.markProjectSynced(project.id, true)
                    }
                }

                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    pendingItemsCount = 0,
                    statusMessage = "All projects synchronized with Cloud."
                )
            } catch (e: Exception) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    statusMessage = "Offline Mode: Changes saved safely to local Room Database."
                )
            }
        }
    }

    fun setNetworkStatus(online: Boolean) {
        _syncState.value = _syncState.value.copy(
            isOnline = online,
            statusMessage = if (online) "Online (Cloud Sync Active)" else "Offline Mode (Local Storage Protected)"
        )
    }
}
