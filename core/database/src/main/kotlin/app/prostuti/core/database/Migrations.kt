package app.prostuti.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written migrations. Every schema change bumps the version in [ProstutiDatabase], exports a new
 * schema JSON (core/database/schemas) and adds a migration plus a test in MigrationTest.
 * Destructive fallback is deliberately NOT enabled: students' offline notes and progress must survive updates.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE pending_operations ADD COLUMN lastError TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
