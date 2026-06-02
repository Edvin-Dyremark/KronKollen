package com.kronkollen.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) confirmRestoreUri = uri }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inställningar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Säkerhetskopiering", style = MaterialTheme.typography.titleMedium)
            Text(
                "Spara all data – transaktioner, kategorier och nyckelord – till en fil du " +
                    "väljer själv (t.ex. i Drive). Återställ när du byter telefon eller " +
                    "installerar om.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { exportLauncher.launch("kronkollen-backup-${LocalDate.now()}.json") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Säkerhetskopiera till fil") }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Återställ från fil") }

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    confirmRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestoreUri = null },
            title = { Text("Återställ data?") },
            text = {
                Text("All nuvarande data ersätts med innehållet i filen. Det går inte att ångra.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restore(uri)
                    confirmRestoreUri = null
                }) { Text("Återställ") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestoreUri = null }) { Text("Avbryt") }
            },
        )
    }
}
