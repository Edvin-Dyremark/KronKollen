package com.kronkollen.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kronkollen.KronKollenApp
import com.kronkollen.data.BackupManager
import com.kronkollen.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    application: Application,
    private val backup: BackupManager,
    private val settings: SettingsDataStore,
) : AndroidViewModel(application) {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun export(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val json = backup.exportJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    } ?: error("Kunde inte skriva filen.")
                }
                _message.value = "Säkerhetskopia sparad."
            } catch (e: Exception) {
                _message.value = "Kunde inte spara: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: error("Kunde inte läsa filen.")
                }
                val result = backup.restoreJson(json)
                settings.setSampleDataPresent(false)
                _message.value =
                    "Återställde ${result.transactions} transaktioner och ${result.categories} kategorier."
            } catch (e: Exception) {
                _message.value = "Kunde inte återställa: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as KronKollenApp
                SettingsViewModel(app, app.container.backupManager, app.container.settings)
            }
        }
    }
}
