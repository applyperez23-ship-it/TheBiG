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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.entity.Tournament
import com.example.ui.components.DeleteConfirmationDialog
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
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay torneos creados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón + para registrar un torneo, configurar premios y generar partidos.",
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

    if (showCreateDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, edition, fee, playersCount, durationMin, ch, ru, sc, asst, gk, minGk ->
                scope.launch {
                    val id = viewModel.createTournament(
                        name = name,
                        edition = edition,
                        entryFee = fee,
                        playersPerTeam = playersCount,
                        matchDurationMinutes = durationMin,
                        prizeChampion = ch,
                        prizeRunnerUp = ru,
                        prizeScorer = sc,
                        prizeAssister = asst,
                        prizeBestGk = gk,
                        minMatchesGk = minGk
                    )
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
                    text = "Edición ${tournament.edition} • Estado: ${formatStatus(tournament.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tournament.entryFee > 0) {
                        Text(
                            text = "Equipo: $${tournament.entryFee}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Jugador: $${"%.2f".format(tournament.feePerPlayer)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${tournament.playersPerTeam} vs ${tournament.playersPerTeam}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar torneo",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun formatStatus(status: String): String {
    return when (status) {
        "PLANNING" -> "Planificación"
        "IN_PROGRESS" -> "En Juego"
        "FINISHED" -> "Finalizado"
        else -> status
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
        championPrize: Double,
        runnerUpPrize: Double,
        scorerPrize: Double,
        assisterPrize: Double,
        bestGkPrize: Double,
        minGkMatches: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf("1") }
    var playersPerTeamStr by remember { mutableStateOf("7") }
    var entryFeeTeamStr by remember { mutableStateOf("70") }
    var feePerPlayerStr by remember { mutableStateOf("10") }
    var matchDurationStr by remember { mutableStateOf("20") }

    // Collapsible accordion state for Prizes & Rules (starts collapsed)
    var isRulesExpanded by remember { mutableStateOf(false) }

    var prizeChampion by remember { mutableStateOf("0") }
    var prizeRunnerUp by remember { mutableStateOf("0") }
    var prizeScorer by remember { mutableStateOf("0") }
    var prizeAssister by remember { mutableStateOf("0") }
    var prizeBestGk by remember { mutableStateOf("0") }
    var minGkMatches by remember { mutableStateOf("2") }

    // Smart financial link helper
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
                // Section 1: Basic Information
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Torneo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tournament_name_input")
                )

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

                // Section 2: Smart Financials (Two-way auto calculation)
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

                // Section 3: Collapsible Accordion for Prizes and Rules
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
                                text = "Premios y Reglas",
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

                // Expanded Compact 2-Column Grid for Prizes
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
                                label = { Text("Mín. Partidos Portero") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                        onConfirm(
                            name,
                            edition,
                            finalFee,
                            finalCount,
                            finalDuration,
                            prizeChampion.toDoubleOrNull() ?: 0.0,
                            prizeRunnerUp.toDoubleOrNull() ?: 0.0,
                            prizeScorer.toDoubleOrNull() ?: 0.0,
                            prizeAssister.toDoubleOrNull() ?: 0.0,
                            prizeBestGk.toDoubleOrNull() ?: 0.0,
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
