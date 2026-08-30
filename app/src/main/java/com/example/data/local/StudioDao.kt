package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {
    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE projects SET isSynced = :synced WHERE id = :id")
    suspend fun updateProjectSyncStatus(id: Long, synced: Boolean)

    // Assets
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    // AI Logs
    @Query("SELECT * FROM ai_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAiLogs(): Flow<List<AiLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiLog(log: AiLogEntity): Long

    // Binary Programs
    @Query("SELECT * FROM binary_programs ORDER BY compiledAt DESC")
    fun getAllBinaryPrograms(): Flow<List<BinaryProgramEntity>>

    @Query("SELECT * FROM binary_programs WHERE id = :id")
    suspend fun getBinaryProgramById(id: Long): BinaryProgramEntity?

    @Query("SELECT * FROM binary_programs WHERE programName = :name LIMIT 1")
    suspend fun getBinaryProgramByName(name: String): BinaryProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinaryProgram(program: BinaryProgramEntity): Long

    @Query("DELETE FROM binary_programs WHERE id = :id")
    suspend fun deleteBinaryProgramById(id: Long)

    // Game Snapshots
    @Query("SELECT * FROM game_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<GameSaveSnapshotEntity>>

    @Query("SELECT * FROM game_snapshots WHERE slotId = :slotId LIMIT 1")
    suspend fun getSnapshotBySlot(slotId: Int): GameSaveSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: GameSaveSnapshotEntity): Long

    @Query("DELETE FROM game_snapshots WHERE slotId = :slotId")
    suspend fun deleteSnapshotBySlot(slotId: Int)

    // Admin Users
    @Query("SELECT * FROM admin_users ORDER BY lastActive DESC")
    fun getAllAdminUsers(): Flow<List<AdminUserEntity>>

    @Query("SELECT * FROM admin_users WHERE id = :id")
    suspend fun getAdminUserById(id: Long): AdminUserEntity?

    @Query("SELECT * FROM admin_users WHERE email = :email LIMIT 1")
    suspend fun getAdminUserByEmail(email: String): AdminUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminUser(user: AdminUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminUsers(users: List<AdminUserEntity>)

    @Update
    suspend fun updateAdminUser(user: AdminUserEntity)

    @Query("UPDATE admin_users SET status = :status WHERE id = :id")
    suspend fun updateUserStatus(id: Long, status: String)

    @Query("UPDATE admin_users SET role = :role WHERE id = :id")
    suspend fun updateUserRole(id: Long, role: String)

    @Query("UPDATE admin_users SET aiTokenQuota = :quota WHERE id = :id")
    suspend fun updateUserQuota(id: Long, quota: Long)

    @Query("DELETE FROM admin_users WHERE id = :id")
    suspend fun deleteAdminUser(id: Long)

    // Moderation Queue
    @Query("SELECT * FROM moderation_items ORDER BY reportedAt DESC")
    fun getAllModerationItems(): Flow<List<ModerationItemEntity>>

    @Query("SELECT * FROM moderation_items WHERE status = :status ORDER BY reportedAt DESC")
    fun getModerationItemsByStatus(status: String): Flow<List<ModerationItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerationItem(item: ModerationItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerationItems(items: List<ModerationItemEntity>)

    @Query("UPDATE moderation_items SET status = :status, reviewerNotes = :notes WHERE id = :id")
    suspend fun updateModerationStatus(id: Long, status: String, notes: String)

    @Query("DELETE FROM moderation_items WHERE id = :id")
    suspend fun deleteModerationItem(id: Long)

    // System Health Events
    @Query("SELECT * FROM system_health_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHealthEvents(): Flow<List<SystemHealthEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthEvent(event: SystemHealthEventEntity): Long

    @Query("DELETE FROM system_health_events")
    suspend fun clearHealthEvents()

    // Admin Audit Logs
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity): Long

    // User Behavior Analytics Events
    @Query("SELECT * FROM user_behavior_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentBehaviorEvents(): Flow<List<UserBehaviorEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorEvent(event: UserBehaviorEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorEvents(events: List<UserBehaviorEventEntity>)
}
