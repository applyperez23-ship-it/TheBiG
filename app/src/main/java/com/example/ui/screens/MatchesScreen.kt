package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.TheBiGApp
import com.example.data.local.entity.Match
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.viewmodel.MatchViewModel
import com.example.util.TeamCrestView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    viewModel: MatchViewModel,
    allTeams: List<Team>,
    onMatchClick: (Long) -> Unit
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val app = TheBiGApp.instance

    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var selectedFilterStatus by remember { mutableStateOf("ALL") }
    var selectedTournamentFilter by remember { mutableLongStateOf(0L) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<Match?>(null) }

    LaunchedEffect(Unit) {
        app.repository.allTournaments.collect { tournaments = it }
    }

    val filteredMatches = matches.filter { m ->
        val statusMatches = when (selectedFilterStatus) {
            "LIVE" -> m.status == "LIVE"
            "SCHEDULED" -> m.status == "SCHEDULED"
            "FINISHED" -> m.status == "FINISHED"
            else -> true
        }
        val tournamentMatches = if (selectedTournamentFilter == 0L) true else m.tournamentId == selectedTournamentFilter
        statusMatches && tournamentMatches
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Partidos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${filteredMatches.size} encuentros registrados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_match_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Partido")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterStatus == "ALL",
                        onClick = { selectedFilterStatus = "ALL" },
                        label = { Text("Todos") },
                        modifier = Modifier.testTag("filter_all_status")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterStatus == "LIVE",
                        onClick = { selectedFilterStatus = "LIVE" },
                        label = { Text("En Vivo") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_live_status")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterStatus == "SCHEDULED",
                        onClick = { selectedFilterStatus = "SCHEDULED" },
                        label = { Text("Programados") },
                        modifier = Modifier.testTag("filter_scheduled_status")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterStatus == "FINISHED",
                        onClick = { selectedFilterStatus = "FINISHED" },
                        label = { Text("Finalizados") },
                        modifier = Modifier.testTag("filter_finished_status")
                    )
                }
            }

            // Tournament Filter Chips (if multiple tournaments exist)
            if (tournaments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTournamentFilter == 0L,
                            onClick = { selectedTournamentFilter = 0L },
                            label = { Text("Todos los Torneos") },
                            modifier = Modifier.testTag("filter_all_tournaments")
                        )
                    }
                    items(tournaments) { tourney ->
                        FilterChip(
                            selected = selectedTournamentFilter == tourney.id,
                            onClick = { selectedTournamentFilter = tourney.id },
                            label = { Text(tourney.name) },
                            modifier = Modifier.testTag("filter_tournament_${tourney.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredMatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay partidos registrados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Programa un nuevo partido con el boton (+) o genera el fixture desde un torneo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp
                    )
                ) {
                    items(filteredMatches, key = { it.id }) { match ->
                        val homeTeam = allTeams.firstOrNull { it.id == match.homeTeamId }
                        val awayTeam = allTeams.firstOrNull { it.id == match.awayTeamId }
                        val tourney = tournaments.firstOrNull { it.id == match.tournamentId }

                        MatchCardItem(
                            match = match,
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            tournamentName = tourney?.name ?: "Torneo TheBiG",
                            onClick = { onMatchClick(match.id) },
                            onDelete = { matchToDelete = match }
                        )
                    }
                }
            }
        }
    }

    // Defensive Deletion Dialog
    matchToDelete?.let { m ->
        val hName = allTeams.firstOrNull { it.id == m.homeTeamId }?.name ?: "Local"
        val aName = allTeams.firstOrNull { it.id == m.awayTeamId }?.name ?: "Visitante"
        DeleteConfirmationDialog(
            title = "Eliminar Partido",
            message = "Estas seguro de eliminar el partido $hName vs $aName? Esta accion es irreversible y borrara sus goles y estadisticas asociadas.",
            onConfirm = {
                viewModel.deleteMatch(m.id)
                matchToDelete = null
            },
            onDismiss = {
                matchToDelete = null
            }
        )
    }

    // Create Match Dialog
    if (showCreateDialog) {
        CreateMatchDialog(
            tournaments = tournaments,
            allTeams = allTeams,
            onDismiss = { showCreateDialog = false },
            onCreate = { tourneyId, homeId, awayId, round ->
                scope.launch {
                    viewModel.createMatch(tourneyId, homeId, awayId, round)
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun MatchCardItem(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    tournamentName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Tournament + Round + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$tournamentName - ${match.roundName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (match.status) {
                        "LIVE" -> {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF10B981).copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "EN VIVO",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        "FINISHED" -> {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FINALIZADO",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "PROGRAMADO",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Partido",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score and Teams Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamCrestView(team = homeTeam, size = 36.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = homeTeam?.name ?: "Local",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                // Score / VS
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (match.status == "SCHEDULED") {
                        Text(
                            text = "VS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${match.homeScore} - ${match.awayScore}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (match.homePenalties != null && match.awayPenalties != null) {
                                Text(
                                    text = "P: ${match.homePenalties} - ${match.awayPenalties}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Away Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = awayTeam?.name ?: "Visitante",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TeamCrestView(team = awayTeam, size = 36.dp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info: Period or Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeInfo = when (match.status) {
                    "LIVE" -> "En Juego: Minuto ${match.elapsedSeconds / 60}'"
                    "FINISHED" -> "Finalizado (${match.elapsedSeconds / 60}' jugados)"
                    else -> "Pendiente por iniciar"
                }
                Text(
                    text = timeInfo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Text(
                        text = "Acta del Partido",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchDialog(
    tournaments: List<Tournament>,
    allTeams: List<Team>,
    onDismiss: () -> Unit,
    onCreate: (Long, Long, Long, String) -> Unit
) {
    if (tournaments.isEmpty() || allTeams.size < 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Requisitos Previos") },
            text = {
                Text("Para programar un partido necesitas tener al menos 1 torneo creado y como minimo 2 equipos registrados.")
            },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Entendido") }
            }
        )
        return
    }

    var selectedTournament by remember { mutableStateOf(tournaments.first()) }
    var homeTeam by remember { mutableStateOf(allTeams.first()) }
    var awayTeam by remember { mutableStateOf(allTeams.getOrElse(1) { allTeams.first() }) }
    var roundName by remember { mutableStateOf("Jornada 1") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var expandedTourney by remember { mutableStateOf(false) }
    var expandedHome by remember { mutableStateOf(false) }
    var expandedAway by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Partido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Select Tournament
                ExposedDropdownMenuBox(
                    expanded = expandedTourney,
                    onExpandedChange = { expandedTourney = !expandedTourney }
                ) {
                    OutlinedTextField(
                        value = selectedTournament.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Torneo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTourney) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTourney,
                        onDismissRequest = { expandedTourney = false }
                    ) {
                        tournaments.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    selectedTournament = t
                                    expandedTourney = false
                                }
                            )
                        }
                    }
                }

                // Select Home Team
                ExposedDropdownMenuBox(
                    expanded = expandedHome,
                    onExpandedChange = { expandedHome = !expandedHome }
                ) {
                    OutlinedTextField(
                        value = homeTeam.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipo Local") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHome) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedHome,
                        onDismissRequest = { expandedHome = false }
                    ) {
                        allTeams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    homeTeam = team
                                    expandedHome = false
                                }
                            )
                        }
                    }
                }

                // Select Away Team
                ExposedDropdownMenuBox(
                    expanded = expandedAway,
                    onExpandedChange = { expandedAway = !expandedAway }
                ) {
                    OutlinedTextField(
                        value = awayTeam.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipo Visitante") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAway) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAway,
                        onDismissRequest = { expandedAway = false }
                    ) {
                        allTeams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    awayTeam = team
                                    expandedAway = false
                                }
                            )
                        }
                    }
                }

                // Round name
                OutlinedTextField(
                    value = roundName,
                    onValueChange = { roundName = it },
                    label = { Text("Jornada o Fase") },
                    placeholder = { Text("Ej. Jornada 1, Semifinal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (homeTeam.id == awayTeam.id) {
                        errorMessage = "El equipo local y el visitante deben ser distintos"
                    } else if (roundName.isBlank()) {
                        errorMessage = "Ingresa el nombre de la jornada o fase"
                    } else {
                        onCreate(selectedTournament.id, homeTeam.id, awayTeam.id, roundName)
                    }
                },
                modifier = Modifier.testTag("confirm_create_match_button")
            ) {
                Text("Programar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
