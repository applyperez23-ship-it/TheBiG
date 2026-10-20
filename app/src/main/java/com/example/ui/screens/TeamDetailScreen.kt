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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

    val paidCount = players.count { it.feePaid }
    val pendingCount = players.size - paidCount

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
                    if (players.size < 22) {
                        showAddPlayerDialog = true
                    } else {
                        errorMessage = "La plantilla ya alcanzó el límite máximo reglamentario de 22 jugadores"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Club Visual Identity Card (Uniforms & Shield)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Uniformes del Club",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Patrón: ${formatPatternName(team.jerseyPattern)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Outfield Jersey
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamJerseyView(team = team, isGoalkeeper = false, dorsal = 10, size = 42.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Jugador Campo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Uniforme Titular", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Goalkeeper Jersey
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeamJerseyView(team = team, isGoalkeeper = true, dorsal = 1, size = 42.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Portero", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("Exclusivo Arquero", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Financial Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Balance Financiero",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        val statusText = when {
                            players.size < 5 -> "Mínimo 5 requerido (${players.size}/5)"
                            players.size == 22 -> "Plantilla Completa (22/22)"
                            else -> "Reglamentario (${players.size}/22)"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (players.size < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Cuotas Cobradas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$paidCount jugadores", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Cuotas Pendientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$pendingCount jugadores",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
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
                            text = "Agrega entre 5 y 22 jugadores con su rol (Campo o Portero) y su dorsal permanente.",
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
                            onToggleFee = { viewModel.togglePlayerFee(player.id, player.feePaid) },
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
            onConfirm = { name, dorsal, isGk, feePaid ->
                scope.launch {
                    val result = viewModel.addPlayer(teamId, name, dorsal, isGk, feePaid)
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

private fun formatPatternName(pattern: String): String {
    return when (pattern) {
        JerseyPatternType.SOLID -> "Lisa"
        JerseyPatternType.STRIPES_VERTICAL -> "Franjas Verticales"
        JerseyPatternType.STRIPES_HORIZONTAL -> "Franjas Horizontales"
        JerseyPatternType.SASH -> "Banda Diagonal"
        JerseyPatternType.HALF -> "Mitad y Mitad"
        else -> pattern
    }
}

@Composable
private fun PlayerRowCard(
    player: Player,
    team: Team,
    onToggleFee: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature graphic vector of the team's jersey with player's permanent dorsal!
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
                    } else {
                        Text(
                            text = "Dorsal #${player.dorsal}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (player.feePaid) "Cuota Pagada" else "Cuota Pendiente",
                    fontSize = 11.sp,
                    color = if (player.feePaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Fee Checkbox
            Checkbox(
                checked = player.feePaid,
                onCheckedChange = { onToggleFee() }
            )

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
    onConfirm: (name: String, dorsal: Int, isGk: Boolean, feePaid: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // Auto suggest next unused dorsal number
    val suggestedDorsal = remember {
        var num = 1
        while (existingDorsals.contains(num)) num++
        num
    }
    var dorsal by remember { mutableStateOf("$suggestedDorsal") }
    var isGoalkeeper by remember { mutableStateOf(currentCount == 0) } // default first player to goalie
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
                    label = { Text("Número de Dorsal Permanente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rol: Portero", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isGoalkeeper,
                        onCheckedChange = { isGoalkeeper = it }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cuota Pagada", fontWeight = FontWeight.SemiBold)
                    Checkbox(
                        checked = feePaid,
                        onCheckedChange = { feePaid = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val d = dorsal.toIntOrNull() ?: suggestedDorsal
                        onConfirm(name, d, isGoalkeeper, feePaid)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_player_button")
            ) {
                Text("Registrar")
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
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedUri = TeamCrestHelper.copyShieldUriToInternal(context, uri)
            customShieldUri = copiedUri
            shieldMode = "GALLERY"
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
        title = { Text("Personalizar Club y Uniformes", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamCrestView(
                        shieldIndex = selectedShieldIdx,
                        primaryColorHex = primaryColor,
                        secondaryColorHex = secondaryColor,
                        borderColorHex = borderColor,
                        shortName = shortName,
                        customShieldUri = if (shieldMode == "GALLERY") customShieldUri else null,
                        size = 50.dp
                    )
                    TeamJerseyView(
                        primaryColorHex = primaryColor,
                        secondaryColorHex = secondaryColor,
                        jerseyPattern = jerseyPattern,
                        isGoalkeeper = false,
                        dorsal = 10,
                        size = 50.dp
                    )
                    TeamJerseyView(
                        primaryColorHex = primaryColor,
                        secondaryColorHex = secondaryColor,
                        goalkeeperColorHex = goalkeeperColor,
                        jerseyPattern = jerseyPattern,
                        isGoalkeeper = true,
                        dorsal = 1,
                        size = 50.dp
                    )
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

                Text("Escudo (Galería o Vectorial):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                        modifier = Modifier.weight(1f)
                    ) {
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

                Text("Diseño Camiseta (Jugadores Campo):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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

                Text("Color Primario:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                ColorSelectorRow(selectedColor = primaryColor, colors = presetColors, onSelect = { primaryColor = it })

                Text("Color Secundario:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                ColorSelectorRow(selectedColor = secondaryColor, colors = presetColors, onSelect = { secondaryColor = it })

                Text("Color Exclusivo del Portero:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                ColorSelectorRow(selectedColor = goalkeeperColor, colors = gkPresetColors, onSelect = { goalkeeperColor = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUri = if (shieldMode == "GALLERY") customShieldUri else null
                    onConfirm(
                        team.copy(
                            name = name.trim(),
                            shortName = shortName.trim().uppercase(),
                            shieldVectorIndex = selectedShieldIdx,
                            primaryColorHex = primaryColor,
                            secondaryColorHex = secondaryColor,
                            borderColorHex = borderColor,
                            customShieldUri = finalUri,
                            jerseyPattern = jerseyPattern,
                            goalkeeperColorHex = goalkeeperColor
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
