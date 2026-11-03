package com.example.re_pro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.re_pro.data.model.AppThemeMode
import com.example.re_pro.data.model.ThemeCategory
import com.example.re_pro.ui.theme.*
import com.example.re_pro.ui.components.DynamicBackground
import com.example.re_pro.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    settingsUiState: SettingsUiState,
    onBackClick: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectBackgroundImage: (String?) -> Unit = {},
    onChangeBlur: (Float) -> Unit = {},
    onChangeOpacity: (Float) -> Unit = {}
) {
    val userSettings = settingsUiState.userSettings
    val currentTheme = userSettings.themeMode

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicBackground(albumArtUri = null)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Personalización", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                ThemeCategory.entries.forEach { category ->
                    val categoryThemes = AppThemeMode.entries.filter { it.category == category }
                    if (categoryThemes.isNotEmpty()) {
                        Text(
                            text = category.categoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 800.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            userScrollEnabled = false
                        ) {
                            items(categoryThemes) { mode ->
                                ThemeGridItem(
                                    themeMode = mode,
                                    isSelected = mode == currentTheme,
                                    onSelect = { onSelectTheme(mode) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ThemeGridItem(
    themeMode: AppThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val sampleColors = when (themeMode) {
        AppThemeMode.REPRO_NEON_BLACK -> listOf(NeonBackground, NeonSurface, NeonPrimary)
        AppThemeMode.AMOLED_PITCH_BLACK -> listOf(AmoledBackground, AmoledSurface, AmoledPrimary)
        AppThemeMode.CYBERPUNK_SALMON -> listOf(CyberBackground, CyberSurface, CyberPrimary)
        AppThemeMode.DARK_EMERALD -> listOf(EmeraldBackground, EmeraldSurface, EmeraldPrimary)
        AppThemeMode.DEEP_SAPPHIRE -> listOf(SapphireBackground, SapphireSurface, SapphirePrimary)
        AppThemeMode.MIDNIGHT_PURPLE -> listOf(PurpleBackground, PurpleSurface, PurplePrimary)
        AppThemeMode.PURE_WHITE_LIGHT -> listOf(LightBackground, LightSurface, LightPrimary)
        AppThemeMode.PASTEL_CREAM_DAY -> listOf(CreamBackground, CreamSurface, CreamPrimary)
        AppThemeMode.SKY_BLUE_LIGHT -> listOf(SkyBackground, SkySurface, SkyPrimary)
        AppThemeMode.MINT_SPRING -> listOf(MintBackground, MintSurface, MintPrimary)
        AppThemeMode.PEACH_BLOSSOM_DAY -> listOf(PeachBackground, PeachSurface, PeachPrimary)
        AppThemeMode.LAVENDER_MARSHMALLOW -> listOf(LavenderBackground, LavenderSurface, LavenderPrimary)
        AppThemeMode.LEMON_SORBET_DAY -> listOf(LemonBackground, LemonSurface, LemonPrimary)
        AppThemeMode.BUBBLEGUM_CLOUD -> listOf(BubbleBackground, BubbleSurface, BubblePrimary)
        AppThemeMode.AQUA_LAGOON_DAY -> listOf(AquaBackground, AquaSurface, AquaPrimary)
        AppThemeMode.SUNSET_ORANGE -> listOf(SunsetBackground, SunsetSurface, SunsetPrimary)
        AppThemeMode.GOLDEN_AUTUMN -> listOf(AutumnBackground, AutumnSurface, AutumnPrimary)
        AppThemeMode.ROSE_QUARTZ -> listOf(RoseBackground, RoseSurface, RosePrimary)
        AppThemeMode.PASTEL_BLUE_PIRANHA -> listOf(BlueBackground, BlueSurface, BluePrimary)
        AppThemeMode.NORDIC_FROST -> listOf(NordicBackground, NordicSurface, NordicPrimary)
        AppThemeMode.PSYCHEDELIC_RAINBOW -> listOf(PsyBackground, PsySurface, PsyPrimary)
        AppThemeMode.NEON_GROOVY -> listOf(GroovyBackground, GroovySurface, GroovyPrimary)
        AppThemeMode.GALAXY_TRIP -> listOf(GalaxyBackground, GalaxySurface, GalaxyPrimary)
        AppThemeMode.RETRO_FIESTA -> listOf(RetroBackground, RetroSurface, RetroPrimary)
        AppThemeMode.CANDY_POP -> listOf(CandyBackground, CandySurface, CandyPrimary)
        AppThemeMode.GLASSMORPHISM_APPLE -> listOf(GlassBackground, GlassSurface, GlassPrimary)
    }
    val bgColor = sampleColors[0]
    val textColor = if (themeMode.isLight) {
        Color(0xFF0F172A)
    } else {
        Color.White
    }

    Surface(
        modifier = Modifier
            .aspectRatio(0.85f)
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = textColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(3.dp)
                )
            }

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    sampleColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = themeMode.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
