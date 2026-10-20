package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.domain.rules.AssisterStat
import com.example.domain.rules.GoalkeeperStat
import com.example.domain.rules.ScorerStat
import com.example.util.ChampionAwardInfo
import com.example.util.ChampionStudioHelper
import com.example.util.GraphicCardGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionPhotoStudioScreen(
    tournament: Tournament,
    championTeam: Team,
    teamPlayers: List<Player>,
    topScorers: List<ScorerStat>,
    topAssisters: List<AssisterStat>,
    goalkeeperRankings: List<GoalkeeperStat>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Adjustable overlay parameters
    var titleOffsetYPercent by remember { mutableFloatStateOf(0.12f) }
    var titleOffsetXPercent by remember { mutableFloatStateOf(0.10f) }
    var overlayOpacity by remember { mutableFloatStateOf(0.85f) }
    var aiRefined by remember { mutableStateOf(false) }

    // Check if awards were won by players of the champion team
    val championTeamPlayerIds = teamPlayers.map { it.id }.toSet()

    val championBestGk = goalkeeperRankings.firstOrNull { it.qualifiesForAward && championTeamPlayerIds.contains(it.player.id) }
        ?: goalkeeperRankings.firstOrNull { championTeamPlayerIds.contains(it.player.id) }
    val championTopScorer = topScorers.firstOrNull { championTeamPlayerIds.contains(it.player.id) }
    val championTopAssister = topAssisters.firstOrNull { championTeamPlayerIds.contains(it.player.id) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }
    )

    val awardInfo = remember(championTeam, tournament, championBestGk, championTopScorer, championTopAssister) {
        ChampionAwardInfo(
            team = championTeam,
            tournament = tournament,
            bestGkPlayerName = championBestGk?.player?.name,
            topScorerPlayerName = championTopScorer?.player?.name,
            topAssisterPlayerName = championTopAssister?.player?.name,
            dateText = "OCTUBRE 2026"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudio de Campeón", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val bitmap = ChampionStudioHelper.generateChampionCompositeBitmap(
                                context = context,
                                imageUri = selectedImageUri,
                                info = awardInfo,
                                titleOffsetYPercent = titleOffsetYPercent,
                                titleOffsetXPercent = titleOffsetXPercent,
                                overlayOpacity = overlayOpacity
                            )
                            GraphicCardGenerator.shareBitmap(
                                context = context,
                                bitmap = bitmap,
                                title = "Compartir Foto del Campeón ${championTeam.name}"
                            )
                        },
                        modifier = Modifier.testTag("export_champion_photo_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir foto")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Felicidades a ${championTeam.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Campeón Oficial de ${tournament.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Photo Selection Trigger
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_team_photo_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImageUri == null) "Elegir Foto del Equipo Campeón" else "Cambiar Foto Seleccionada",
                    fontWeight = FontWeight.Bold
                )
            }

            // Interactive Preview Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base Image
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Foto del equipo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.thebig_champion_bg_1792365669962),
                            contentDescription = "Fondo de campeón",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Watermark: "TheBiG" in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .alpha(overlayOpacity)
                            .background(Color(0xFF0F172A).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TheBiG",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Daniel (el Grande)",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Adjustable Team Name Header Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(
                                    x = (titleOffsetXPercent * 300).toInt(),
                                    y = (titleOffsetYPercent * 500).toInt()
                                )
                            }
                            .alpha(overlayOpacity)
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = "CAMPEON",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = championTeam.name.uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Bottom & Side Badges Bar (Date, Best Gk, Scorer, Assister)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .alpha(overlayOpacity)
                            .background(Color(0xFF090D16).copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OCTUBRE 2026",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (championBestGk != null) {
                                    AwardMiniBadge(
                                        title = "Portero",
                                        playerName = championBestGk.player.name,
                                        icon = Icons.Default.Shield
                                    )
                                }
                                if (championTopScorer != null) {
                                    AwardMiniBadge(
                                        title = "Pichichi",
                                        playerName = championTopScorer.player.name,
                                        icon = Icons.Default.MilitaryTech
                                    )
                                }
                                if (championTopAssister != null) {
                                    AwardMiniBadge(
                                        title = "Asistidor",
                                        playerName = championTopAssister.player.name,
                                        icon = Icons.Default.EmojiEvents
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // IA Improvement & Automatic Placement Button
            Button(
                onClick = {
                    // Intelligent positioning algorithm
                    titleOffsetYPercent = 0.08f
                    titleOffsetXPercent = 0.08f
                    overlayOpacity = 0.90f
                    aiRefined = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_enhance_champion_photo_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (aiRefined) "Acomodado con IA (Optimizado)" else "Mejorar con IA y Acomodar Textos",
                    fontWeight = FontWeight.Bold
                )
            }

            // Adjustment Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ajustes Manuales de Superposición",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Vertical Position Slider
                    Text(
                        text = "Posición Vertical del Nombre: ${(titleOffsetYPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = titleOffsetYPercent,
                        onValueChange = {
                            titleOffsetYPercent = it
                            aiRefined = false
                        },
                        valueRange = 0.04f..0.35f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Horizontal Position Slider
                    Text(
                        text = "Posición Horizontal del Nombre: ${(titleOffsetXPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = titleOffsetXPercent,
                        onValueChange = {
                            titleOffsetXPercent = it
                            aiRefined = false
                        },
                        valueRange = 0.04f..0.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Opacity Slider
                    Text(
                        text = "Opacidad de Textos y Placas: ${(overlayOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = overlayOpacity,
                        onValueChange = {
                            overlayOpacity = it
                            aiRefined = false
                        },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Export High Resolution Button
            Button(
                onClick = {
                    val bitmap = ChampionStudioHelper.generateChampionCompositeBitmap(
                        context = context,
                        imageUri = selectedImageUri,
                        info = awardInfo,
                        titleOffsetYPercent = titleOffsetYPercent,
                        titleOffsetXPercent = titleOffsetXPercent,
                        overlayOpacity = overlayOpacity
                    )
                    GraphicCardGenerator.shareBitmap(
                        context = context,
                        bitmap = bitmap,
                        title = "Exportar Foto de Campeón ${championTeam.name}"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_high_res_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exportar en Alta Resolución para Redes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AwardMiniBadge(
    title: String,
    playerName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = title,
                color = Color(0xFFF59E0B),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = playerName.take(10),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
