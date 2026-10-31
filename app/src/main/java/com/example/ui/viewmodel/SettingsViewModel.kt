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
import org.json.JSONArray
import org.json.JSONObject

data class RemoteBroadcastNotice(
    val title: String,
    val message: String,
    val isSurvey: Boolean = false,
    val options: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class UserFeedbackReport(
    val id: Long = System.currentTimeMillis(),
    val type: String, // "SUGGESTION" or "BUG"
    val subject: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DevTicketMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "USER" or "DANIEL"
    val text: String,
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

    // Master switch for push notifications (default disabled)
    private val _isPushNotificationsEnabled = MutableStateFlow(prefs.getBoolean("push_notifications_master", false))
    val isPushNotificationsEnabled: StateFlow<Boolean> = _isPushNotificationsEnabled.asStateFlow()

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

    // Internal Feedback Reports
    private val _feedbackReports = MutableStateFlow<List<UserFeedbackReport>>(loadFeedbacks())
    val feedbackReports: StateFlow<List<UserFeedbackReport>> = _feedbackReports.asStateFlow()

    // Developer Chat / Ticket Messages
    private val _ticketMessages = MutableStateFlow<List<DevTicketMessage>>(loadTicketMessages())
    val ticketMessages: StateFlow<List<DevTicketMessage>> = _ticketMessages.asStateFlow()

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

    fun togglePushNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("push_notifications_master", enabled).apply()
        _isPushNotificationsEnabled.value = enabled
    }

    fun sendTicketMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = DevTicketMessage(
            sender = "USER",
            text = text.trim()
        )
        val current = _ticketMessages.value.toMutableList()
        current.add(userMsg)

        // Automated helpful acknowledgement from Daniel
        val devReply = DevTicketMessage(
            sender = "DANIEL",
            text = "¡Hola! Gracias por escribir al soporte técnico de TheBiG. Daniel ha recibido tu ticket y revisará los registros del sistema."
        )
        current.add(devReply)

        _ticketMessages.value = current
        saveTicketMessages(current)
    }

    fun submitFeedback(type: String, subject: String, description: String) {
        val newReport = UserFeedbackReport(
            type = type,
            subject = subject.trim(),
            description = description.trim()
        )
        val current = _feedbackReports.value.toMutableList()
        current.add(0, newReport)
        _feedbackReports.value = current
        saveFeedbacks(current)
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

    private fun loadFeedbacks(): List<UserFeedbackReport> {
        val raw = prefs.getString("user_feedbacks", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<UserFeedbackReport>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserFeedbackReport(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        type = obj.getString("type"),
                        subject = obj.getString("subject"),
                        description = obj.getString("description"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveFeedbacks(reports: List<UserFeedbackReport>) {
        try {
            val arr = JSONArray()
            for (r in reports) {
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("type", r.type)
                    put("subject", r.subject)
                    put("description", r.description)
                    put("timestamp", r.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString("user_feedbacks", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTicketMessages(): List<DevTicketMessage> {
        val raw = prefs.getString("dev_ticket_messages", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DevTicketMessage>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DevTicketMessage(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        sender = obj.getString("sender"),
                        text = obj.getString("text"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTicketMessages(messages: List<DevTicketMessage>) {
        try {
            val arr = JSONArray()
            for (m in messages) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("sender", m.sender)
                    put("text", m.text)
                    put("timestamp", m.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString("dev_ticket_messages", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
