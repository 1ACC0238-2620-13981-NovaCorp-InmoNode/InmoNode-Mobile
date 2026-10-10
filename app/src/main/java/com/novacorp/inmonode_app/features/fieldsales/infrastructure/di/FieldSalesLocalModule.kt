package com.novacorp.inmonode_app.features.fieldsales.infrastructure.di

import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module @InstallIn(SingletonComponent::class)
object FieldSalesLocalModule {
    @Provides fun lots(database: InmoNodeDatabase) = database.lotDao()
    @Provides fun projects(database: InmoNodeDatabase) = database.portfolioDao()
    @Provides fun prospects(database: InmoNodeDatabase) = database.prospectDao()
    @Provides fun reservations(database: InmoNodeDatabase) = database.reservationDao()
}
