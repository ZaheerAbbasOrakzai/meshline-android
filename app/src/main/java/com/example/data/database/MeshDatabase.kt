package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ContactDao
import com.example.data.dao.IdentityDao
import com.example.data.dao.JoinRequestDao
import com.example.data.dao.MessageDao
import com.example.data.model.ContactEntity
import com.example.data.model.IdentityEntity
import com.example.data.model.JoinRequestEntity
import com.example.data.model.MessageEntity

@Database(
    entities = [IdentityEntity::class, ContactEntity::class, MessageEntity::class, JoinRequestEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MeshDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun joinRequestDao(): JoinRequestDao

    companion object {
        @Volatile
        private var INSTANCE: MeshDatabase? = null

        fun getInstance(context: Context): MeshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeshDatabase::class.java,
                    "meshline_secure_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
