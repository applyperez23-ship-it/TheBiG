package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.viewmodel.TeamViewModel
import com.example.util.JerseyPatternType
import com.example.util.TeamCrestHelper
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView
import kotlinx.coroutines.launch

@Composable
fun TeamsScreen(
    viewModel: TeamViewModel,
    onTeamClick: (Long) -> Unit
) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_team_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Equipo")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (teams.isEmpty()) {
                // Empty state per restriction: "No hay equipos guardados"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay equipos guardados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón + para registrar un club con su plantilla (de 5 a 22 jugadores), diseñar su uniforme y personalizar su escudo con fotos o vectores.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(teams, key = { it.id }) { team ->
                        TeamCard(
                            team = team,
                            onClick = { onTeamClick(team.id) },
                            onDelete = { teamToDelete = team }
                        )
                    }
                }
            }
        }
    }

    if (teamToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "el club '${teamToDelete!!.name}'",
            onDismiss = { teamToDelete = null },
            onConfirm = {
                viewModel.deleteTeam(teamToDelete!!.id)
                teamToDelete = null
            }
        )
    }

    if (showCreateDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, shortName, shieldIdx, primaryColor, secondaryColor, borderColor, customShieldUri, jerseyPattern, gkColor ->
                scope.launch {
                    val id = viewModel.createTeam(
                        name = name,
                        shortName = shortName,
                        shieldVectorIndex = shieldIdx,
                        primaryColorHex = primaryColor,
                        secondaryColorHex = secondaryColor,
                        borderColorHex = borderColor,
                        customShieldUri = customShieldUri,
                        jerseyPattern = jerseyPattern,
                        goalkeeperColorHex = gkColor
                    )
                    showCreateDialog = false
                    onTeamClick(id)
                }
            }
        )
    }
}

