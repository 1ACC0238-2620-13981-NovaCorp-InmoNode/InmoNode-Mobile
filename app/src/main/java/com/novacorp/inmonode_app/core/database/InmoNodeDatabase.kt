package com.novacorp.inmonode_app.core.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.IdentityDocumentDao
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.IdentityDocumentEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherDao
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.PaymentEvidencesEntity

// v3 (US-59) only adds the identity_documents table, so Room migrates it on its own.
@Database(entities = [ProjectEntity::class, LotEntity::class, ProspectEntity::class,
    ReservationEntity::class, SyncMetadataEntity::class, VoucherEntity::class, PaymentEvidencesEntity::class,
    IdentityDocumentEntity::class], version = 3, exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)])
abstract class InmoNodeDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
    abstract fun lotDao(): LotDao
    abstract fun prospectDao(): ProspectDao
    abstract fun reservationDao(): ReservationDao
    abstract fun voucherDao(): VoucherDao
    abstract fun identityDocumentDao(): IdentityDocumentDao
}

/** Download replacement, lot claims and sync acknowledgements are all-or-nothing. */
internal suspend fun <T> InmoNodeDatabase.atomic(block: suspend () -> T): T =
    useWriterConnection { connection -> connection.immediateTransaction { block() } }
