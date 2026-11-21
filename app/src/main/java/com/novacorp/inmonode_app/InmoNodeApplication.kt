package com.novacorp.inmonode_app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.novacorp.inmonode_app.features.fieldsales.application.PendingSyncTrigger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class InmoNodeApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var pendingSync: PendingSyncTrigger

    override fun onCreate() {
        super.onCreate()
        pendingSync.start()
    }

    /** On-demand initialization (the default initializer is removed in the manifest) so workers get Hilt injection. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
