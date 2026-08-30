package com.example.data.local

import kotlinx.coroutines.flow.Flow

class StudioRepository(private val dao: StudioDao) {
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()
    val allAssets: Flow<List<AssetEntity>> = dao.getAllAssets()
    val recentAiLogs: Flow<List<AiLogEntity>> = dao.getRecentAiLogs()

    suspend fun getProject(id: Long): ProjectEntity? = dao.getProjectById(id)

    suspend fun saveProject(project: ProjectEntity): Long = dao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)

    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)

    suspend fun markProjectSynced(id: Long, synced: Boolean) = dao.updateProjectSyncStatus(id, synced)

    suspend fun saveAsset(asset: AssetEntity): Long = dao.insertAsset(asset)

    suspend fun deleteAsset(id: Long) = dao.deleteAssetById(id)

    suspend fun logAiInteraction(log: AiLogEntity): Long = dao.insertAiLog(log)

    // Binary Assembly Programs
    val allBinaryPrograms: Flow<List<BinaryProgramEntity>> = dao.getAllBinaryPrograms()

    suspend fun getBinaryProgram(id: Long): BinaryProgramEntity? = dao.getBinaryProgramById(id)

    suspend fun getBinaryProgramByName(name: String): BinaryProgramEntity? = dao.getBinaryProgramByName(name)

    suspend fun saveBinaryProgram(program: BinaryProgramEntity): Long = dao.insertBinaryProgram(program)

    suspend fun deleteBinaryProgram(id: Long) = dao.deleteBinaryProgramById(id)

    // Game Snapshots
    val allSnapshots: Flow<List<GameSaveSnapshotEntity>> = dao.getAllSnapshots()

    suspend fun getSnapshotBySlot(slotId: Int): GameSaveSnapshotEntity? = dao.getSnapshotBySlot(slotId)

    suspend fun saveSnapshot(snapshot: GameSaveSnapshotEntity): Long = dao.insertSnapshot(snapshot)

    suspend fun deleteSnapshot(slotId: Int) = dao.deleteSnapshotBySlot(slotId)

    // Admin Users
    val allAdminUsers: Flow<List<AdminUserEntity>> = dao.getAllAdminUsers()

    suspend fun getAdminUserById(id: Long): AdminUserEntity? = dao.getAdminUserById(id)

    suspend fun saveAdminUser(user: AdminUserEntity): Long = dao.insertAdminUser(user)

    suspend fun seedAdminUsers(users: List<AdminUserEntity>) = dao.insertAdminUsers(users)

    suspend fun updateAdminUser(user: AdminUserEntity) = dao.updateAdminUser(user)

    suspend fun updateUserStatus(id: Long, status: String) = dao.updateUserStatus(id, status)

    suspend fun updateUserRole(id: Long, role: String) = dao.updateUserRole(id, role)

    suspend fun updateUserQuota(id: Long, quota: Long) = dao.updateUserQuota(id, quota)

    suspend fun deleteAdminUser(id: Long) = dao.deleteAdminUser(id)

    // Moderation Queue
    val allModerationItems: Flow<List<ModerationItemEntity>> = dao.getAllModerationItems()

    fun getModerationItemsByStatus(status: String): Flow<List<ModerationItemEntity>> = dao.getModerationItemsByStatus(status)

    suspend fun saveModerationItem(item: ModerationItemEntity): Long = dao.insertModerationItem(item)

    suspend fun seedModerationItems(items: List<ModerationItemEntity>) = dao.insertModerationItems(items)

    suspend fun updateModerationStatus(id: Long, status: String, notes: String) = dao.updateModerationStatus(id, status, notes)

    suspend fun deleteModerationItem(id: Long) = dao.deleteModerationItem(id)

    // System Health Events
    val recentHealthEvents: Flow<List<SystemHealthEventEntity>> = dao.getRecentHealthEvents()

    suspend fun logHealthEvent(event: SystemHealthEventEntity): Long = dao.insertHealthEvent(event)

    suspend fun clearHealthEvents() = dao.clearHealthEvents()

    // Admin Audit Logs
    val recentAuditLogs: Flow<List<AdminAuditLogEntity>> = dao.getRecentAuditLogs()

    suspend fun logAdminAudit(log: AdminAuditLogEntity): Long = dao.insertAuditLog(log)

    // User Behavior Events
    val recentBehaviorEvents: Flow<List<UserBehaviorEventEntity>> = dao.getRecentBehaviorEvents()

    suspend fun logBehaviorEvent(event: UserBehaviorEventEntity): Long = dao.insertBehaviorEvent(event)

    suspend fun seedBehaviorEvents(events: List<UserBehaviorEventEntity>) = dao.insertBehaviorEvents(events)
}
