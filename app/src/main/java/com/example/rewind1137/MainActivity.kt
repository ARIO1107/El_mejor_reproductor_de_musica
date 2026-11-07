package com.example.rewind1137

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.example.rewind1137.receiver.AudioBecomesNoisyReceiver
import com.example.rewind1137.ui.navigation.AppNavGraph
import com.example.rewind1137.ui.theme.RewindAppTheme
import com.example.rewind1137.viewmodel.LibraryViewModel
import com.example.rewind1137.viewmodel.PlayerViewModel
import com.example.rewind1137.viewmodel.SettingsViewModel
import androidx.lifecycle.lifecycleScope
import com.example.rewind1137.spotify.SpotifyAuthManager
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SpotifyAuthManager.init(this)
        handleSpotifyAuthIntent(intent)
        val crashLog = readCrashLogFile(this)

        setContent {
            var showCrashReport by remember { mutableStateOf(crashLog != null) }

            if (showCrashReport && crashLog != null) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("Rewind 1137 se cerro (detalles del fallo)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    },
                    text = {
                        Text(
                            crashLog,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteCrashLogFile(this)
                            showCrashReport = false
                        }) {
                            Text("Borrar registro y continuar")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            } else {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val libraryViewModel: LibraryViewModel = hiltViewModel()
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            val context = LocalContext.current

            DisposableEffect(Unit) {
                val noisyReceiver = AudioBecomesNoisyReceiver()
                val filter = IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                val notifFilter = IntentFilter().apply {
                    addAction("ACTION_TOGGLE_MODE")
                }
                val notifReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            "ACTION_TOGGLE_MODE" -> playerViewModel.togglePlaybackMode()
                        }
                    }
                }

                ContextCompat.registerReceiver(context, noisyReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                ContextCompat.registerReceiver(context, notifReceiver, notifFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

                var lastMediaChangeCall = 0L
                val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        val now = System.currentTimeMillis()
                        if (now - lastMediaChangeCall < 1500L) return
                        lastMediaChangeCall = now
                        libraryViewModel.refreshSongs()
                    }
                }
                context.contentResolver.registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mediaStoreObserver
                )

                onDispose {
                    try { context.unregisterReceiver(noisyReceiver) } catch (e: Exception) { e.printStackTrace() }
                    try { context.unregisterReceiver(notifReceiver) } catch (e: Exception) { e.printStackTrace() }
                    try { context.contentResolver.unregisterContentObserver(mediaStoreObserver) } catch (e: Exception) { e.printStackTrace() }
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && hasReadAudioPermission(context)) {
                        libraryViewModel.refreshOnForeground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val audioGranted = permissionsMap[Manifest.permission.READ_MEDIA_AUDIO] ?: false
                val storageGranted = permissionsMap[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
                if (audioGranted || storageGranted) {
                    libraryViewModel.refreshSongs()
                }
            }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }

            LaunchedEffect(Unit) {
                if (settingsViewModel.isFirstLaunch()) {
                    settingsViewModel.markFirstLaunchComplete()
                }
            }

            RewindAppTheme(
                themeMode = settingsUiState.userSettings.themeMode,
                backgroundImageUri = settingsUiState.userSettings.backgroundImageUri,
                blurRadius = settingsUiState.userSettings.backgroundBlur,
                opacity = settingsUiState.userSettings.backgroundOpacity
            ) {
                val navController = rememberNavController()

                AppNavGraph(
                    navController = navController
                )
            }
            }
        }
    }

    private fun hasReadAudioPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun readCrashLogFile(context: Context): String? {
        return try {
            val file = File(context.filesDir, "crash_log.txt")
            if (file.exists()) file.readText().trim().ifBlank { null } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteCrashLogFile(context: Context) {
        try {
            File(context.filesDir, "crash_log.txt").delete()
        } catch (_: Exception) {
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSpotifyAuthIntent(intent)
    }

    private fun handleSpotifyAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "rewind" && data.host == "spotify-callback") {
            val code = data.getQueryParameter("code")
            lifecycleScope.launch {
                SpotifyAuthManager.handleAuthResult(this@MainActivity, code)
            }
        }
    }
}
