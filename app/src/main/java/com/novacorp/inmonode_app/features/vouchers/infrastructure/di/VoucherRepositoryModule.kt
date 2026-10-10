package com.novacorp.inmonode_app.features.vouchers.infrastructure.di

import com.novacorp.inmonode_app.features.vouchers.domain.OcrEngine
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.MlKitOcrEngine
import com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories.VoucherRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
interface VoucherRepositoryModule {
    @Binds @Singleton fun repository(impl: VoucherRepositoryImpl): VoucherRepository
    @Binds @Singleton fun ocr(impl: MlKitOcrEngine): OcrEngine
}
