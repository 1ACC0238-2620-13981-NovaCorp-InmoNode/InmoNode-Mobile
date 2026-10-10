package com.novacorp.inmonode_app.features.vouchers.infrastructure.di

import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.VoucherService
import com.novacorp.inmonode_app.core.di.SessionScopedApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object VoucherApiModule {
    @Provides @Singleton
    fun service(@SessionScopedApi retrofit: Retrofit): VoucherService = retrofit.create(VoucherService::class.java)
}
