package com.kronkollen

import android.app.Application
import com.kronkollen.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KronKollenApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // First launch: seed demo categories, keyword rules and transactions so the app
        // isn't empty. The demo transactions are replaced on the first real import.
        appScope.launch { container.sampleDataSeeder.seedIfFirstRun() }
    }
}
