package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.util.ImageEnhancer
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
                Icon(Icons.Default.Add, contentDescription = "Crear Club")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (teams.isEmpty()) {
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
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay equipos registrados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón '+' para registrar un equipo con sus colores, escudo vectorial o foto y uniforme.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Clubes Registrados (${teams.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(teams, key = { it.id }) { team ->
                        TeamCard(
                            team = team,
                            viewModel = viewModel,
                            onClick = { onTeamClick(team.id) },
                            onDelete = { teamToDelete = team }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        com.example.ui.components.CreateTeamWizardDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, shortName, shieldIdx, pColor, sColor, bColor, customUri, pattern, gkColor, roster ->
                scope.launch {
                    val id = viewModel.createTeam(
                        name = name,
                        shortName = shortName,
                        shieldVectorIndex = shieldIdx,
                        primaryColorHex = pColor,
                        secondaryColorHex = sColor,
                        borderColorHex = bColor,
                        customShieldUri = customUri,
                        jerseyPattern = pattern,
                        goalkeeperColorHex = gkColor
                    )
                    // Add initial roster
                    for (dp in roster) {
                        viewModel.addPlayer(
                            teamId = id,
                            name = dp.name,
                            dorsal = dp.number,
                            isGoalkeeper = dp.isGoalkeeper,
                            feePaid = false,
                            isCaptain = dp.isCaptain
                        )
                    }
                    showCreateDialog = false
                    onTeamClick(id)
                }
            }
        )
    }

    if (teamToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "al club '${teamToDelete!!.name}' y toda su plantilla",
            onDismiss = { teamToDelete = null },
            onConfirm = {
                viewModel.deleteTeam(teamToDelete!!.id)
                teamToDelete = null
            }
        )
    }
}

@Composable
private fun TeamCard(
    team: Team,
    viewModel: TeamViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val playersFlow = remember(team.id) { viewModel.getPlayersForTeam(team.id) }
    val players by playersFlow.collectAsStateWithLifecycle()

    val paidCount = players.count { it.feePaid }
    val captain = players.firstOrNull { it.isCaptain }

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
                    text = "Siglas: [${team.shortName}] • ${players.size} Jugadores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (captain != null) {
                    Text(
                        text = "(C) Capitán: ${captain.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TeamJerseyView(
                team = team,
                isGoalkeeper = false,
                dorsal = null,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_team_button_${team.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar club",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CreateTeamDialog(
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
    var shieldMode by remember { mutableStateOf("PRESET") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val enhancedUri = ImageEnhancer.processAndSaveEnhancedUri(context, uri)
                    ?: TeamCrestHelper.copyShieldUriToInternal(context, uri)
                if (enhancedUri != null) {
                    customShieldUri = enhancedUri
                    shieldMode = "GALLERY"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val presetColors = listOf(
        "#F59E0B", "#10B981", "#3B82F6", "#EF4444", "#8B5CF6",
        "#EC4899", "#0F172A", "#FFFFFF", "#F97316", "#14B8A6"
    )

    val gkPresetColors = listOf(
        "#10B981", "#F97316", "#06B6D4", "#EC4899", "#EAB308", "#1E293B", "#8B5CF6"
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

                Text(
                    text = "Escudo del Club (Galería con Filtro IA o Vectorial):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
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
                        selectedTabIndex = selectedShieldIdx % 10,
                        edgePadding = 0.dp
                    ) {
                        TeamCrestHelper.PRESET_NAMES.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedShieldIdx == index,
                                onClick = { selectedShieldIdx = index },
                                text = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Primary Color Selector
                Text(
                    text = "Color Principal (Jugadores de Campo):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Yellow)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (primaryColor == hex) 3.dp else 1.dp,
                                    color = if (primaryColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { primaryColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (primaryColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Secondary Color Selector
                Text(
                    text = "Color Secundario / Acentos:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Black)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (secondaryColor == hex) 3.dp else 1.dp,
                                    color = if (secondaryColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { secondaryColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (secondaryColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Goalkeeper Color Selector
                Text(
                    text = "Color Exclusivo del Portero:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gkPresetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Green)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (goalkeeperColor == hex) 3.dp else 1.dp,
                                    color = if (goalkeeperColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { goalkeeperColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goalkeeperColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Text(
                    text = "Patrón de Camiseta:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val patterns = listOf(
                        JerseyPatternType.SOLID to "Lisa",
                        JerseyPatternType.STRIPES_VERTICAL to "Rayas V",
                        JerseyPatternType.STRIPES_HORIZONTAL to "Franja H",
                        JerseyPatternType.SASH to "Banda",
                        JerseyPatternType.HALF to "Mitad"
                    )
                    patterns.forEach { (pKey, pLabel) ->
                        androidx.compose.material3.FilterChip(
                            selected = jerseyPattern == pKey,
                            onClick = { jerseyPattern = pKey },
                            label = { Text(pLabel, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            if (shortName.isNotBlank()) shortName else name.take(3).uppercase(),
                            selectedShieldIdx,
                            primaryColor,
                            secondaryColor,
                            borderColor,
                            if (shieldMode == "GALLERY") customShieldUri else null,
                            jerseyPattern,
                            goalkeeperColor
                        )
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_create_team_button")
            ) {
                Text("Crear Club")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
