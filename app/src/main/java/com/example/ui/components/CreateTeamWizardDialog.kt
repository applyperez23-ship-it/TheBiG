package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.Player
import com.example.util.JerseyPatternType
import com.example.util.TeamCrestHelper
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView

data class DraftPlayer(
    val number: Int,
    val name: String,
    val isGoalkeeper: Boolean,
    val isCaptain: Boolean
)

@Composable
fun CreateTeamWizardDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        shortName: String,
        shieldIndex: Int,
        primaryColorHex: String,
        secondaryColorHex: String,
        borderColorHex: String,
        customShieldUri: String?,
        jerseyPattern: String,
        goalkeeperColorHex: String,
        roster: List<DraftPlayer>
    ) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Identidad, 2: Uniforme, 3: Plantilla

    // Form states
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var shieldMode by remember { mutableStateOf("PRESET") } // PRESET, GALLERY
    var selectedShieldIdx by remember { mutableIntStateOf(0) }
    var customShieldUri by remember { mutableStateOf<String?>(null) }
    var rawGalleryBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showShieldEditor by remember { mutableStateOf(false) }

    var primaryColor by remember { mutableStateOf("#0284C7") }
    var secondaryColor by remember { mutableStateOf("#0F172A") }
    var borderColor by remember { mutableStateOf("#FFFFFF") }
    var goalkeeperColor by remember { mutableStateOf("#10B981") }
    var jerseyPattern by remember { mutableStateOf(JerseyPatternType.STRIPES_VERTICAL) }

    val roster = remember { mutableStateListOf<DraftPlayer>() }
    var newPlayerNumber by remember { mutableStateOf("") }
    var newPlayerName by remember { mutableStateOf("") }
    var newPlayerIsGk by remember { mutableStateOf(false) }
    var newPlayerIsCap by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4",
        "#0284C7", "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
        "#0F172A", "#FFFFFF", "#64748B", "#78350F", "#14532D"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val internalPath = TeamCrestHelper.copyShieldUriToInternal(context, it)
                customShieldUri = internalPath
                shieldMode = "GALLERY"

                // Load bitmap for optional background editing
                context.contentResolver.openInputStream(it)?.use { stream ->
                    rawGalleryBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showShieldEditor && rawGalleryBitmap != null) {
        ShieldEditorDialog(
            initialBitmap = rawGalleryBitmap!!,
            onDismiss = { showShieldEditor = false },
            onSaveShield = { editedUri ->
                customShieldUri = editedUri.toString()
                showShieldEditor = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(680.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (step) {
                                1 -> "Paso 1: Identidad del Club"
                                2 -> "Paso 2: Colores y Uniforme"
                                else -> "Paso 3: Plantilla Inicial"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paso $step de 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                }

                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Step Contents Animated
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (step) {
                        1 -> {
                            // Step 1: Identidad
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Live Crest Preview
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TeamCrestView(
                                        shieldIndex = selectedShieldIdx,
                                        primaryColorHex = primaryColor,
                                        secondaryColorHex = secondaryColor,
                                        borderColorHex = borderColor,
                                        shortName = if (shortName.isNotBlank()) shortName else name.take(3),
                                        customShieldUri = if (shieldMode == "GALLERY") customShieldUri else null,
                                        size = 80.dp
                                    )
                                }

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nombre del Club") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wizard_team_name_input")
                                )

                                OutlinedTextField(
                                    value = shortName,
                                    onValueChange = { if (it.length <= 4) shortName = it.uppercase() },
                                    label = { Text("Siglas (ej: RMA, BAR, FCB)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Escudo Oficial:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Subir Foto", fontSize = 12.sp)
                                    }

                                    if (customShieldUri != null) {
                                        Button(
                                            onClick = { showShieldEditor = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Editar Fondo", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "O elige un Escudo Vectorial:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Grid of 10 presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(TeamCrestHelper.PRESET_NAMES.size) { idx ->
                                            val isSel = shieldMode == "PRESET" && selectedShieldIdx == idx
                                            Card(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clickable {
                                                        shieldMode = "PRESET"
                                                        selectedShieldIdx = idx
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = if (isSel) 2.5.dp else 1.dp,
                                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    TeamCrestView(
                                                        shieldIndex = idx,
                                                        primaryColorHex = primaryColor,
                                                        secondaryColorHex = secondaryColor,
                                                        borderColorHex = borderColor,
                                                        shortName = "",
                                                        size = 40.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Step 2: Colores y Uniforme
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Live Jersey Previews
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        TeamJerseyView(
                                            primaryColorHex = primaryColor,
                                            secondaryColorHex = secondaryColor,
                                            jerseyPattern = jerseyPattern,
                                            isGoalkeeper = false,
                                            dorsal = 10,
                                            size = 60.dp
                                        )
                                        Text("Jugador de Campo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        TeamJerseyView(
                                            primaryColorHex = goalkeeperColor,
                                            secondaryColorHex = "#1E293B",
                                            jerseyPattern = JerseyPatternType.SOLID,
                                            isGoalkeeper = true,
                                            dorsal = 1,
                                            size = 60.dp
                                        )
                                        Text("Portero Oficial", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }

                                Text("Color Principal (Campo):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetColors) { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(TeamCrestHelper.parseColor(hex, Color.Blue), CircleShape)
                                                .border(if (primaryColor == hex) 3.dp else 1.dp, if (primaryColor == hex) MaterialTheme.colorScheme.primary else Color.White, CircleShape)
                                                .clickable { primaryColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (primaryColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text("Color Secundario / Rayas:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetColors) { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(TeamCrestHelper.parseColor(hex, Color.Black), CircleShape)
                                                .border(if (secondaryColor == hex) 3.dp else 1.dp, if (secondaryColor == hex) MaterialTheme.colorScheme.primary else Color.White, CircleShape)
                                                .clickable { secondaryColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (secondaryColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text("Color Exclusivo del Portero:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(presetColors) { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(TeamCrestHelper.parseColor(hex, Color.Green), CircleShape)
                                                .border(if (goalkeeperColor == hex) 3.dp else 1.dp, if (goalkeeperColor == hex) MaterialTheme.colorScheme.secondary else Color.White, CircleShape)
                                                .clickable { goalkeeperColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (goalkeeperColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Text("Patrón de Camiseta:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val patterns = listOf(
                                        JerseyPatternType.SOLID to "Lisa",
                                        JerseyPatternType.STRIPES_VERTICAL to "Rayas",
                                        JerseyPatternType.STRIPES_HORIZONTAL to "Franja",
                                        JerseyPatternType.SASH to "Banda",
                                        JerseyPatternType.HALF to "Mitad"
                                    )
                                    patterns.forEach { (pKey, pLabel) ->
                                        FilterChip(
                                            selected = jerseyPattern == pKey,
                                            onClick = { jerseyPattern = pKey },
                                            label = { Text(pLabel, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Step 3: Plantilla Inicial
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Registrar Integrantes (${roster.size} agregados):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                // Add player row
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newPlayerNumber,
                                                onValueChange = { if (it.length <= 3) newPlayerNumber = it },
                                                label = { Text("#") },
                                                singleLine = true,
                                                modifier = Modifier.width(64.dp)
                                            )
                                            OutlinedTextField(
                                                value = newPlayerName,
                                                onValueChange = { newPlayerName = it },
                                                label = { Text("Nombre Jugador") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChip(
                                                selected = newPlayerIsGk,
                                                onClick = { newPlayerIsGk = !newPlayerIsGk },
                                                label = { Text(if (newPlayerIsGk) "Es Portero" else "Jugador Campo", fontSize = 11.sp) }
                                            )

                                            FilterChip(
                                                selected = newPlayerIsCap,
                                                onClick = { newPlayerIsCap = !newPlayerIsCap },
                                                label = { Text(if (newPlayerIsCap) "Capitán (C)" else "Sin cinta", fontSize = 11.sp) }
                                            )

                                            Button(
                                                onClick = {
                                                    val num = newPlayerNumber.toIntOrNull() ?: (roster.size + 1)
                                                    if (newPlayerName.isNotBlank()) {
                                                        roster.add(DraftPlayer(num, newPlayerName.trim(), newPlayerIsGk, newPlayerIsCap))
                                                        newPlayerNumber = ""
                                                        newPlayerName = ""
                                                        newPlayerIsGk = false
                                                        newPlayerIsCap = false
                                                    }
                                                },
                                                enabled = newPlayerName.isNotBlank(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Agregar", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                // List of added players
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(roster) { index, player ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("#${player.number}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(player.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                    if (player.isCaptain) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("(C)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                    }
                                                    if (player.isGoalkeeper) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("[POR]", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { roster.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Atrás")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                    }

                    if (step < 3) {
                        Button(
                            onClick = { step++ },
                            enabled = if (step == 1) name.isNotBlank() else true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_team_next_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Siguiente")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                onConfirm(
                                    name.trim(),
                                    if (shortName.isNotBlank()) shortName.trim() else name.take(3).uppercase(),
                                    selectedShieldIdx,
                                    primaryColor,
                                    secondaryColor,
                                    borderColor,
                                    if (shieldMode == "GALLERY") customShieldUri else null,
                                    jerseyPattern,
                                    goalkeeperColor,
                                    roster.toList()
                                )
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_team_finish_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear Club")
                        }
                    }
                }
            }
        }
    }
}
