package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TheBiGLogoVector
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.WhatsAppShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isPowerSaving by viewModel.isPowerSavingMode.collectAsStateWithLifecycle()
    val apkUrl by viewModel.apkDownloadUrl.collectAsStateWithLifecycle()
    val activeNotice by viewModel.activeBroadcast.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    // 5-tap secret trigger on version string
    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isDevPanelUnlocked by remember { mutableStateOf(false) }

    // Dialogs
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }
    var showBroadcastSimulatorDialog by remember { mutableStateOf(false) }
    var showEditUrlDialog by remember { mutableStateOf(false) }

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
            // App Branding Header
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
                            text = "Desarrollado por Daniel (el Grande)",
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
                                            "Toca $remaining veces mas para acceso desarrollador",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Version Oficial 1.0 (Build 2026)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                                    text = if (isPowerSaving) "Modo economico activo: animaciones pesadas descargadas para maximo ahorro de RAM y bateria"
                                    else "Animaciones fluidas activas. Activalo en dispositivos de gama de entrada",
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

            // WhatsApp Direct Support Card (+53 63270465 - Daniel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Soporte Oficial WhatsApp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "+53 63270465 - Daniel (el Grande)",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Contacta directamente con el creador para sugerencias, reportes o solicitud de nuevas funciones en TheBiG.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { WhatsAppShareHelper.openSupportWhatsApp(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_support_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chatear con Daniel (+53 63270465)")
                    }
                }
            }

            // Backup & Restore Database Section
            Text(
                text = "Copia de Seguridad y Restauracion",
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
                        text = "Guarda o transfiere todos tus torneos, equipos, jugadores, partidos e incidencias en un archivo JSON portable.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportBackup { json ->
                                    exportedJsonText = json
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar Copia", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importJsonText = ""
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar Copia", fontSize = 12.sp)
                        }
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

            // HIDDEN DEVELOPER PANEL (UNLOCKED AFTER 5 TAPS + PASSWORD "XhQzniwJ")
            if (isDevPanelUnlocked) {
                DeveloperPanelSection(
                    apkUrl = apkUrl,
                    onUpdateUrl = { showEditUrlDialog = true },
                    onOpenBroadcastDialog = { showBroadcastSimulatorDialog = true },
                    onLockPanel = { isDevPanelUnlocked = false }
                )
            } else {
                // Subtle hint about developer mode
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
                            Toast.makeText(context, "Modo Desarrollador Daniel activado con exito", Toast.LENGTH_SHORT).show()
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

    // Edit APK URL Dialog
    if (showEditUrlDialog) {
        var newUrlText by remember { mutableStateOf(apkUrl) }
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = { Text("Actualizador Dinamico del APK") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modifica el enlace directo de descarga del nuevo archivo APK:")
                    OutlinedTextField(
                        value = newUrlText,
                        onValueChange = { newUrlText = it },
                        label = { Text("URL Directa del APK") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setApkDownloadUrl(newUrlText.trim())
                        showEditUrlDialog = false
                        Toast.makeText(context, "URL de APK actualizada", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Guardar URL")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Remote Broadcast / Survey Dialog Simulator
    if (showBroadcastSimulatorDialog) {
        var noticeTitle by remember { mutableStateOf("Novedad en TheBiG") }
        var noticeMessage by remember { mutableStateOf("Ya esta disponible el fixture y la tabla de posiciones con descarga directa.") }
        var isSurvey by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBroadcastSimulatorDialog = false },
            title = { Text("Enviar Notificacion / Encuesta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Titulo del Aviso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noticeMessage,
                        onValueChange = { noticeMessage = it },
                        label = { Text("Mensaje o Pregunta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = isSurvey,
                            onCheckedChange = { isSurvey = it }
                        )
                        Text("Habilitar como Encuesta de Votacion", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.broadcastNotice(
                            title = noticeTitle,
                            message = noticeMessage,
                            isSurvey = isSurvey,
                            options = if (isSurvey) listOf("Excelente", "Mejorar", "Sugerir cambio") else emptyList()
                        )
                        showBroadcastSimulatorDialog = false
                        Toast.makeText(context, "Aviso transmitido a los usuarios", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Emitir Aviso")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBroadcastSimulatorDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Active Remote Broadcast Dialog (shown when broadcast exists)
    activeNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissBroadcast() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (notice.isSurvey) Icons.Default.Poll else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(notice.title)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notice.message)
                    if (notice.isSurvey) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Opciones de Votacion:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        notice.options.forEach { opt ->
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Voto registrado: $opt", Toast.LENGTH_SHORT).show()
                                    viewModel.dismissBroadcast()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(opt)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissBroadcast() }) {
                    Text("Cerrar Aviso")
                }
            }
        )
    }

    // Export Backup Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Copia de Seguridad Generada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copia el texto JSON completo para guardarlo en un archivo de respaldo:")
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TheBiG_Backup", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copia copiada al portapapeles", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text("Copiar al Portapapeles")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restaurar Copia de Seguridad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pega el texto JSON de tu copia de seguridad. Esto agregara los registros existentes:")
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Contenido JSON") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackup(importJsonText) { success ->
                            showImportDialog = false
                            val msg = if (success) "Restauracion completada exitosamente" else "Error al importar el formato JSON"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = importJsonText.isNotBlank()
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
}

@Composable
fun DeveloperPanelSection(
    apkUrl: String,
    onUpdateUrl: () -> Unit,
    onOpenBroadcastDialog: () -> Unit,
    onLockPanel: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B) // Dark slate developer accent
        )
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
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Panel de Desarrollador Daniel",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Modo Maestro Activo (Clave: XhQzniwJ)",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onLockPanel) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloquear Panel",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Divider(color = Color(0xFF334155))

            // APK Dynamic Updater
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Actualizador Dinamico de la App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = "URL Actual: $apkUrl",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUpdateUrl,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar URL APK", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "URL invalida: $apkUrl", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar Link", fontSize = 11.sp)
                    }
                }
            }

            Divider(color = Color(0xFF334155))

            // Remote Notifications & Surveys
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Notificaciones Remotas y Encuestas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = "Emite mensajes emergentes o encuestas de votacion a todos los usuarios de la aplicacion.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = onOpenBroadcastDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emitir Aviso / Encuesta a Usuarios", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
