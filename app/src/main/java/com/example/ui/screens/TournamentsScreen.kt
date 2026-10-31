package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AwardTimingModes
import com.example.data.local.entity.Tournament
import com.example.data.local.entity.TournamentModes
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.TheBiGLogo
import com.example.ui.viewmodel.TournamentViewModel
import kotlinx.coroutines.launch

@Composable
fun TournamentsScreen(
    viewModel: TournamentViewModel,
    onTournamentClick: (Long) -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var tournamentToDelete by remember { mutableStateOf<Tournament?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_tournament_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Torneo")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (tournaments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TheBiGLogo(size = 64.dp, showText = false)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay torneos registrados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instalación limpia sin datos de prueba. Toca el botón '+' para registrar tu primer torneo, configurar premios y reglas.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TheBiGLogo(size = 32.dp, showText = true)
                            Text(
                                text = "${tournaments.size} Torneo(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(tournaments, key = { it.id }) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onClick = { onTournamentClick(tournament.id) },
                            onDelete = { tournamentToDelete = tournament }
                        )
                    }
                }
            }
        }
    }

    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()

    if (showCreateDialog) {
        com.example.ui.components.CreateTournamentWizardDialog(
            availableTeams = allTeams,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, edition, type, durationMin, fee, playersCount, selectedTeamIds, ch, ru, sc, gk, homeAndAway, financialModel ->
                scope.launch {
                    val id = viewModel.createTournament(
                        name = name,
                        edition = edition,
                        entryFee = fee,
                        playersPerTeam = playersCount,
                        matchDurationMinutes = durationMin,
                        tournamentType = type,
                        awardTiming = AwardTimingModes.END_OF_LEAGUE,
                        prizeChampion = ch,
                        prizeRunnerUp = ru,
                        prizeScorer = sc,
                        prizeAssister = 0.0,
                        prizeBestGk = gk,
                        prizeMvp = 0.0,
                        yellowCardFine = 5.0,
                        redCardFine = 10.0,
                        minMatchesGk = 2,
                        financialModel = financialModel
                    )
                    // Enroll all selected teams
                    for (tId in selectedTeamIds) {
                        viewModel.enrollTeam(id, tId, feePaid = false)
                    }
                    // Generate schedule using exact mathematical TournamentScheduler
                    if (selectedTeamIds.size >= 2) {
                        val enrolled = allTeams.filter { selectedTeamIds.contains(it.id) }
                        val generatedMatches = com.example.domain.rules.TournamentScheduler.generateSchedule(
                            tournamentId = id,
                            tournamentType = type,
                            teams = enrolled,
                            homeAndAway = homeAndAway
                        )
                        for (m in generatedMatches) {
                            com.example.TheBiGApp.instance.repository.saveMatch(m)
                        }
                    }
                    showCreateDialog = false
                    onTournamentClick(id)
                }
            }
        )
    }

    if (tournamentToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "el torneo '${tournamentToDelete!!.name}'",
            onDismiss = { tournamentToDelete = null },
            onConfirm = {
                viewModel.deleteTournament(tournamentToDelete!!.id)
                tournamentToDelete = null
            }
        )
    }
}

