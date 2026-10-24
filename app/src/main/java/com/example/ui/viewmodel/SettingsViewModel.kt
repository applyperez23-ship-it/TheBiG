package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TheBiGRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RemoteBroadcastNotice(
    val title: String,
    val message: String,
    val isSurvey: Boolean = false,
    val options: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class SettingsViewModel(
    private val repository: TheBiGRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("thebig_prefs", Context.MODE_PRIVATE)

    private val _onboardingShown = MutableStateFlow(prefs.getBoolean("onboarding_accepted", false))
    val onboardingShown: StateFlow<Boolean> = _onboardingShown.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Performance Saving Mode: "Descargar/Optimizar Animaciones"
    private val _isPowerSavingMode = MutableStateFlow(prefs.getBoolean("power_saving_mode", false))
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    // Hidden Developer Mode APK URL
    private val _apkDownloadUrl = MutableStateFlow(
        prefs.getString("apk_download_url", "https://thebig.app/download/app-latest.apk")
            ?: "https://thebig.app/download/app-latest.apk"
    )
    val apkDownloadUrl: StateFlow<String> = _apkDownloadUrl.asStateFlow()

    // Remote Broadcaster State
    private val _activeBroadcast = MutableStateFlow<RemoteBroadcastNotice?>(null)
    val activeBroadcast: StateFlow<RemoteBroadcastNotice?> = _activeBroadcast.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun acceptOnboarding() {
        prefs.edit().putBoolean("onboarding_accepted", true).apply()
        _onboardingShown.value = true
    }

    fun toggleDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun togglePowerSavingMode(enabled: Boolean) {
        prefs.edit().putBoolean("power_saving_mode", enabled).apply()
        _isPowerSavingMode.value = enabled
    }

    fun setApkDownloadUrl(newUrl: String) {
        prefs.edit().putString("apk_download_url", newUrl).apply()
        _apkDownloadUrl.value = newUrl
    }

    fun broadcastNotice(title: String, message: String, isSurvey: Boolean = false, options: List<String> = emptyList()) {
        val notice = RemoteBroadcastNotice(title, message, isSurvey, options)
        _activeBroadcast.value = notice
    }

    fun dismissBroadcast() {
        _activeBroadcast.value = null
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportDatabaseToJson()
                _backupStatus.value = "Copia generada exitosamente"
                onReady(json)
            } catch (e: Exception) {
                _backupStatus.value = "Error al exportar: ${e.message}"
            }
        }
    }

    fun importBackup(jsonContent: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.importDatabaseFromJson(jsonContent)
                _backupStatus.value = if (success) "Copia restaurada exitosamente" else "Error al importar datos"
                onComplete(success)
            } catch (e: Exception) {
                _backupStatus.value = "Error al importar: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}

class SettingsViewModelFactory(
    private val repository: TheBiGRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
