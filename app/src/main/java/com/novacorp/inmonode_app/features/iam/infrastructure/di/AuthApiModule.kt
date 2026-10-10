package com.novacorp.inmonode_app.features.iam.infrastructure.di

import com.novacorp.inmonode_app.features.iam.infrastructure.remote.AuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import okhttp3.Authenticator
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthApiModule {

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        // A protected request may block inside Authenticator. Auth must not queue behind it.
        val client = (retrofit.callFactory() as OkHttpClient).newBuilder()
            .dispatcher(Dispatcher())
            .authenticator(Authenticator.NONE)
            // Never replay credentials or refresh tokens to a redirect destination.
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        return retrofit.newBuilder().client(client).build().create(AuthService::class.java)
    }
}