@Composable
private fun TeamCard(
    team: Team,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val app = com.example.TheBiGApp.instance
    var players by remember { mutableStateOf<List<Player>>(emptyList()) }

    LaunchedEffect(team.id) {
        app.repository.getPlayersForTeam(team.id).collect {
            players = it
        }
    }

    val paidCount = players.count { it.feePaid }
    val pendingCount = players.size - paidCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("team_card_${team.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Escudo
            TeamCrestView(
                shieldIndex = team.shieldVectorIndex,
                primaryColorHex = team.primaryColorHex,
                secondaryColorHex = team.secondaryColorHex,
                borderColorHex = team.borderColorHex,
                shortName = team.shortName,
                customShieldUri = team.customShieldUri,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Plantilla: ${players.size} jugadores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Visual Uniform Preview badges (Campo y Portero)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamJerseyView(team = team, isGoalkeeper = false, size = 20.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Campo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamJerseyView(team = team, isGoalkeeper = true, size = 20.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Portero", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (players.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Cobrados: $paidCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pendientes: $pendingCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar equipo",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        shortName: String,
        shieldVectorIndex: Int,
        primaryColorHex: String,
        secondaryColorHex: String,
        borderColorHex: String,
        customShieldUri: String?,
        jerseyPattern: String,
        goalkeeperColorHex: String
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var selectedShieldIdx by remember { mutableIntStateOf(0) }
    var customShieldUri by remember { mutableStateOf<String?>(null) }
    var shieldMode by remember { mutableStateOf("PRESET") } // "PRESET" or "GALLERY"

    // Photo picker launcher (zero-permission standard Android photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedUri = TeamCrestHelper.copyShieldUriToInternal(context, uri)
            customShieldUri = copiedUri
            shieldMode = "GALLERY"
        }
    }

    // Color options presets
    val presetColors = listOf(
        "#F59E0B", "#10B981", "#3B82F6", "#EF4444", "#8B5CF6",
        "#EC4899", "#0F172A", "#FFFFFF", "#F97316", "#14B8A6"
    )

    // Goalkeeper specific colors (differentiated vibrant colors)
    val gkPresetColors = listOf(
        "#10B981", // Neon Green
        "#F97316", // Flame Orange
        "#06B6D4", // Cyan
        "#EC4899", // Neon Pink
        "#EAB308", // Vivid Yellow
        "#1E293B", // Graphite
        "#8B5CF6"  // Violet
    )

    var primaryColor by remember { mutableStateOf("#F59E0B") }
    var secondaryColor by remember { mutableStateOf("#0F172A") }
    var borderColor by remember { mutableStateOf("#FFFFFF") }
    var jerseyPattern by remember { mutableStateOf(JerseyPatternType.SOLID) }
    var goalkeeperColor by remember { mutableStateOf("#10B981") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Club", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Previews Card (Escudo + Camiseta Jugador + Camiseta Portero)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Vista Previa del Club",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TeamCrestView(
                                    shieldIndex = selectedShieldIdx,
                                    primaryColorHex = primaryColor,
                                    secondaryColorHex = secondaryColor,
                                    borderColorHex = borderColor,
                                    shortName = if (shortName.isNotBlank()) shortName else "BIG",
                                    customShieldUri = if (shieldMode == "GALLERY") customShieldUri else null,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Escudo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TeamJerseyView(
                                    primaryColorHex = primaryColor,
                                    secondaryColorHex = secondaryColor,
                                    jerseyPattern = jerseyPattern,
                                    isGoalkeeper = false,
                                    dorsal = 10,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Camiseta Campo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TeamJerseyView(
                                    primaryColorHex = primaryColor,
                                    secondaryColorHex = secondaryColor,
                                    goalkeeperColorHex = goalkeeperColor,
                                    jerseyPattern = jerseyPattern,
                                    isGoalkeeper = true,
                                    dorsal = 1,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Uniforme Portero", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Basic Info
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Club") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("team_name_input")
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { if (it.length <= 4) shortName = it.uppercase() },
                    label = { Text("Siglas (máx 4 letras)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 1. Escudos Personalizados
                Text(
                    text = "Escudo del Club (Galería o Vectorial):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (customShieldUri != null && shieldMode == "GALLERY") "Cambiar Foto" else "Desde Galería", fontSize = 11.sp)
                    }

                    if (customShieldUri != null) {
                        Button(
                            onClick = { shieldMode = "PRESET" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Usar Vectorial", fontSize = 11.sp)
                        }
                    }
                }

                if (shieldMode == "PRESET") {
                    ScrollableTabRow(
                        selectedTabIndex = selectedShieldIdx,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent
                    ) {
                        TeamCrestHelper.PRESET_NAMES.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedShieldIdx == index,
                                onClick = {
                                    selectedShieldIdx = index
                                    shieldMode = "PRESET"
                                },
                                text = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 2. Uniforme de Campo (Camisetas)
                Text(
                    text = "Diseño de Camiseta (Jugadores de Campo):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ScrollableTabRow(
                    selectedTabIndex = JerseyPatternType.PATTERN_OPTIONS.indexOfFirst { it.first == jerseyPattern }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    JerseyPatternType.PATTERN_OPTIONS.forEach { (patternKey, patternLabel) ->
                        Tab(
                            selected = jerseyPattern == patternKey,
                            onClick = { jerseyPattern = patternKey },
                            text = { Text(patternLabel, fontSize = 11.sp) }
                        )
                    }
                }

                Text(
                    text = "Color Primario de la Camiseta:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                ColorSelectorRow(selectedColor = primaryColor, colors = presetColors, onSelect = { primaryColor = it })

                Text(
                    text = "Color Secundario / Franjas / Detalles:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                ColorSelectorRow(selectedColor = secondaryColor, colors = presetColors, onSelect = { secondaryColor = it })

                // 3. Uniforme Exclusivo de Portero
                Text(
                    text = "Color Exclusivo del Portero:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Diferenciado del resto del equipo según reglamento.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorSelectorRow(selectedColor = goalkeeperColor, colors = gkPresetColors, onSelect = { goalkeeperColor = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val sn = if (shortName.isNotBlank()) shortName else name.take(3).uppercase()
                        val finalUri = if (shieldMode == "GALLERY") customShieldUri else null
                        onConfirm(
                            name,
                            sn,
                            selectedShieldIdx,
                            primaryColor,
                            secondaryColor,
                            borderColor,
                            finalUri,
                            jerseyPattern,
                            goalkeeperColor
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_team_button")
            ) {
                Text("Crear Club")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ColorSelectorRow(
    selectedColor: String,
    colors: List<String>,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { hex ->
            val color = TeamCrestHelper.parseColor(hex, Color.Gray)
            val isSelected = selectedColor.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color, CircleShape)
                    .clickable { onSelect(hex) }
                    .then(
                        if (isSelected) Modifier.background(Color.White.copy(alpha = 0.5f), CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (hex == "#FFFFFF" || hex == "#EAB308") Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
