package com.example.carrom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.carrom.data.local.dao.ActiveMatchDao
import com.example.carrom.data.local.dao.GroupDao
import com.example.carrom.data.local.dao.MatchDao
import com.example.carrom.data.local.dao.PlayerDao
import com.example.carrom.data.local.entity.ActiveMatchEntity
import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity

@Database(
    entities = [
        PlayerEntity::class,
        MatchEntity::class,
        ActiveMatchEntity::class,
        GroupEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CarromDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun activeMatchDao(): ActiveMatchDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: CarromDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN groupName TEXT NOT NULL DEFAULT 'General'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        colorIndex INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN memberPlayerIds TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN team1Name TEXT NOT NULL DEFAULT 'Team 1'")
                db.execSQL("ALTER TABLE groups ADD COLUMN team2Name TEXT NOT NULL DEFAULT 'Team 2'")
                db.execSQL("ALTER TABLE groups ADD COLUMN team1Player1 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE groups ADD COLUMN team1Player2 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE groups ADD COLUMN team2Player1 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE groups ADD COLUMN team2Player2 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE groups ADD COLUMN isDoubles INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): CarromDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarromDatabase::class.java,
                    "carrom_score_keeper.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
