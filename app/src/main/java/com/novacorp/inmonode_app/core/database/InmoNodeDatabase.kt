package com.novacorp.inmonode_app.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherDao
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.PaymentEvidencesEntity

@Database(entities = [ProjectEntity::class, LotEntity::class, ProspectEntity::class,
    ReservationEntity::class, SyncMetadataEntity::class, VoucherEntity::class, PaymentEvidencesEntity::class], version = 1, exportSchema = true)
abstract class InmoNodeDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
    abstract fun lotDao(): LotDao
    abstract fun prospectDao(): ProspectDao
    abstract fun reservationDao(): ReservationDao
    abstract fun voucherDao(): VoucherDao
}

/** Download replacement, lot claims and sync acknowledgements are all-or-nothing. */
internal suspend fun <T> InmoNodeDatabase.atomic(block: suspend () -> T): T =
    useWriterConnection { connection -> connection.immediateTransaction { block() } }
