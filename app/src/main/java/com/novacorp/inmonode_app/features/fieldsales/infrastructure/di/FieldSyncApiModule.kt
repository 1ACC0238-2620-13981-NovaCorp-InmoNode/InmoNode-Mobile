package com.novacorp.inmonode_app.features.fieldsales.infrastructure.di

import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.FieldSyncService
import com.novacorp.inmonode_app.core.di.SessionScopedApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object FieldSyncApiModule {
    @Provides @Singleton
    fun service(@SessionScopedApi retrofit: Retrofit): FieldSyncService = retrofit.create(FieldSyncService::class.java)
}
