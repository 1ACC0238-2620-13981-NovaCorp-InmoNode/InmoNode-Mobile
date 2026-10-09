package com.novacorp.inmonode_app.features.iam.infrastructure.di

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.infrastructure.repositories.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthRepositoryModule {

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
