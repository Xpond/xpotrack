package com.xpotrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xpotrack.app.data.security.PassphraseStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [NoteEntity::class, TaskEntity::class, MetaEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class XpDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun metaDao(): MetaDao

    companion object {
        @Volatile private var instance: XpDatabase? = null

        fun get(context: Context): XpDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(appContext: Context): XpDatabase {
            // SQLCipher loads its native libs lazily; call site below is safe pre-Room.
            System.loadLibrary("sqlcipher")
            val passphrase = PassphraseStore(appContext).getOrCreate()
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(appContext, XpDatabase::class.java, "xpotrack.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigrationFrom(1, 2, 3)
                .build()
        }
    }
}
