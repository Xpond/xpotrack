package com.xpotrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xpotrack.app.data.security.PassphraseStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.LocalDate
import java.time.ZoneId

@Database(
    entities = [NoteEntity::class, TaskEntity::class, CategoryEntity::class, MetaEntity::class, QuickNoteEntity::class],
    version = 11,
    exportSchema = false,
)
abstract class XpDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun metaDao(): MetaDao
    abstract fun quickNoteDao(): QuickNoteDao

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
            // Pass the key as a raw-hex literal ("x'<64hex>'") so SQLCipher
            // skips PBKDF2 — the passphrase is already 32 random bytes from
            // Keystore, so KDF strengthening adds nothing but ~800ms per open.
            val rawKey = CipherFastKdf.rawKeyLiteral(passphrase)
            val factory = SupportOpenHelperFactory(rawKey)
            return Room.databaseBuilder(appContext, XpDatabase::class.java, "xpotrack.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .build()
        }

        // v8→v9: tasks gain a per-row date. Backfill existing rows to "today"
        // in the device's local zone — closest match to old behavior, where
        // every task implicitly meant today.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
                db.execSQL("ALTER TABLE tasks ADD COLUMN dateEpochDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE tasks SET dateEpochDay = $today")
            }
        }

        // v9→v10: tasks gain a recurrence rule. Existing rows default to "none".
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeat TEXT NOT NULL DEFAULT 'none'")
            }
        }

        // v10→v11: optional pointer to a note row. Nullable — most tasks won't have one.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN linkedNoteId INTEGER")
            }
        }
    }
}
