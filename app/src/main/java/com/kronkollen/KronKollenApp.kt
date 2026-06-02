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
        // Populate a starter set of categories on first launch.
        appScope.launch { container.categoryRepository.seedDefaultsIfEmpty() }
    }
}
