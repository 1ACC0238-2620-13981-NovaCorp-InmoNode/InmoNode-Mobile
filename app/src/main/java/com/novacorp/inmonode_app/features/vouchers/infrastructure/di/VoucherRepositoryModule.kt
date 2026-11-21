package com.novacorp.inmonode_app.features.vouchers.infrastructure.di

import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import com.novacorp.inmonode_app.features.vouchers.domain.OcrEngine
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.MlKitOcrEngine
import com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories.IdentityDocumentRepositoryImpl
import com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories.VoucherRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
interface VoucherRepositoryModule {
    @Binds @Singleton fun repository(impl: VoucherRepositoryImpl): VoucherRepository
    @Binds @Singleton fun identityDocuments(impl: IdentityDocumentRepositoryImpl): IdentityDocumentRepository
    @Binds @Singleton fun ocr(impl: MlKitOcrEngine): OcrEngine
}
