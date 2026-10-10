package com.novacorp.inmonode_app.features.fieldsales.infrastructure.di

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
interface FieldSalesRepositoryModule {
    @Binds @Singleton fun portfolio(impl: PortfolioRepositoryImpl): PortfolioRepository
    @Binds @Singleton fun prospects(impl: ProspectRepositoryImpl): ProspectRepository
    @Binds @Singleton fun reservations(impl: ReservationRepositoryImpl): ReservationRepository
    @Binds @Singleton fun sync(impl: FieldSyncRepositoryImpl): FieldSyncRepository
}
