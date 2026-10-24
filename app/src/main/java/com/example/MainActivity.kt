package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.TheBiGTheme
import com.example.ui.viewmodel.MatchViewModel
import com.example.ui.viewmodel.MatchViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import com.example.ui.viewmodel.TeamViewModel
import com.example.ui.viewmodel.TeamViewModelFactory
import com.example.ui.viewmodel.TournamentViewModel
import com.example.ui.viewmodel.TournamentViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TheBiGApp.instance.repository

        val tournamentViewModel: TournamentViewModel by viewModels {
            TournamentViewModelFactory(repository)
        }
        val teamViewModel: TeamViewModel by viewModels {
            TeamViewModelFactory(repository)
        }
        val matchViewModel: MatchViewModel by viewModels {
            MatchViewModelFactory(repository)
        }
        val settingsViewModel: SettingsViewModel by viewModels {
            SettingsViewModelFactory(repository, applicationContext)
        }

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Granted or dismissed */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            TheBiGTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        tournamentViewModel = tournamentViewModel,
                        teamViewModel = teamViewModel,
                        matchViewModel = matchViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
