package com.example.carrom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.carrom.data.local.dao.ActiveMatchDao
import com.example.carrom.data.local.dao.MatchDao
import com.example.carrom.data.local.dao.PlayerDao
import com.example.carrom.data.local.entity.ActiveMatchEntity
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity

@Database(
    entities = [
        PlayerEntity::class,
        MatchEntity::class,
        ActiveMatchEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CarromDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun activeMatchDao(): ActiveMatchDao

    companion object {
        @Volatile
        private var INSTANCE: CarromDatabase? = null

        fun getDatabase(context: Context): CarromDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarromDatabase::class.java,
                    "carrom_score_keeper.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
