package com.mobiletrack.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class PurposeEditorViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val disabledPurposes = userPreferences.disabledPurposes

    val customHintsMap = userPreferences.purposeHintsJson
        .map { json -> parseHintsJson(json) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun togglePurpose(label: String, currentDisabled: Set<String>) {
        viewModelScope.launch {
            val newSet = if (label in currentDisabled) currentDisabled - label
                else currentDisabled + label
            userPreferences.setDisabledPurposes(newSet)
        }
    }

    fun addHint(mapKey: String, hint: String, currentHints: Map<String, List<String>>) {
        viewModelScope.launch {
            val defaults = ALL_PURPOSE_DEFS.find { it.mapKey == mapKey }?.defaultHints ?: emptyList()
            val existing = currentHints[mapKey] ?: defaults
            if (hint !in existing) {
                val updated = currentHints.toMutableMap()
                updated[mapKey] = existing + hint
                userPreferences.setPurposeHintsJson(toHintsJson(updated))
            }
        }
    }

    fun removeHint(mapKey: String, hint: String, currentHints: Map<String, List<String>>) {
        viewModelScope.launch {
            val defaults = ALL_PURPOSE_DEFS.find { it.mapKey == mapKey }?.defaultHints ?: emptyList()
            val existing = currentHints[mapKey] ?: defaults
            val updated = currentHints.toMutableMap()
            updated[mapKey] = existing - hint
            userPreferences.setPurposeHintsJson(toHintsJson(updated))
        }
    }

    private fun parseHintsJson(json: String): Map<String, List<String>> {
        if (json.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, List<String>>()
            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val list = (0 until arr.length()).map { arr.getString(it) }
                result[key] = list
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun toHintsJson(map: Map<String, List<String>>): String {
        val obj = JSONObject()
        map.forEach { (key, hints) ->
            val arr = org.json.JSONArray(hints)
            obj.put(key, arr)
        }
        return obj.toString()
    }
}
