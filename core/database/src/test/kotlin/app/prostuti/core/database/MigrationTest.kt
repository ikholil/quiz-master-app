package app.prostuti.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ProstutiDatabase::class.java)

    @Test fun `v1 to v2 keeps queued offline operations`() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL("INSERT INTO pending_operations (id, type, payloadJson, createdAt, attempts) VALUES ('op1', 'note.upsert', '{}', 1000, 2)")
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2).use { db ->
            db.query("SELECT id, attempts, lastError FROM pending_operations").use { c ->
                c.moveToFirst()
                assertEquals("op1", c.getString(0))
                assertEquals(2, c.getInt(1))
                assertEquals(true, c.isNull(2))
            }
        }
    }

    @Test fun `room opens a migrated database with the current entities`() = runBlocking {
        helper.createDatabase(dbName, 1).close()
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), ProstutiDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL).allowMainThreadQueries().build()
        assertNull(db.pendingOperationDao().oldest(10).firstOrNull()?.lastError)
        db.close()
    }
}
