package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index("category"), Index("updatedAt"), Index("isFavorite")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // GAME, APP, WEB, SHADER, MATH_PHYSICS, ALGORITHM
    val language: String, // KOTLIN, JAVASCRIPT, GLSL, PYTHON, HTML5, LUA
    val codeContent: String,
    val description: String = "",
    val isSynced: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assets",
    indices = [Index("projectId"), Index("assetType"), Index("createdAt")]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val title: String,
    val assetType: String, // IMAGE, VIDEO, AUDIO, 3D_MESH
    val prompt: String,
    val dataUriOrBase64: String, // Base64 data or image URL
    val resolution: String = "1K", // 1K, 2K, 4K, 1080p
    val aspectRatio: String = "1:1", // 1:1, 16:9, 9:16
    val modelUsed: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_logs",
    indices = [Index("timestamp"), Index("model")]
)
data class AiLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val response: String,
    val thinkingProcess: String = "",
    val model: String,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "binary_programs",
    indices = [Index("projectId"), Index("programName"), Index("compiledAt")]
)
data class BinaryProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val programName: String,
    val bytecode: ByteArray,
    val memorySnapshot: ByteArray = ByteArray(0),
    val instructionCount: Int = 0,
    val signatureSha256: String = "",
    val compiledAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BinaryProgramEntity
        if (id != other.id) return false
        if (!bytecode.contentEquals(other.bytecode)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + bytecode.contentHashCode()
        return result
    }
}

@Entity(
    tableName = "game_snapshots",
    indices = [Index("slotId"), Index("timestamp")]
)
data class GameSaveSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slotId: Int = 1,
    val levelName: String = "Default Level",
    val binaryData: ByteArray,
    val score: Int = 0,
    val health: Int = 3,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameSaveSnapshotEntity
        if (id != other.id) return false
        if (!binaryData.contentEquals(other.binaryData)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + binaryData.contentHashCode()
        return result
    }
}

@Entity(
    tableName = "admin_users",
    indices = [Index("email", unique = true), Index("role"), Index("status")]
)
data class AdminUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val role: String, // ADMIN, MODERATOR, DEVELOPER, CREATOR, FREE_USER
    val status: String, // ACTIVE, SUSPENDED, FLAGGED, BANNED
    val aiTokenQuota: Long = 500000L,
    val aiTokensUsed: Long = 12450L,
    val riskScore: Int = 5, // 0 - 100
    val deviceModel: String = "Pixel 9 Pro",
    val lastActive: Long = System.currentTimeMillis(),
    val joinedAt: Long = System.currentTimeMillis() - 86400000L * 30
)

@Entity(
    tableName = "moderation_items",
    indices = [Index("contentType"), Index("status"), Index("severity"), Index("reportedAt")]
)
data class ModerationItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentType: String, // AI_IMAGE, CODE_SNIPPET, GAME_LEVEL, PROMPT, USER_PROFILE, COMMENT
    val title: String,
    val content: String,
    val authorEmail: String,
    val authorName: String,
    val flagReason: String,
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val status: String, // PENDING, APPROVED, REJECTED, ESCALATED
    val safetyScore: Int, // 0 - 100 (Higher means safer, lower means dangerous)
    val toxicityScore: Int = 12,
    val nsfwScore: Int = 4,
    val copyrightScore: Int = 8,
    val reviewerNotes: String = "",
    val reportedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "system_health_events",
    indices = [Index("serviceName"), Index("status"), Index("timestamp")]
)
data class SystemHealthEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceName: String, // GEMINI_API, FIRESTORE_SYNC, ROOM_DATABASE, PHYSICS_SOLVER, VM_RUNTIME
    val status: String, // OPERATIONAL, DEGRADED, INCIDENT, MAINTENANCE
    val latencyMs: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admin_audit_logs",
    indices = [Index("adminEmail"), Index("action"), Index("timestamp")]
)
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adminEmail: String,
    val action: String, // BAN_USER, APPROVE_CONTENT, REJECT_CONTENT, CHANGE_ROLE, FLUSH_CACHE, TOGGLE_MAINTENANCE
    val target: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_behavior_events",
    indices = [Index("eventName"), Index("screenName"), Index("timestamp")]
)
data class UserBehaviorEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userEmail: String,
    val eventName: String, // SCREEN_VIEW, PHYSICS_SPAWN, CODE_RUN, AI_GENERATE, EXPORT_PACKAGE
    val screenName: String,
    val durationMs: Long = 0L,
    val metadataJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)
