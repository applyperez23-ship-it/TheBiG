package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.util.GraphicCardGenerator
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class PitchPosition(
    val xRatio: Float, // 0.0 (left) to 1.0 (right)
    val yRatio: Float, // 0.0 (top goal) to 1.0 (bottom goal)
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalPitchScreen(
    team: Team,
    players: List<Player>,
    defaultFormatPlayersCount: Int = 7,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var formationPlayerCount by remember { mutableIntStateOf(defaultFormatPlayersCount.coerceIn(5, 11)) }
    val starters = remember { mutableStateListOf<Player>() }
    val bench = remember { mutableStateListOf<Player>() }

    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var isSimulatingPlay by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // Ball Animation states
    val ballX = remember { Animatable(0.5f) }
    val ballY = remember { Animatable(0.85f) }
    val gkX = remember { Animatable(0.5f) }

    // Initialize starters & bench when players list or formation count changes
    LaunchedEffect(players, formationPlayerCount) {
        starters.clear()
        bench.clear()

        val gk = players.firstOrNull { it.isGoalkeeper } ?: players.firstOrNull()
        val nonGk = players.filter { it.id != gk?.id }

        val activeList = mutableListOf<Player>()
        if (gk != null) activeList.add(gk)
        activeList.addAll(nonGk.take(formationPlayerCount - if (gk != null) 1 else 0))

        val benchList = players.filter { p -> !activeList.any { it.id == p.id } }

        starters.addAll(activeList)
        bench.addAll(benchList)
    }

    // Positions mapping based on player count
    val tacticalPositions = remember(formationPlayerCount) {
        calculateTacticalPositions(formationPlayerCount)
    }

    fun handlePlayerTap(playerId: Long) {
        if (selectedPlayerId == null) {
            selectedPlayerId = playerId
        } else if (selectedPlayerId == playerId) {
            selectedPlayerId = null
        } else {
            // Swap logic
            val firstId = selectedPlayerId!!
            val secondId = playerId

            val firstInStarters = starters.indexOfFirst { it.id == firstId }
            val firstInBench = bench.indexOfFirst { it.id == firstId }
            val secondInStarters = starters.indexOfFirst { it.id == secondId }
            val secondInBench = bench.indexOfFirst { it.id == secondId }

            if (firstInStarters != -1 && secondInStarters != -1) {
                val temp = starters[firstInStarters]
                starters[firstInStarters] = starters[secondInStarters]
                starters[secondInStarters] = temp
            } else if (firstInStarters != -1 && secondInBench != -1) {
                val pStarter = starters[firstInStarters]
                val pBench = bench[secondInBench]
                starters[firstInStarters] = pBench
                bench[secondInBench] = pStarter
            } else if (firstInBench != -1 && secondInStarters != -1) {
                val pBench = bench[firstInBench]
                val pStarter = starters[secondInStarters]
                bench[firstInBench] = pStarter
                starters[secondInStarters] = pBench
            }

            selectedPlayerId = null
        }
    }

    fun run2DPlaySimulation() {
        if (isSimulatingPlay) return
        scope.launch {
            isSimulatingPlay = true
            saveMessage = null

            // Step 1: Start at Defender
            ballX.snapTo(0.3f)
            ballY.snapTo(0.72f)
            gkX.snapTo(0.5f)

            // Step 2: Pass to Midfielder
            ballX.animateTo(0.7f, tween(600, easing = LinearEasing))
            ballY.animateTo(0.52f, tween(600, easing = LinearEasing))

            // Step 3: Pass to Forward
            ballX.animateTo(0.5f, tween(500, easing = LinearEasing))
            ballY.animateTo(0.32f, tween(500, easing = LinearEasing))

            // Step 4: Powerful Shot toward top goal right corner
            launch {
                gkX.animateTo(0.68f, tween(450, easing = FastOutSlowInEasing))
            }
            ballX.animateTo(0.66f, tween(450, easing = FastOutSlowInEasing))
            ballY.animateTo(0.12f, tween(450, easing = FastOutSlowInEasing))

            // Step 5: Keeper Dives and Saves!
            saveMessage = "¡ATAJADÓN DEL ARQUERO! 🧤⚽"

            // Rebound
            ballY.animateTo(0.22f, tween(400, easing = FastOutSlowInEasing))

            kotlinx.coroutines.delay(1800)
            isSimulatingPlay = false
            saveMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pizarra Táctica Oficial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${team.name} • Formación F${formationPlayerCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val bmp = generateTacticalPitchBitmap(context, team, starters.toList(), formationPlayerCount)
                            GraphicCardGenerator.shareBitmap(
                                context = context,
                                bitmap = bmp,
                                title = "Alineación Táctica - ${team.name} (F$formationPlayerCount)"
                            )
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir Alineación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Formation Selector Chips
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Modalidad de Fútbol y Jugadores en Cancha:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 7, 8, 9, 11).forEach { count ->
                            FilterChip(
                                selected = formationPlayerCount == count,
                                onClick = { formationPlayerCount = count },
                                label = { Text("F$count", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Top action bar: Simulation & Swap guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedPlayerId != null) "Toca otro jugador para cambiarlo" else "Toca para permutar titulares/suplentes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedPlayerId != null) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { run2DPlaySimulation() },
                    enabled = !isSimulatingPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simular Jugada", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // THE GREEN PITCH (Cenital View)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF15803D))
                    .border(3.dp, Color(0xFFF59E0B), RoundedCornerShape(18.dp))
            ) {
                // Soccer Pitch Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Alternating lawn stripe grass bands
                    val bands = 8
                    val bandH = h / bands
                    for (i in 0 until bands) {
                        if (i % 2 == 0) {
                            drawRect(
                                color = Color(0xFF166534).copy(alpha = 0.35f),
                                topLeft = Offset(0f, i * bandH),
                                size = Size(w, bandH)
                            )
                        }
                    }

                    val linePaint = Color.White.copy(alpha = 0.85f)
                    val stroke = Stroke(width = 3.dp.toPx())

                    // Outer pitch border margin
                    val m = 18.dp.toPx()
                    drawRect(
                        color = linePaint,
                        topLeft = Offset(m, m),
                        size = Size(w - 2 * m, h - 2 * m),
                        style = stroke
                    )

                    // Halfway Line
                    val midY = h / 2f
                    drawLine(
                        color = linePaint,
                        start = Offset(m, midY),
                        end = Offset(w - m, midY),
                        strokeWidth = stroke.width
                    )

                    // Center Circle
                    val circleR = 48.dp.toPx()
                    drawCircle(
                        color = linePaint,
                        radius = circleR,
                        center = Offset(w / 2f, midY),
                        style = stroke
                    )
                    drawCircle(
                        color = linePaint,
                        radius = 4.dp.toPx(),
                        center = Offset(w / 2f, midY)
                    )

                    // Top Penalty Box (Keeper area)
                    val boxW = w * 0.52f
                    val boxH = h * 0.16f
                    val boxLeft = (w - boxW) / 2f
                    drawRect(
                        color = linePaint,
                        topLeft = Offset(boxLeft, m),
                        size = Size(boxW, boxH),
                        style = stroke
                    )

                    // Top Goal
                    val goalW = w * 0.28f
                    val goalH = 12.dp.toPx()
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset((w - goalW) / 2f, m - goalH),
                        size = Size(goalW, goalH),
                        style = stroke
                    )

                    // Bottom Penalty Box
                    drawRect(
                        color = linePaint,
                        topLeft = Offset(boxLeft, h - m - boxH),
                        size = Size(boxW, boxH),
                        style = stroke
                    )

                    // Bottom Goal
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset((w - goalW) / 2f, h - m),
                        size = Size(goalW, goalH),
                        style = stroke
                    )
                }

                // Render Starters on the pitch
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val pitchWidth = maxWidth
                    val pitchHeight = maxHeight

                    starters.forEachIndexed { index, player ->
                        val pos = tacticalPositions.getOrElse(index) {
                            PitchPosition(0.5f, 0.5f, "JUG")
                        }

                        val isSelected = player.id == selectedPlayerId

                        val xOffset = pitchWidth * pos.xRatio - 26.dp
                        val yOffset = pitchHeight * pos.yRatio - 26.dp

                        Box(
                            modifier = Modifier
                                .offset(x = xOffset, y = yOffset)
                                .size(52.dp)
                                .clickable { handlePlayerTap(player.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (isSelected) Color(0xFFF59E0B) else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) Color.White else Color(0xFFF59E0B),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TeamJerseyView(
                                        team = team,
                                        isGoalkeeper = player.isGoalkeeper || index == 0,
                                        dorsal = player.dorsal,
                                        size = 32.dp
                                    )
                                }

                                Text(
                                    text = player.name.split(" ").firstOrNull() ?: player.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // 2D Ball Animation
                    if (isSimulatingPlay) {
                        val currentBallX = pitchWidth * ballX.value - 12.dp
                        val currentBallY = pitchHeight * ballY.value - 12.dp

                        Box(
                            modifier = Modifier
                                .offset(x = currentBallX, y = currentBallY)
                                .size(24.dp)
                                .background(Color.White, CircleShape)
                                .border(1.5.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Save Banner Toast
                    saveMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = msg,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SUPLENTES (BANCA DE RESERVAS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Banca de Suplentes (${bench.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Toca para ingresar al campo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (bench.isEmpty()) {
                        Text(
                            text = "No hay suplentes disponibles en la plantilla.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bench, key = { it.id }) { player ->
                                val isSelected = player.id == selectedPlayerId
                                Card(
                                    modifier = Modifier
                                        .clickable { handlePlayerTap(player.id) }
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFF59E0B) else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TeamJerseyView(
                                            team = team,
                                            isGoalkeeper = player.isGoalkeeper,
                                            dorsal = player.dorsal,
                                            size = 36.dp
                                        )
                                        Text(
                                            text = player.name.split(" ").firstOrNull() ?: player.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "#${player.dorsal} • ${if (player.isGoalkeeper) "POR" else "SUP"}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share & WhatsApp Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val bmp = generateTacticalPitchBitmap(context, team, starters.toList(), formationPlayerCount)
                        GraphicCardGenerator.shareBitmap(
                            context = context,
                            bitmap = bmp,
                            title = "Pizarra Táctica TheBiG - ${team.name} (F$formationPlayerCount)"
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir HD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val bmp = generateTacticalPitchBitmap(context, team, starters.toList(), formationPlayerCount)
                        GraphicCardGenerator.shareBitmap(
                            context = context,
                            bitmap = bmp,
                            title = "Alineación Oficial ${team.name}\nFormación F$formationPlayerCount vía TheBiG"
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar al WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Calculates balanced soccer pitch coordinates (xRatio 0..1, yRatio 0..1) based on number of players on field.
 */
fun calculateTacticalPositions(playerCount: Int): List<PitchPosition> {
    return when (playerCount) {
        5 -> listOf(
            PitchPosition(0.5f, 0.88f, "POR"), // GK
            PitchPosition(0.28f, 0.68f, "DEF"),
            PitchPosition(0.72f, 0.68f, "DEF"),
            PitchPosition(0.5f, 0.44f, "MED"),
            PitchPosition(0.5f, 0.22f, "DEL")
        )
        7 -> listOf(
            PitchPosition(0.5f, 0.88f, "POR"), // GK
            PitchPosition(0.26f, 0.70f, "LI"),
            PitchPosition(0.74f, 0.70f, "LD"),
            PitchPosition(0.32f, 0.48f, "MC"),
            PitchPosition(0.68f, 0.48f, "MC"),
            PitchPosition(0.32f, 0.24f, "DEL"),
            PitchPosition(0.68f, 0.24f, "DEL")
        )
        8 -> listOf(
            PitchPosition(0.5f, 0.88f, "POR"),
            PitchPosition(0.22f, 0.70f, "LI"),
            PitchPosition(0.5f, 0.72f, "DFC"),
            PitchPosition(0.78f, 0.70f, "LD"),
            PitchPosition(0.34f, 0.48f, "MC"),
            PitchPosition(0.66f, 0.48f, "MC"),
            PitchPosition(0.35f, 0.24f, "DEL"),
            PitchPosition(0.65f, 0.24f, "DEL")
        )
        9 -> listOf(
            PitchPosition(0.5f, 0.88f, "POR"),
            PitchPosition(0.22f, 0.72f, "LI"),
            PitchPosition(0.5f, 0.74f, "DFC"),
            PitchPosition(0.78f, 0.72f, "LD"),
            PitchPosition(0.24f, 0.50f, "MI"),
            PitchPosition(0.5f, 0.52f, "MC"),
            PitchPosition(0.76f, 0.50f, "MD"),
            PitchPosition(0.36f, 0.24f, "DEL"),
            PitchPosition(0.64f, 0.24f, "DEL")
        )
        11 -> listOf(
            PitchPosition(0.5f, 0.89f, "POR"),
            PitchPosition(0.18f, 0.74f, "LI"),
            PitchPosition(0.39f, 0.75f, "DFC"),
            PitchPosition(0.61f, 0.75f, "DFC"),
            PitchPosition(0.82f, 0.74f, "LD"),
            PitchPosition(0.22f, 0.52f, "MI"),
            PitchPosition(0.5f, 0.54f, "MC"),
            PitchPosition(0.78f, 0.52f, "MD"),
            PitchPosition(0.22f, 0.26f, "EI"),
            PitchPosition(0.5f, 0.23f, "DC"),
            PitchPosition(0.78f, 0.26f, "ED")
        )
        else -> {
            // Generic balanced distribution
            val list = mutableListOf<PitchPosition>()
            list.add(PitchPosition(0.5f, 0.88f, "POR"))
            val fieldPlayers = playerCount - 1
            for (i in 0 until fieldPlayers) {
                val row = (i / 3) + 1
                val col = i % 3
                val y = 0.75f - (row * 0.22f)
                val x = 0.25f + (col * 0.25f)
                list.add(PitchPosition(x.coerceIn(0.15f, 0.85f), y.coerceIn(0.15f, 0.85f), "JUG"))
            }
            list
        }
    }
}

/**
 * Generates an HD 1080x1080 tactical pitch card with team branding, field lines, and starters.
 */
fun generateTacticalPitchBitmap(
    context: Context,
    team: Team,
    starters: List<Player>,
    playerCount: Int
): Bitmap {
    val size = 1080
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Dark Navy Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#0F172A")
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Header branding
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = AndroidColor.parseColor("#F59E0B")
    paint.textSize = 28f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("TheBiG • PIZARRA TÁCTICA", 60f, 70f, paint)

    paint.color = AndroidColor.WHITE
    paint.textSize = 38f
    canvas.drawText("${team.name.uppercase()} (F$playerCount)", 60f, 120f, paint)

    // Soccer pitch rect
    val pitchRect = RectF(60f, 160f, 1020f, 960f)
    val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#15803D")
    }
    canvas.drawRoundRect(pitchRect, 24f, 24f, grassPaint)

    // Grass stripes
    val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#166534")
    }
    val bandH = pitchRect.height() / 8f
    for (i in 0 until 8) {
        if (i % 2 == 0) {
            canvas.drawRect(pitchRect.left, pitchRect.top + (i * bandH), pitchRect.right, pitchRect.top + ((i + 1) * bandH), stripePaint)
        }
    }

    // Pitch markings
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRoundRect(pitchRect, 24f, 24f, linePaint)

    val midY = pitchRect.centerY()
    canvas.drawLine(pitchRect.left, midY, pitchRect.right, midY, linePaint)
    canvas.drawCircle(pitchRect.centerX(), midY, 110f, linePaint)

    // Goal boxes
    val boxW = pitchRect.width() * 0.46f
    val boxH = pitchRect.height() * 0.16f
    val boxLeft = pitchRect.centerX() - (boxW / 2f)
    canvas.drawRect(boxLeft, pitchRect.top, boxLeft + boxW, pitchRect.top + boxH, linePaint)
    canvas.drawRect(boxLeft, pitchRect.bottom - boxH, boxLeft + boxW, pitchRect.bottom, linePaint)

    // Draw starters
    val positions = calculateTacticalPositions(playerCount)
    paint.style = Paint.Style.FILL
    paint.textAlign = Paint.Align.CENTER

    starters.take(playerCount).forEachIndexed { index, player ->
        val pos = positions.getOrElse(index) { PitchPosition(0.5f, 0.5f, "JUG") }
        val cx = pitchRect.left + (pitchRect.width() * pos.xRatio)
        val cy = pitchRect.top + (pitchRect.height() * pos.yRatio)

        // Player circle
        paint.color = if (player.isGoalkeeper || index == 0) AndroidColor.parseColor("#10B981") else AndroidColor.parseColor("#F59E0B")
        canvas.drawCircle(cx, cy, 32f, paint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, 32f, border)

        // Dorsal number
        paint.color = AndroidColor.parseColor("#0F172A")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${player.dorsal}", cx, cy + 8f, paint)

        // Name tag below
        paint.color = AndroidColor.WHITE
        paint.textSize = 20f
        val shortName = player.name.split(" ").firstOrNull() ?: player.name
        canvas.drawText(shortName, cx, cy + 54f, paint)
    }

    // Footer Watermark
    paint.color = AndroidColor.parseColor("#64748B")
    paint.textSize = 22f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("TheBiG • Gestión Profesional de Torneos de Fútbol", 540f, 1020f, paint)

    return bitmap
}
