package com.novacorp.inmonode_app.core.di

import com.novacorp.inmonode_app.core.network.RequestOwnerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionScopedApi

@Module @InstallIn(SingletonComponent::class)
object SessionScopedApiModule {
    @Provides @Singleton @SessionScopedApi
    fun retrofit(retrofit: Retrofit, client: OkHttpClient, ownerInterceptor: RequestOwnerInterceptor): Retrofit =
        retrofit.newBuilder().client(client.newBuilder().addInterceptor(ownerInterceptor)
            .followRedirects(false).followSslRedirects(false).build()).build()
}
