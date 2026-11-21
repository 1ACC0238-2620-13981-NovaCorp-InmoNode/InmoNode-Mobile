package com.novacorp.inmonode_app.core.database

import android.util.Log
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.LotEntity
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.ProspectEntity
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.ReservationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * T62 spike: what encrypting the field database with SQLCipher costs. It is a measurement, not a regression test:
 * each variant writes a field day's worth of data, is reopened cold and read back, and the medians are logged with
 * the tag "T62". The only assertions check that the data is the same and that an encrypted file needs its key.
 */
class LocalDatabaseEncryptionBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** A 256-bit key as SQLCipher's blob literal, which skips the passphrase key derivation (PBKDF2). */
    private val rawKey = "x'" + ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02X".format(it) } + "'"

    private enum class Variant(val label: String) {
        PLAIN_DEFAULT("Room por defecto (la app hoy)"),
        PLAIN_DRIVER("AndroidSQLiteDriver, sin cifrar"),
        SQLCIPHER_RAW_KEY("SQLCipher, clave de 256 bits"),
        SQLCIPHER_PASSPHRASE("SQLCipher, frase con PBKDF2"),
    }

    @Before fun loadSqlCipher() { System.loadLibrary("sqlcipher") }

    @After fun deleteDatabases() { Variant.entries.forEach { context.deleteDatabase(name(it)) } }

    @Test fun measureEachVariant() = runBlocking<Unit> {
        val report = StringBuilder("\n| Variante | Escritura (ms) | Apertura en frío (ms) | Lectura x$READS (ms) | Archivo (KB) |\n|---|---:|---:|---:|---:|\n")
        for (variant in Variant.entries) {
            val writes = mutableListOf<Long>(); val opens = mutableListOf<Long>(); val reads = mutableListOf<Long>(); var size = 0L
            repeat(REPEAT) {
                context.deleteDatabase(name(variant))
                writes += timed { open(variant).use { it.writeFieldDay() } }
                open(variant).use { database ->
                    opens += timed { database.lotDao().find(OWNER, 1) }
                    reads += timed { repeat(READS) { assertEquals(LOTS, database.lotDao().observeLots(OWNER).first().size.toLong()) } }
                    assertEquals(RESERVATIONS, database.reservationDao().observeReservations(OWNER).first().size)
                }
                size = listOf("", "-wal", "-shm").sumOf { suffix -> context.getDatabasePath(name(variant) + suffix).length() }
            }
            report.append("| ${variant.label} | ${writes.median()} | ${opens.median()} | ${reads.median()} | ${size / 1024} |\n")
        }
        Log.i(TAG, report.toString())
    }

    @Test fun anEncryptedFileCannotBeReadWithoutItsKey() = runBlocking<Unit> {
        open(Variant.SQLCIPHER_RAW_KEY).use { it.writeFieldDay() }
        val path = context.getDatabasePath(name(Variant.SQLCIPHER_RAW_KEY)).absolutePath
        val header = context.getDatabasePath(name(Variant.SQLCIPHER_RAW_KEY)).inputStream().use { String(it.readNBytes(16)) }
        assertTrue("an encrypted file has no SQLite header", !header.startsWith("SQLite format 3"))
        for (driver in listOf(AndroidSQLiteDriver(), SQLCipherDriver("otra clave".toByteArray(), null, null))) {
            val failure = runCatching {
                val connection = driver.open(path)
                try { connection.prepare("SELECT count(*) FROM lots").let { s -> try { s.step() } finally { s.close() } } } finally { connection.close() }
            }
            assertTrue("${driver.javaClass.simpleName} must not read the file", failure.isFailure)
        }
    }

    private suspend fun InmoNodeDatabase.writeFieldDay() {
        // A downloaded portfolio: lots with their polygon, written in one transaction as the download does.
        lotDao().upsert((1L..LOTS).map { id ->
            LotEntity(OWNER, id, 1 + id % 4, "M-${id / 20}-L$id", "8.00", "20.00", "160.00", "45000.00", "PEN", "AVAILABLE",
                "[[-12.0461,-77.0428],[-12.0462,-77.0427],[-12.0463,-77.0429],[-12.0461,-77.0430],[-12.0461,-77.0428]]")
        })
        // Prospects and separations registered one by one in the wizard, each in its own transaction.
        for (i in 1..RESERVATIONS) {
            prospectDao().upsert(ProspectEntity(OWNER, "p$i", "4${"%07d".format(i)}", "Prospecto $i", "9${"%08d".format(i)}", "SOLTERO", AT))
            reservationDao().upsert(ReservationEntity(OWNER, "r$i", i.toLong(), "p$i", "500.00", AT, "PENDING_SYNC"))
        }
    }

    private fun open(variant: Variant): InmoNodeDatabase {
        val builder = Room.databaseBuilder<InmoNodeDatabase>(context, context.getDatabasePath(name(variant)).absolutePath)
        return when (variant) {
            Variant.PLAIN_DEFAULT -> builder
            Variant.PLAIN_DRIVER -> builder.setDriver(AndroidSQLiteDriver())
            Variant.SQLCIPHER_RAW_KEY -> builder.setDriver(SQLCipherDriver(rawKey.toByteArray(), null, null))
            Variant.SQLCIPHER_PASSPHRASE -> builder.setDriver(SQLCipherDriver("frase de prueba del spike".toByteArray(), null, null))
        }.build()
    }

    private inline fun <R> InmoNodeDatabase.use(block: (InmoNodeDatabase) -> R): R = try { block(this) } finally { close() }
    private inline fun timed(block: () -> Unit): Long { val start = System.nanoTime(); block(); return (System.nanoTime() - start) / 1_000_000 }
    private fun List<Long>.median() = sorted()[size / 2]
    private fun name(variant: Variant) = "t62-${variant.name.lowercase()}.db"

    private companion object {
        const val TAG = "T62"
        const val OWNER = 7L
        const val LOTS = 2_000L
        const val RESERVATIONS = 300
        const val READS = 10
        const val REPEAT = 5
        const val AT = "2026-11-21T10:00:00Z"
    }
}
