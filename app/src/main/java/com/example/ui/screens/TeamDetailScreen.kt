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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: Long,
    viewModel: TeamViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val team = teams.firstOrNull { it.id == teamId }

    val playersFlow = remember(teamId) { viewModel.getPlayersForTeam(teamId) }
    val players by playersFlow.collectAsStateWithLifecycle()

    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showEditTeamDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    val scope = rememberCoroutineScope()

    if (team == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Club no encontrado")
        }
        return
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeamCrestView(
                            shieldIndex = team.shieldVectorIndex,
                            primaryColorHex = team.primaryColorHex,
                            secondaryColorHex = team.secondaryColorHex,
                            borderColorHex = team.borderColorHex,
                            shortName = team.shortName,
                            customShieldUri = team.customShieldUri,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(team.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Plantilla: ${players.size}/22 jugadores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditTeamDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Uniforme y Escudo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (players.size >= 22) {
                        errorMessage = "La plantilla ya alcanzó el máximo permitido de 22 jugadores."
                    } else {
                        showAddPlayerDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_player_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Jugador")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Identidad y Uniformes del Club",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamJerseyView(team = team, isGoalkeeper = false, dorsal = 10, size = 42.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Jugador Campo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Titular", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamJerseyView(team = team, isGoalkeeper = true, dorsal = 1, size = 42.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Portero", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("Arquero", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sin jugadores registrados",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agrega entre 5 y 22 jugadores con su rol, dorsal y asigna la cinta de Capitán (C).",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Plantilla de Jugadores (${players.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players, key = { it.id }) { player ->
                        PlayerRowCard(
                            player = player,
                            team = team,
                            onToggleCaptain = { viewModel.toggleCaptain(team.id, player.id, player.isCaptain) },
                            onDelete = { playerToDelete = player }
                        )
                    }
                }
            }
        }
    }

    if (playerToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "al jugador '${playerToDelete!!.name}'",
            onDismiss = { playerToDelete = null },
            onConfirm = {
                viewModel.deletePlayer(playerToDelete!!.id)
                playerToDelete = null
            }
        )
    }

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            currentCount = players.size,
            existingDorsals = players.map { it.dorsal }.toSet(),
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, dorsal, isGk, feePaid, isCaptain ->
                scope.launch {
                    val result = viewModel.addPlayer(team.id, name, dorsal, isGk, feePaid, isCaptain)
                    if (result.isSuccess) {
                        showAddPlayerDialog = false
                    } else {
                        errorMessage = result.exceptionOrNull()?.message
                    }
                }
            }
        )
    }

    if (showEditTeamDialog) {
        EditTeamDialog(
            team = team,
            onDismiss = { showEditTeamDialog = false },
            onConfirm = { updated ->
                viewModel.updateTeam(updated)
                showEditTeamDialog = false
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Aviso de Plantilla", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun PlayerRowCard(
    player: Player,
    team: Team,
    onToggleCaptain: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player.isCaptain) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamJerseyView(
                team = team,
                isGoalkeeper = player.isGoalkeeper,
                dorsal = player.dorsal,
                size = 38.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (player.isCaptain) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "(C) CAPITÁN",
                                color = Color(0xFF0F172A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (player.isGoalkeeper) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Portero",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (!player.isCaptain) {
                        Text(
                            text = "Dorsal #${player.dorsal}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Dorsal #${player.dorsal} • ${if (player.isGoalkeeper) "Portero" else "Jugador de Campo"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleCaptain,
                modifier = Modifier.testTag("captain_button_${player.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = if (player.isCaptain) "Quitar Capitán" else "Designar Capitán (C)",
                    tint = if (player.isCaptain) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar jugador",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun AddPlayerDialog(
    currentCount: Int,
    existingDorsals: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dorsal: Int, isGk: Boolean, feePaid: Boolean, isCaptain: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val suggestedDorsal = remember {
        var num = 1
        while (existingDorsals.contains(num)) num++
        num
    }
    var dorsal by remember { mutableStateOf("$suggestedDorsal") }
    var isGoalkeeper by remember { mutableStateOf(currentCount == 0) }
    var isCaptain by remember { mutableStateOf(false) }
    var feePaid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Jugador", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre y Apellidos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("player_name_input")
                )
                OutlinedTextField(
                    value = dorsal,
                    onValueChange = { dorsal = it },
                    label = { Text("Dorsal Permanente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("player_dorsal_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Es Portero (Arquero):", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isGoalkeeper, onCheckedChange = { isGoalkeeper = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cinta de Capitán (C):", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isCaptain, onCheckedChange = { isCaptain = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cuota de Torneo Pagada:", fontWeight = FontWeight.SemiBold)
                    Switch(checked = feePaid, onCheckedChange = { feePaid = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dNum = dorsal.toIntOrNull() ?: suggestedDorsal
                    if (name.isNotBlank()) {
                        onConfirm(name, dNum, isGoalkeeper, feePaid, isCaptain)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_player_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditTeamDialog(
    team: Team,
    onDismiss: () -> Unit,
    onConfirm: (Team) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(team.name) }
    var shortName by remember { mutableStateOf(team.shortName) }
    var selectedShieldIdx by remember { mutableIntStateOf(team.shieldVectorIndex) }
    var customShieldUri by remember { mutableStateOf(team.customShieldUri) }
    var shieldMode by remember { mutableStateOf(if (team.customShieldUri != null) "GALLERY" else "PRESET") }

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

    var primaryColor by remember { mutableStateOf(team.primaryColorHex) }
    var secondaryColor by remember { mutableStateOf(team.secondaryColorHex) }
    var borderColor by remember { mutableStateOf(team.borderColorHex) }
    var jerseyPattern by remember { mutableStateOf(team.jerseyPattern) }
    var goalkeeperColor by remember { mutableStateOf(team.goalkeeperColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Club y Uniformes", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Visual Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
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
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Escudo", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamJerseyView(
                                primaryColorHex = primaryColor,
                                secondaryColorHex = secondaryColor,
                                jerseyPattern = jerseyPattern,
                                isGoalkeeper = false,
                                dorsal = 10,
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Campo", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TeamJerseyView(
                                primaryColorHex = primaryColor,
                                secondaryColorHex = secondaryColor,
                                goalkeeperColorHex = goalkeeperColor,
                                jerseyPattern = jerseyPattern,
                                isGoalkeeper = true,
                                dorsal = 1,
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Portero", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Club") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { if (it.length <= 4) shortName = it.uppercase() },
                    label = { Text("Siglas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Shield Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (customShieldUri != null && shieldMode == "GALLERY") "Cambiar Foto" else "Desde Galería", fontSize = 11.sp)
                    }

                    if (customShieldUri != null) {
                        Button(
                            onClick = { shieldMode = "PRESET" },
                            modifier = Modifier.weight(1f)
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

                // Primary Color
                Text("Color Principal (Campo):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Yellow)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (primaryColor == hex) 3.dp else 1.dp,
                                    color = if (primaryColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { primaryColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (primaryColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Secondary Color
                Text("Color Secundario / Acentos:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Black)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (secondaryColor == hex) 3.dp else 1.dp,
                                    color = if (secondaryColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { secondaryColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (secondaryColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Goalkeeper Color
                Text("Color Exclusivo del Portero:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gkPresetColors) { hex ->
                        val parsed = TeamCrestHelper.parseColor(hex, Color.Green)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(parsed, CircleShape)
                                .border(
                                    width = if (goalkeeperColor == hex) 3.dp else 1.dp,
                                    color = if (goalkeeperColor == hex) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { goalkeeperColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goalkeeperColor == hex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Jersey Pattern
                Text("Patrón de Camiseta:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val patterns = listOf(
                        JerseyPatternType.SOLID to "Lisa",
                        JerseyPatternType.STRIPES_VERTICAL to "Rayas V",
                        JerseyPatternType.STRIPES_HORIZONTAL to "Franja H",
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        team.copy(
                            name = name,
                            shortName = shortName,
                            shieldVectorIndex = selectedShieldIdx,
                            customShieldUri = if (shieldMode == "GALLERY") customShieldUri else null,
                            primaryColorHex = primaryColor,
                            secondaryColorHex = secondaryColor,
                            borderColorHex = borderColor,
                            jerseyPattern = jerseyPattern,
                            goalkeeperColorHex = goalkeeperColor
                        )
                    )
                }
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
