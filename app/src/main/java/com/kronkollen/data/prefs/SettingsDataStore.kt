package com.kronkollen.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kronkollen.importer.AmountMode
import com.kronkollen.importer.ColumnMapping
import com.kronkollen.util.DateFormatOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("kronkollen_settings")

/** Remembers the last column mapping so repeated imports pre-fill the same choices. */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val hasHeader = booleanPreferencesKey("map_has_header")
        val dateCol = intPreferencesKey("map_date_col")
        val descCol = intPreferencesKey("map_desc_col")
        val amountMode = stringPreferencesKey("map_amount_mode")
        val amountCol = intPreferencesKey("map_amount_col")
        val flipSign = booleanPreferencesKey("map_flip_sign")
        val outCol = intPreferencesKey("map_out_col")
        val inCol = intPreferencesKey("map_in_col")
        val dateFormat = stringPreferencesKey("map_date_format")
    }

    val mapping: Flow<ColumnMapping> = context.dataStore.data.map { p ->
        val default = ColumnMapping()
        ColumnMapping(
            hasHeaderRow = p[Keys.hasHeader] ?: default.hasHeaderRow,
            dateColumn = p[Keys.dateCol] ?: default.dateColumn,
            descriptionColumn = p[Keys.descCol] ?: default.descriptionColumn,
            amountMode = p[Keys.amountMode]?.let { runCatching { AmountMode.valueOf(it) }.getOrNull() }
                ?: default.amountMode,
            amountColumn = p[Keys.amountCol] ?: default.amountColumn,
            flipSign = p[Keys.flipSign] ?: default.flipSign,
            outColumn = p[Keys.outCol] ?: default.outColumn,
            inColumn = p[Keys.inCol] ?: default.inColumn,
            dateFormat = p[Keys.dateFormat]?.let { runCatching { DateFormatOption.valueOf(it) }.getOrNull() }
                ?: default.dateFormat,
        )
    }

    suspend fun saveMapping(mapping: ColumnMapping) {
        context.dataStore.edit { p ->
            p[Keys.hasHeader] = mapping.hasHeaderRow
            p[Keys.dateCol] = mapping.dateColumn
            p[Keys.descCol] = mapping.descriptionColumn
            p[Keys.amountMode] = mapping.amountMode.name
            p[Keys.amountCol] = mapping.amountColumn
            p[Keys.flipSign] = mapping.flipSign
            p[Keys.outCol] = mapping.outColumn
            p[Keys.inCol] = mapping.inColumn
            p[Keys.dateFormat] = mapping.dateFormat.name
        }
    }
}
