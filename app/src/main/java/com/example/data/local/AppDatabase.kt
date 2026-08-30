package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        AssetEntity::class,
        AiLogEntity::class,
        BinaryProgramEntity::class,
        GameSaveSnapshotEntity::class,
        AdminUserEntity::class,
        ModerationItemEntity::class,
        SystemHealthEventEntity::class,
        AdminAuditLogEntity::class,
        UserBehaviorEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studio_ai_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Low-RAM optimizations for itel A70 / Android 5.0+
                            try {
                                db.execSQL("PRAGMA journal_mode = WAL;")
                                db.execSQL("PRAGMA synchronous = NORMAL;")
                                db.execSQL("PRAGMA cache_size = -2000;") // 2MB page cache
                                db.execSQL("PRAGMA temp_store = MEMORY;")
                            } catch (_: Exception) {}
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
