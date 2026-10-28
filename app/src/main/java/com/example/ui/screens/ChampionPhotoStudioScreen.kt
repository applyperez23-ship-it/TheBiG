package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TheBiGApp
import com.example.data.local.entity.Player
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.domain.rules.AssisterStat
import com.example.domain.rules.GoalkeeperStat
import com.example.domain.rules.ScorerStat
import com.example.util.GraphicCardGenerator
import com.example.util.ImageEnhancer
import com.example.util.TeamCrestView
import com.example.util.TeamJerseyView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionPhotoStudioScreen(
    tournament: Tournament,
    championTeam: Team,
    teamPlayers: List<Player> = emptyList(),
    topScorers: List<ScorerStat> = emptyList(),
    topAssisters: List<AssisterStat> = emptyList(),
    goalkeeperRankings: List<GoalkeeperStat> = emptyList(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val app = TheBiGApp.instance

    var players by remember { mutableStateOf(teamPlayers) }
    var customPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isEnhancedWithAi by remember { mutableStateOf(false) }
    var generatedCardBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(championTeam.id, teamPlayers) {
        if (teamPlayers.isNotEmpty()) {
            players = teamPlayers
        } else {
            players = app.repository.getPlayersForTeamOnce(championTeam.id)
        }
    }

    // Photo picker launcher (using Android Photo Picker contract without broad permissions)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                customPhotoBitmap = originalBitmap
                isEnhancedWithAi = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Recompute generated bitmap when dependencies change
    LaunchedEffect(players, customPhotoBitmap, isEnhancedWithAi) {
        val photoToUse = if (isEnhancedWithAi && customPhotoBitmap != null) {
            ImageEnhancer.enhanceCrestOrPhoto(customPhotoBitmap!!)
        } else {
            customPhotoBitmap
        }

        generatedCardBitmap = GraphicCardGenerator.generateChampionFrameBitmap(
            tournament = tournament,
            championTeam = championTeam,
            players = players,
            customPhoto = photoToUse
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cuadro del Campeon",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${tournament.name} • ${championTeam.name}",
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
                            generatedCardBitmap?.let { bmp ->
                                GraphicCardGenerator.shareBitmap(
                                    context = context,
                                    bitmap = bmp,
                                    title = "Cuadro del Campeon - ${championTeam.name} (${tournament.name})"
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar Tarjeta 1:1")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Champion Banner Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamCrestView(team = championTeam, size = 64.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CAMPEON OFICIAL",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = championTeam.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        val captain = players.firstOrNull { it.isCaptain }
                        if (captain != null) {
                            Text(
                                text = "Capitan: ${captain.name} (#${captain.number})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Photo and AI Graphic Enhancement Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Personalización y Filtro Gráfico",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Puedes usar el escudo vectorial en alta definición o subir una foto grupal del equipo campeón para procesarla con el filtro gráfico de nitidez y contraste.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (customPhotoBitmap != null) "Cambiar Foto" else "Subir Foto", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { isEnhancedWithAi = !isEnhancedWithAi },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnhancedWithAi) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEnhancedWithAi) "Filtro HD Activo" else "Filtro Gráfico HD", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Official Card Live Preview
            Text(
                text = "Vista Previa Oficial",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            generatedCardBitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                        .background(Color.Black, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Tarjeta de Campeon Oficial",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Export & Share Button
            Button(
                onClick = {
                    generatedCardBitmap?.let { bmp ->
                        GraphicCardGenerator.shareBitmap(
                            context = context,
                            bitmap = bmp,
                            title = "Cuadro del Campeon Oficial - ${championTeam.name}"
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_champion_card_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir Imagen del Campeón", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