@Composable
private fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SportsScore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Edición ${tournament.edition} • Modo: ${formatModeName(tournament.tournamentType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tournament.entryFee > 0) {
                        Text(
                            text = "$${tournament.entryFee.toInt()}/equipo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "($${tournament.feePerPlayer.toInt()}/jugador)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_tournament_button_${tournament.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar torneo",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatModeName(mode: String): String {
    return when (mode) {
        TournamentModes.LEAGUE -> "Liga Tradicional"
        TournamentModes.CUP -> "Copa (Eliminatoria)"
        TournamentModes.GROUPS_CUP -> "Grupos + Copa"
        TournamentModes.MIXED -> "Modo Mixto"
        else -> mode
    }
}

@Composable
private fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        edition: String,
        fee: Double,
        playersPerTeam: Int,
        matchDurationMinutes: Int,
        tournamentType: String,
        awardTiming: String,
        championPrize: Double,
        runnerUpPrize: Double,
        scorerPrize: Double,
        assisterPrize: Double,
        bestGkPrize: Double,
        mvpPrize: Double,
        yellowCardFine: Double,
        redCardFine: Double,
        minGkMatches: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf("1") }
    var tournamentType by remember { mutableStateOf(TournamentModes.LEAGUE) }
    var awardTiming by remember { mutableStateOf(AwardTimingModes.END_OF_LEAGUE) }
    var playersPerTeamStr by remember { mutableStateOf("7") }
    var entryFeeTeamStr by remember { mutableStateOf("70") }
    var feePerPlayerStr by remember { mutableStateOf("10") }
    var matchDurationStr by remember { mutableStateOf("20") }

    var isRulesExpanded by remember { mutableStateOf(false) }

    var prizeChampion by remember { mutableStateOf("0") }
    var prizeRunnerUp by remember { mutableStateOf("0") }
    var prizeScorer by remember { mutableStateOf("0") }
    var prizeAssister by remember { mutableStateOf("0") }
    var prizeBestGk by remember { mutableStateOf("0") }
    var prizeMvp by remember { mutableStateOf("0") }
    var yellowCardFineStr by remember { mutableStateOf("5") }
    var redCardFineStr by remember { mutableStateOf("10") }
    var minGkMatches by remember { mutableStateOf("2") }

    fun onTeamFeeChanged(newTeamFeeStr: String) {
        entryFeeTeamStr = newTeamFeeStr
        val teamVal = newTeamFeeStr.toDoubleOrNull()
        val count = playersPerTeamStr.toIntOrNull() ?: 7
        if (teamVal != null && count > 0) {
            val perPlayer = teamVal / count
            feePerPlayerStr = if (perPlayer % 1.0 == 0.0) perPlayer.toInt().toString() else "%.2f".format(perPlayer)
        }
    }

    fun onPlayerFeeChanged(newPlayerFeeStr: String) {
        feePerPlayerStr = newPlayerFeeStr
        val playerVal = newPlayerFeeStr.toDoubleOrNull()
        val count = playersPerTeamStr.toIntOrNull() ?: 7
        if (playerVal != null && count > 0) {
            val totalTeam = playerVal * count
            entryFeeTeamStr = if (totalTeam % 1.0 == 0.0) totalTeam.toInt().toString() else "%.2f".format(totalTeam)
        }
    }

    fun onPlayersCountChanged(newCountStr: String) {
        playersPerTeamStr = newCountStr
        val count = newCountStr.toIntOrNull()
        val teamVal = entryFeeTeamStr.toDoubleOrNull()
        if (count != null && count > 0 && teamVal != null) {
            val perPlayer = teamVal / count
            feePerPlayerStr = if (perPlayer % 1.0 == 0.0) perPlayer.toInt().toString() else "%.2f".format(perPlayer)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Crear Nuevo Torneo",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Torneo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tournament_name_input")
                )

                Text(
                    text = "Modo de Torneo:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tournamentType == TournamentModes.LEAGUE,
                            onClick = { tournamentType = TournamentModes.LEAGUE },
                            label = { Text("Liga Tradicional", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = tournamentType == TournamentModes.CUP,
                            onClick = { tournamentType = TournamentModes.CUP },
                            label = { Text("Copa (K.O.)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tournamentType == TournamentModes.GROUPS_CUP,
                            onClick = { tournamentType = TournamentModes.GROUPS_CUP },
                            label = { Text("Grupos + Copa", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = tournamentType == TournamentModes.MIXED,
                            onClick = { tournamentType = TournamentModes.MIXED },
                            label = { Text("Modo Mixto", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Momento de Premiación Individual:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = awardTiming == AwardTimingModes.END_OF_LEAGUE,
                        onClick = { awardTiming = AwardTimingModes.END_OF_LEAGUE },
                        label = { Text("Al terminar Liga", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = awardTiming == AwardTimingModes.END_OF_CUP,
                        onClick = { awardTiming = AwardTimingModes.END_OF_CUP },
                        label = { Text("Al finalizar Copa", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = edition,
                        onValueChange = { edition = it },
                        label = { Text("Edición") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = playersPerTeamStr,
                        onValueChange = { onPlayersCountChanged(it) },
                        label = { Text("Jugadores/Equipo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = entryFeeTeamStr,
                        onValueChange = { onTeamFeeChanged(it) },
                        label = { Text("Cuota por Equipo ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = feePerPlayerStr,
                        onValueChange = { onPlayerFeeChanged(it) },
                        label = { Text("Cuota/Jugador ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = matchDurationStr,
                    onValueChange = { matchDurationStr = it },
                    label = { Text("Duración por Partido (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRulesExpanded = !isRulesExpanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Premios y Pozo de Sanciones",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (isRulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isRulesExpanded) "Ocultar" else "Mostrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isRulesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = prizeChampion,
                                onValueChange = { prizeChampion = it },
                                label = { Text("Campeón ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = prizeRunnerUp,
                                onValueChange = { prizeRunnerUp = it },
                                label = { Text("Subcampeón ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = prizeScorer,
                                onValueChange = { prizeScorer = it },
                                label = { Text("Goleador ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = prizeAssister,
                                onValueChange = { prizeAssister = it },
                                label = { Text("Asistidor ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = prizeBestGk,
                                onValueChange = { prizeBestGk = it },
                                label = { Text("Mejor Portero ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = minGkMatches,
                                onValueChange = { minGkMatches = it },
                                label = { Text("Mín. PJ Portero") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Pozo de Sanciones (Financia Premio MVP):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = yellowCardFineStr,
                                onValueChange = { yellowCardFineStr = it },
                                label = { Text("Multa T. Amarilla ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = redCardFineStr,
                                onValueChange = { redCardFineStr = it },
                                label = { Text("Multa T. Roja ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = prizeMvp,
                            onValueChange = { prizeMvp = it },
                            label = { Text("Base Extra Premio MVP ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalFee = entryFeeTeamStr.toDoubleOrNull() ?: 0.0
                        val finalCount = playersPerTeamStr.toIntOrNull() ?: 7
                        val finalDuration = matchDurationStr.toIntOrNull() ?: 20
                        val yellowF = yellowCardFineStr.toDoubleOrNull() ?: 5.0
                        val redF = redCardFineStr.toDoubleOrNull() ?: 10.0
                        val mvp = prizeMvp.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            name,
                            edition,
                            finalFee,
                            finalCount,
                            finalDuration,
                            tournamentType,
                            awardTiming,
                            prizeChampion.toDoubleOrNull() ?: 0.0,
                            prizeRunnerUp.toDoubleOrNull() ?: 0.0,
                            prizeScorer.toDoubleOrNull() ?: 0.0,
                            prizeAssister.toDoubleOrNull() ?: 0.0,
                            prizeBestGk.toDoubleOrNull() ?: 0.0,
                            mvp,
                            yellowF,
                            redF,
                            minGkMatches.toIntOrNull() ?: 2
                        )
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_create_tournament_button")
            ) {
                Text("Crear")
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
