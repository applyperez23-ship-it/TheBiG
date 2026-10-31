package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.Team
import com.example.data.local.entity.TournamentModes
import com.example.domain.rules.PrizeCalculator
import com.example.domain.rules.PrizeCategory
import com.example.util.TeamCrestView

@Composable
fun CreateTournamentWizardDialog(
    availableTeams: List<Team>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        edition: String,
        type: String,
        durationMinutes: Int,
        entryFee: Double,
        playersPerTeam: Int,
        selectedTeamIds: List<Long>,
        championPrize: Double,
        runnerUpPrize: Double,
        topScorerPrize: Double,
        bestGkPrize: Double,
        homeAndAway: Boolean,
        financialModel: String
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: General, 2: Equipos, 3: Premios %, 4: Resumen

    // Step 1 states - Clean string inputs for fluid backspace erasing
    var name by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf("1") }
    var tournamentType by remember { mutableStateOf(TournamentModes.LEAGUE) }
    var homeAndAway by remember { mutableStateOf(false) }
    var durationMinutesText by remember { mutableStateOf("20") }
    var playersPerTeamText by remember { mutableStateOf("7") }

    // Financial model: "BY_TEAM" or "BY_PLAYER"
    var financialModel by remember { mutableStateOf("BY_TEAM") }
    var financesEnabled by remember { mutableStateOf(true) }
    var entryFeeText by remember { mutableStateOf("100") }

    // Step 2 states
    val selectedTeamIds = remember { mutableStateListOf<Long>() }

    // Step 3 states - Auto-balancing 100% hierarchical prize percentages
    var percentages by remember {
        mutableStateOf<Map<PrizeCategory, Float>>(
            mapOf(
                PrizeCategory.CHAMPION to 50f,
                PrizeCategory.RUNNER_UP to 25f,
                PrizeCategory.TOP_SCORER to 15f,
                PrizeCategory.BEST_GK to 10f
            )
        )
    }

    val parsedPlayersPerTeam = playersPerTeamText.toIntOrNull() ?: 7
    val parsedDurationMinutes = durationMinutesText.toIntOrNull() ?: 20
    val rawEntryFee = entryFeeText.toDoubleOrNull() ?: 0.0

    // Pool calculation with strict 10-unit rounding
    val rawTotalPool = if (financialModel == "BY_PLAYER") {
        rawEntryFee * parsedPlayersPerTeam * selectedTeamIds.size
    } else {
        rawEntryFee * selectedTeamIds.size
    }
    val totalPool = PrizeCalculator.roundToTen(rawTotalPool)

    val championPrize = if (financesEnabled) {
        PrizeCalculator.roundToTen(totalPool * ((percentages[PrizeCategory.CHAMPION] ?: 50f).toDouble() / 100.0))
    } else 0.0

    val runnerUpPrize = if (financesEnabled) {
        PrizeCalculator.roundToTen(totalPool * ((percentages[PrizeCategory.RUNNER_UP] ?: 25f).toDouble() / 100.0))
    } else 0.0

    val topScorerPrize = if (financesEnabled) {
        PrizeCalculator.roundToTen(totalPool * ((percentages[PrizeCategory.TOP_SCORER] ?: 15f).toDouble() / 100.0))
    } else 0.0

    val bestGkPrize = if (financesEnabled) {
        PrizeCalculator.roundToTen(totalPool * ((percentages[PrizeCategory.BEST_GK] ?: 10f).toDouble() / 100.0))
    } else 0.0

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (step) {
                                1 -> "Paso 1: Configuración General"
                                2 -> "Paso 2: Selección de Equipos"
                                3 -> "Paso 3: Finanzas y Premios (100%)"
                                else -> "Paso 4: Resumen y Fixture"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paso $step de 4",
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
                    progress = { step / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Body content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (step) {
                        1 -> {
                            // Step 1: Configuración General & Formato
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Nombre del Torneo") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wizard_tournament_name_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = edition,
                                        onValueChange = { edition = it },
                                        label = { Text("Edición (ej: 1, 2026)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = playersPerTeamText,
                                        onValueChange = {
                                            // Allow backspace to empty without reverting immediately
                                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                                playersPerTeamText = it
                                            }
                                        },
                                        label = { Text("Jugadores/Equipo") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "Formato de Competencia:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                val types = listOf(
                                    Triple(TournamentModes.LEAGUE, "Liga (Todos vs Todos)", "Calendario matemático completo de jornadas."),
                                    Triple(TournamentModes.CUP, "Copa (Eliminatoria Directa)", "Partidos de eliminación directa hasta la Gran Final."),
                                    Triple(TournamentModes.GROUPS_CUP, "Fase de Grupos + Playoffs", "Grupos clasificatorios (Top 2 avanzan) seguidos de eliminatoria."),
                                    Triple(TournamentModes.MIXED, "Mixto (Liga + Eliminatoria)", "Fase regular de tabla general seguida de eliminatoria.")
                                )

                                types.forEach { (tKey, tTitle, tDesc) ->
                                    val isSel = tournamentType == tKey
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { tournamentType = tKey },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSel) {
                                                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(tTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(tDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                if (tournamentType == TournamentModes.LEAGUE) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Partidos de Ida y Vuelta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Genera doble enfrentamiento alternando localía", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = homeAndAway, onCheckedChange = { homeAndAway = it })
                                    }
                                }

                                // Match duration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = durationMinutesText,
                                        onValueChange = {
                                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                                durationMinutesText = it
                                            }
                                        },
                                        label = { Text("Duración Partido (Min)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${parsedDurationMinutes / 2}m 1T + ${parsedDurationMinutes / 2}m 2T",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Step 2: Selección de Equipos
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Clubes Participantes (${selectedTeamIds.size} seleccionados):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    if (availableTeams.isNotEmpty()) {
                                        Text(
                                            text = if (selectedTeamIds.size == availableTeams.size) "Deseleccionar" else "Todos",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.clickable {
                                                if (selectedTeamIds.size == availableTeams.size) {
                                                    selectedTeamIds.clear()
                                                } else {
                                                    selectedTeamIds.clear()
                                                    selectedTeamIds.addAll(availableTeams.map { it.id })
                                                }
                                            }
                                        )
                                    }
                                }

                                if (availableTeams.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No hay clubes creados todavía. Ve a la pestaña 'Clubes' para crearlos primero.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(availableTeams) { team ->
                                            val isChecked = selectedTeamIds.contains(team.id)
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (isChecked) selectedTeamIds.remove(team.id)
                                                        else selectedTeamIds.add(team.id)
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { check ->
                                                            if (check) selectedTeamIds.add(team.id)
                                                            else selectedTeamIds.remove(team.id)
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    TeamCrestView(team = team, size = 36.dp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(team.shortName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Step 3: Finanzas y Premios por Porcentajes Auto-balanceados a 100%
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Control Financiero de Torneo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Switch(checked = financesEnabled, onCheckedChange = { financesEnabled = it })
                                }

                                if (financesEnabled) {
                                    // Dual financial mode toggle
                                    Text("Modalidad de Cobro:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = financialModel == "BY_TEAM",
                                            onClick = { financialModel = "BY_TEAM" },
                                            label = { Text("Por Club (Fijo)", fontSize = 12.sp) }
                                        )
                                        FilterChip(
                                            selected = financialModel == "BY_PLAYER",
                                            onClick = { financialModel = "BY_PLAYER" },
                                            label = { Text("Por Jugador ($rawEntryFee × $parsedPlayersPerTeam)", fontSize = 12.sp) }
                                        )
                                    }

                                    OutlinedTextField(
                                        value = entryFeeText,
                                        onValueChange = {
                                            if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                                                entryFeeText = it
                                            }
                                        },
                                        label = {
                                            Text(
                                                if (financialModel == "BY_PLAYER") "Cuota por Jugador Individual ($)"
                                                else "Cuota Fija por Club ($)"
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Total Pool Summary
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("POZO TOTAL (REDONDEO A 10)", color = Color(0xFFF59E0B), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                                val detail = if (financialModel == "BY_PLAYER") {
                                                    "${selectedTeamIds.size} Clubes × $parsedPlayersPerTeam Jugadores × $$rawEntryFee"
                                                } else {
                                                    "${selectedTeamIds.size} Clubes × $$rawEntryFee"
                                                }
                                                Text(detail, fontSize = 11.sp)
                                            }
                                            Text("$${"%.0f".format(totalPool)}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFF59E0B))
                                        }
                                    }

                                    Text("Reparto Jerárquico (Campeón ≥ Subcampeón ≥ Resto):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                    // Champion %
                                    val chPct = percentages[PrizeCategory.CHAMPION] ?: 50f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Campeón: ${chPct.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$${"%.0f".format(championPrize)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = chPct,
                                            onValueChange = {
                                                percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.CHAMPION, it, percentages)
                                            },
                                            valueRange = 0f..100f
                                        )
                                    }

                                    // Runner Up %
                                    val ruPct = percentages[PrizeCategory.RUNNER_UP] ?: 25f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Subcampeón: ${ruPct.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$${"%.0f".format(runnerUpPrize)}", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = ruPct,
                                            onValueChange = {
                                                percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.RUNNER_UP, it, percentages)
                                            },
                                            valueRange = 0f..100f
                                        )
                                    }

                                    // Top Scorer %
                                    val scPct = percentages[PrizeCategory.TOP_SCORER] ?: 15f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Máximo Goleador: ${scPct.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$${"%.0f".format(topScorerPrize)}", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = scPct,
                                            onValueChange = {
                                                percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.TOP_SCORER, it, percentages)
                                            },
                                            valueRange = 0f..100f
                                        )
                                    }

                                    // Best GK %
                                    val gkPct = percentages[PrizeCategory.BEST_GK] ?: 10f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Valla Menos Vencida: ${gkPct.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$${"%.0f".format(bestGkPrize)}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Slider(
                                            value = gkPct,
                                            onValueChange = {
                                                percentages = PrizeCalculator.rebalanceHierarchicalPercentages(PrizeCategory.BEST_GK, it, percentages)
                                            },
                                            valueRange = 0f..100f
                                        )
                                    }
                                } else {
                                    Text(
                                        "Finanzas desactivadas para este torneo.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        4 -> {
                            // Step 4: Resumen y Confirmación
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("Edición: $edition • Formato: $tournamentType", fontSize = 12.sp)
                                        if (tournamentType == TournamentModes.LEAGUE) {
                                            Text("Modalidad: ${if (homeAndAway) "Ida y Vuelta (Doble Partido)" else "Solo Ida"}", fontSize = 12.sp)
                                        }
                                        Text("Duración por partido: $parsedDurationMinutes minutos", fontSize = 12.sp)
                                        Text("Plantilla por equipo: $parsedPlayersPerTeam jugadores", fontSize = 12.sp)
                                        Text("Clubes participantes: ${selectedTeamIds.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (financesEnabled) {
                                            val modeLabel = if (financialModel == "BY_PLAYER") "Por Jugador ($rawEntryFee/jugador)" else "Por Club Fijo ($rawEntryFee/club)"
                                            Text("Modelo Financiero: $modeLabel • Pozo: $$totalPool", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (financesEnabled) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Premios Asignados (Redondeados a 10):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("• Campeón: $${"%.0f".format(championPrize)} (${(percentages[PrizeCategory.CHAMPION] ?: 50f).toInt()}%)", fontSize = 12.sp)
                                            Text("• Subcampeón: $${"%.0f".format(runnerUpPrize)} (${(percentages[PrizeCategory.RUNNER_UP] ?: 25f).toInt()}%)", fontSize = 12.sp)
                                            Text("• Goleador: $${"%.0f".format(topScorerPrize)} (${(percentages[PrizeCategory.TOP_SCORER] ?: 15f).toInt()}%)", fontSize = 12.sp)
                                            Text("• Valla Menos Vencida: $${"%.0f".format(bestGkPrize)} (${(percentages[PrizeCategory.BEST_GK] ?: 10f).toInt()}%)", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "Al presionar 'Generar Calendario', se creará el torneo y se calcularán automáticamente todas las jornadas matemáticas exactas.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

                    if (step < 4) {
                        Button(
                            onClick = { step++ },
                            enabled = if (step == 1) name.isNotBlank() else if (step == 2) selectedTeamIds.size >= 2 else true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_tournament_next_button"),
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
                                    edition.trim(),
                                    tournamentType,
                                    parsedDurationMinutes,
                                    rawEntryFee,
                                    parsedPlayersPerTeam,
                                    selectedTeamIds.toList(),
                                    championPrize,
                                    runnerUpPrize,
                                    topScorerPrize,
                                    bestGkPrize,
                                    homeAndAway,
                                    financialModel
                                )
                            },
                            enabled = name.isNotBlank() && selectedTeamIds.size >= 2,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_tournament_finish_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generar Calendario")
                        }
                    }
                }
            }
        }
    }
}
