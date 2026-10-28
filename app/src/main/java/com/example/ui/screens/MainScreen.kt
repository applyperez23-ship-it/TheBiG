package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Team
import com.example.data.local.entity.Tournament
import com.example.ui.components.OnboardingDialog
import com.example.ui.viewmodel.MatchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TeamViewModel
import com.example.ui.viewmodel.TournamentViewModel

sealed class AppDestination {
    object Main : AppDestination()
    data class TournamentDetail(val tournamentId: Long) : AppDestination()
    data class TeamDetail(val teamId: Long) : AppDestination()
    data class MatchLive(val matchId: Long) : AppDestination()
    data class ChampionStudio(
        val tournament: Tournament,
        val championTeam: Team
    ) : AppDestination()
}

@Composable
fun MainScreen(
    tournamentViewModel: TournamentViewModel,
    teamViewModel: TeamViewModel,
    matchViewModel: MatchViewModel,
    settingsViewModel: SettingsViewModel,
    initialMatchId: Long? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentDestination by remember {
        mutableStateOf<AppDestination>(
            if (initialMatchId != null && initialMatchId > 0) AppDestination.MatchLive(initialMatchId)
            else AppDestination.Main
        )
    }

    val onboardingShown by settingsViewModel.onboardingShown.collectAsStateWithLifecycle()
    val allTeams by teamViewModel.teams.collectAsStateWithLifecycle()

    // Onboarding Mandatory Dialog for first-launch
    if (!onboardingShown) {
        OnboardingDialog(
            onDismiss = { settingsViewModel.acceptOnboarding() }
        )
    }

    // Defensive BackHandler for sub-destinations
    BackHandler(enabled = currentDestination !is AppDestination.Main) {
        currentDestination = AppDestination.Main
    }

    when (val dest = currentDestination) {
        is AppDestination.Main -> {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Torneos") },
                            label = { Text("Torneos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_tournaments"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Shield, contentDescription = "Equipos") },
                            label = { Text("Equipos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_teams"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.SportsSoccer, contentDescription = "Partidos") },
                            label = { Text("Partidos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_matches"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                            label = { Text("Ajustes", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            modifier = Modifier.testTag("tab_settings"),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (selectedTab) {
                        0 -> TournamentsScreen(
                            viewModel = tournamentViewModel,
                            onTournamentClick = { tId ->
                                currentDestination = AppDestination.TournamentDetail(tId)
                            }
                        )
                        1 -> TeamsScreen(
                            viewModel = teamViewModel,
                            onTeamClick = { teamId ->
                                currentDestination = AppDestination.TeamDetail(teamId)
                            }
                        )
                        2 -> MatchesScreen(
                            viewModel = matchViewModel,
                            allTeams = allTeams,
                            onMatchClick = { matchId ->
                                currentDestination = AppDestination.MatchLive(matchId)
                            }
                        )
                        3 -> SettingsScreen(
                            viewModel = settingsViewModel
                        )
                    }
                }
            }
        }

        is AppDestination.TournamentDetail -> {
            TournamentDetailScreen(
                tournamentId = dest.tournamentId,
                viewModel = tournamentViewModel,
                onBack = { currentDestination = AppDestination.Main },
                onMatchClick = { mId -> currentDestination = AppDestination.MatchLive(mId) },
                onOpenChampionStudio = { champTeam ->
                    val tournaments = tournamentViewModel.tournaments.value
                    val tournament = tournaments.firstOrNull { it.id == dest.tournamentId }
                    if (tournament != null) {
                        currentDestination = AppDestination.ChampionStudio(tournament, champTeam)
                    }
                }
            )
        }

        is AppDestination.TeamDetail -> {
            TeamDetailScreen(
                teamId = dest.teamId,
                viewModel = teamViewModel,
                onBack = { currentDestination = AppDestination.Main }
            )
        }

        is AppDestination.MatchLive -> {
            MatchLiveSheetScreen(
                matchId = dest.matchId,
                viewModel = matchViewModel,
                onBack = { currentDestination = AppDestination.Main }
            )
        }

        is AppDestination.ChampionStudio -> {
            ChampionPhotoStudioScreen(
                tournament = dest.tournament,
                championTeam = dest.championTeam,
                onBack = { currentDestination = AppDestination.Main }
            )
        }
    }
}
