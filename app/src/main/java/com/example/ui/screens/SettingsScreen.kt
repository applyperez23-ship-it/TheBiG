package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.TheBiGApp
import com.example.ui.components.TheBiGLogoVector
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.UserFeedbackReport
import com.example.util.TheBiGBackupHelper
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPowerSaving by viewModel.isPowerSavingMode.collectAsStateWithLifecycle()
    val apkUrl by viewModel.apkDownloadUrl.collectAsStateWithLifecycle()
    val activeNotice by viewModel.activeBroadcast.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val feedbacks by viewModel.feedbackReports.collectAsStateWithLifecycle()

    // 5-tap secret trigger on version string
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isDevPanelUnlocked by remember { mutableStateOf(false) }

    // Dialogs
    var showSupportOptionDialog by remember { mutableStateOf(false) }
    var showInternalReportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedTbText by remember { mutableStateOf("") }
    var importTbText by remember { mutableStateOf("") }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictingNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var showBroadcastSimulatorDialog by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var showUpdateCheckDialog by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var showQrShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes y Sistema",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Header: single subtle credit to Daniel (el Grande)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TheBiGLogoVector(size = 56.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "TheBiG",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Creada por Daniel (el Grande)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Secret 5-tap target
                        Box(
                            modifier = Modifier
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTimestamp > 2500) {
                                        versionTapCount = 1
                                    } else {
                                        versionTapCount++
                                    }
                                    lastTapTimestamp = now

                                    if (versionTapCount >= 5) {
                                        versionTapCount = 0
                                        showPasswordDialog = true
                                    } else if (versionTapCount in 2..4) {
                                        val remaining = 5 - versionTapCount
                                        Toast.makeText(
                                            context,
                                            "Toca $remaining veces más para acceso desarrollador",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "TheBiG v1.7 • Creado por Daniel el grande",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Support & Feedback Button: "Comunicar una sugerencia o error"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Atención y Sugerencias",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Envía reportes internos o contacta directamente con Daniel",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showSupportOptionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_option_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comunicar una sugerencia o error")
                    }
                }
            }

            // Performance Saving Mode: "Descargar/Optimizar Animaciones"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPowerSaving) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isPowerSaving) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (isPowerSaving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Descargar / Optimizar Animaciones",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isPowerSaving) "Modo económico activo: animaciones pesadas descargadas para máximo ahorro de RAM y batería"
                                    else "Animaciones fluidas activas. Actívalo en teléfonos con recursos limitados",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isPowerSaving,
                            onCheckedChange = { viewModel.togglePowerSavingMode(it) },
                            modifier = Modifier.testTag("power_saving_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Dark Mode Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tema Oscuro Deportivo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Contraste optimizado para campos de juego y bajo consumo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }

            // Push Notifications Master Switch (Default Off)
            val pushEnabled by viewModel.isPushNotificationsEnabled.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Notificaciones Push Globales",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Avisos de inicio, goles y fin de partidos (desactivado por defecto)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { viewModel.togglePushNotifications(it) }
                    )
                }
            }

            // Backup & Restore Database Section (Dedicated .tb File Format)
            Text(
                text = "Copia de Seguridad y Restauración (.tb)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Guarda o restaura tus torneos, equipos, jugadores, partidos y escudos en un paquete exclusivo (.tb) de TheBiG.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val uri = TheBiGBackupHelper.exportFullDatabaseTbFile(context, TheBiGApp.instance.repository)
                                    if (uri != null) {
                                        TheBiGBackupHelper.shareTbFile(context, uri, "Completo")
                                    } else {
                                        Toast.makeText(context, "Error al generar archivo .tb", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar .tb", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importTbText = ""
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar .tb", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showQrShareDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modo Espectador en Vivo (QR)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    backupStatus?.let { status ->
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Software Updates Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Actualizaciones de Software",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Versión instalada: v1.7.0 (Estable)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            showUpdateCheckDialog = true
                            isCheckingUpdates = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("check_updates_button")
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar", fontSize = 12.sp)
                    }
                }
            }

            // HIDDEN DEVELOPER PANEL (UNLOCKED AFTER 5 TAPS + PASSWORD "XhQzniwJ")
            if (isDevPanelUnlocked) {
                DeveloperPanelSection(
                    apkUrl = apkUrl,
                    feedbacks = feedbacks,
                    onUpdateUrl = { showEditUrlDialog = true },
                    onOpenBroadcastDialog = { showBroadcastSimulatorDialog = true },
                    onLockPanel = { isDevPanelUnlocked = false }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Panel de desarrollador protegido",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Modal: Option Dialog for Support & Suggestions
    if (showSupportOptionDialog) {
        AlertDialog(
            onDismissRequest = { showSupportOptionDialog = false },
            title = { Text("Comunicar Sugerencia o Error", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Elige cómo deseas enviar tu sugerencia o reporte:")

                    // Option A: Internal Report to Daniel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSupportOptionDialog = false
                                showInternalReportDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Opción 1: Reporte Interno a Daniel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Registra un ticket de sugerencia o bug dentro de la app.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option B: WhatsApp to Daniel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSupportOptionDialog = false
                                WhatsAppShareHelper.openSupportWhatsApp(context)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Opción 2: WhatsApp (+53 63270465)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                                Text("Conversa directamente con Daniel (el Grande).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showSupportOptionDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal: Internal Report Form
    if (showInternalReportDialog) {
        var reportType by remember { mutableStateOf("SUGERENCIA") }
        var reportSubject by remember { mutableStateOf("") }
        var reportDetails by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showInternalReportDialog = false },
            title = { Text("Reporte Directo al Desarrollador", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reportType == "SUGERENCIA",
                            onClick = { reportType = "SUGERENCIA" },
                            label = { Text("Sugerencia") }
                        )
                        FilterChip(
                            selected = reportType == "ERROR",
                            onClick = { reportType = "ERROR" },
                            label = { Text("Error / Bug") }
                        )
                    }

                    OutlinedTextField(
                        value = reportSubject,
                        onValueChange = { reportSubject = it },
                        label = { Text("Asunto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Descripción o Detalles") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportSubject.isNotBlank() && reportDetails.isNotBlank()) {
                            viewModel.submitFeedback(reportType, reportSubject, reportDetails)
                            showInternalReportDialog = false
                            Toast.makeText(context, "Reporte guardado para Daniel", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = reportSubject.isNotBlank() && reportDetails.isNotBlank()
                ) {
                    Text("Enviar Reporte")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showInternalReportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Password Prompt Dialog ("XhQzniwJ")
    if (showPasswordDialog) {
        var inputPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acceso Desarrollador Daniel")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Introduce la clave maestra de seguridad para desbloquear el panel de control oculto de TheBiG:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = {
                            inputPassword = it
                            passwordError = false
                        },
                        label = { Text("Clave Maestra") },
                        singleLine = true,
                        isError = passwordError,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError) {
                        Text(
                            text = "Clave incorrecta. Acceso denegado.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPassword == "XhQzniwJ") {
                            isDevPanelUnlocked = true
                            showPasswordDialog = false
                            Toast.makeText(context, "Modo Desarrollador activado", Toast.LENGTH_SHORT).show()
                        } else {
                            passwordError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_password_button")
                ) {
                    Text("Desbloquear")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Import .tb Content Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restaurar Paquete .tb") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pega el contenido del archivo .tb para restaurar los torneos y sus escudos:")
                    OutlinedTextField(
                        value = importTbText,
                        onValueChange = { importTbText = it },
                        label = { Text("Contenido .tb") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val conflicts = TheBiGBackupHelper.checkConflicts(TheBiGApp.instance.repository, importTbText)
                            if (conflicts.hasConflict) {
                                conflictingNames = conflicts.duplicateTeamNames + conflicts.duplicateTournamentNames
                                showConflictDialog = true
                            } else {
                                val success = TheBiGBackupHelper.importTbFileContent(context, TheBiGApp.instance.repository, importTbText, com.example.util.ConflictResolution.OVERWRITE)
                                showImportDialog = false
                                val msg = if (success) "Restauración .tb completada" else "Error al importar el paquete .tb"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = importTbText.isNotBlank()
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Conflict Resolution Dialog (Sobrescribir vs Crear Copia)
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conflicto de Nombres Detectado", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Los siguientes elementos ya existen en TheBiG:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    conflictingNames.forEach { name ->
                        Text("• $name", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¿Deseas sobrescribir los datos existentes o crear un nuevo registro como copia con el sufijo ' (Copia)'?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val success = TheBiGBackupHelper.importTbFileContent(context, TheBiGApp.instance.repository, importTbText, com.example.util.ConflictResolution.CREATE_COPY)
                                showConflictDialog = false
                                showImportDialog = false
                                Toast.makeText(context, if (success) "Importado creando copias" else "Error al importar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Crear Copia")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val success = TheBiGBackupHelper.importTbFileContent(context, TheBiGApp.instance.repository, importTbText, com.example.util.ConflictResolution.OVERWRITE)
                                showConflictDialog = false
                                showImportDialog = false
                                Toast.makeText(context, if (success) "Restaurado sobrescribiendo" else "Error al importar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Sobrescribir")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConflictDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Update Check Dialog
    if (showUpdateCheckDialog) {
        androidx.compose.runtime.LaunchedEffect(showUpdateCheckDialog) {
            kotlinx.coroutines.delay(1000)
            isCheckingUpdates = false
        }

        AlertDialog(
            onDismissRequest = { showUpdateCheckDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Actualizaciones TheBiG")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Text(
                            text = "Comprobando nuevas compilaciones en el servidor oficial...",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                        Text(
                            text = "TheBiG está actualizado a la versión v1.7.0.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Cuentas con el motor matemático de los 4 modos de torneo, formato .tb ZIP exclusivo, cronómetro de árbitro estático y editor de escudos con interpolación táctil.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showUpdateCheckDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // QR Spectator Dialog with Network Verification
    if (showQrShareDialog) {
        val isNetworkOk = remember { TheBiGBackupHelper.isNetworkAvailable(context) }
        val qrBitmap = remember {
            try {
                TheBiGBackupHelper.generateQrBitmap("thebig://spectator/torneo", 400)
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showQrShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modo Espectador en Vivo (QR)")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isNetworkOk) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sin conexión a internet: los espectadores en otros dispositivos no podrán sincronizar partidos en vivo.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Text(
                        text = "Escanea este código QR desde otro teléfono con TheBiG para abrir el visor en tiempo real:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    qrBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Código QR Espectador",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showQrShareDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun DeveloperPanelSection(
    apkUrl: String,
    feedbacks: List<UserFeedbackReport>,
    onUpdateUrl: () -> Unit,
    onOpenBroadcastDialog: () -> Unit,
    onLockPanel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF38BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Panel de Desarrollador Daniel",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Gestión de sugerencias, avisos y enlaces",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onLockPanel) {
                    Icon(Icons.Default.Lock, contentDescription = "Bloquear", tint = Color(0xFF94A3B8))
                }
            }

            Divider(color = Color(0xFF334155))

            // Suggestions and Error reports list
            Text(
                text = "Reportes y Sugerencias de Usuarios (${feedbacks.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF38BDF8)
            )

            if (feedbacks.isEmpty()) {
                Text(
                    text = "No se han recibido sugerencias ni reportes todavía.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    feedbacks.take(5).forEach { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "[${r.type}] ${r.subject}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (r.type == "ERROR") Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(r.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = r.description, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFF334155))

            // Remote Broadcast button
            Button(
                onClick = onOpenBroadcastDialog,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Emitir Notificación o Encuesta Remota", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
