package com.example.re_pro.ui.theme

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.re_pro.data.model.AppThemeMode

val NeonBackground = Color(0xFF120C0C)
val NeonSurface = Color(0xFF2B2020)
val NeonSurfaceVariant = Color(0xFF3F3030)
val NeonPrimary = Color(0xFFFBB8AC)
val NeonContainer = Color(0xFF5A3E3C)
val NeonTextPrimary = Color(0xFFFBE4E0)
val NeonTextSecondary = Color(0xFFA89694)

val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF222222)
val AmoledSurfaceVariant = Color(0xFF333333)
val AmoledPrimary = Color(0xFFFFFFFF)
val AmoledContainer = Color(0xFF3A3A3A)

val CyberBackground = Color(0xFF0B0E14)
val CyberSurface = Color(0xFF202A3B)
val CyberSurfaceVariant = Color(0xFF2D3E56)
val CyberPrimary = Color(0xFFFF2A85)
val CyberContainer = Color(0xFF5B1638)

val EmeraldBackground = Color(0xFF061A14)
val EmeraldSurface = Color(0xFF163E32)
val EmeraldSurfaceVariant = Color(0xFF225847)
val EmeraldPrimary = Color(0xFF34D399)
val EmeraldContainer = Color(0xFF0A7458)

val SapphireBackground = Color(0xFF070F26)
val SapphireSurface = Color(0xFF192A56)
val SapphireSurfaceVariant = Color(0xFF273E78)
val SapphirePrimary = Color(0xFF38BDF8)
val SapphireContainer = Color(0xFF23479B)

val PurpleBackground = Color(0xFF120A2A)
val PurpleSurface = Color(0xFF2B1B5A)
val PurpleSurfaceVariant = Color(0xFF3E2B7B)
val PurplePrimary = Color(0xFFA855F7)
val PurpleContainer = Color(0xFF6B21A8)

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightPrimary = Color(0xFF2563EB)
val LightContainer = Color(0xFFDBEAFE)

val CreamBackground = Color(0xFFFDFBF7)
val CreamSurface = Color(0xFFFFFDF9)
val CreamSurfaceVariant = Color(0xFFF7F2EA)
val CreamPrimary = Color(0xFFE07A5F)
val CreamContainer = Color(0xFFF4A261)

val SkyBackground = Color(0xFFF0F9FF)
val SkySurface = Color(0xFFFFFFFF)
val SkySurfaceVariant = Color(0xFFE0F2FE)
val SkyPrimary = Color(0xFF0284C7)
val SkyContainer = Color(0xFFBAE6FD)

val MintBackground = Color(0xFFF2FBF6)
val MintSurface = Color(0xFFFFFFFF)
val MintSurfaceVariant = Color(0xFFD7F2E4)
val MintPrimary = Color(0xFF059669)
val MintContainer = Color(0xFFA7F3D0)

val PeachBackground = Color(0xFFFFF7F5)
val PeachSurface = Color(0xFFFFFFFF)
val PeachSurfaceVariant = Color(0xFFFFE4DC)
val PeachPrimary = Color(0xFFE11D63)
val PeachContainer = Color(0xFFFECDD3)

val LavenderBackground = Color(0xFFF9F7FF)
val LavenderSurface = Color(0xFFFFFFFF)
val LavenderSurfaceVariant = Color(0xFFECE7FB)
val LavenderPrimary = Color(0xFF7C3AED)
val LavenderContainer = Color(0xFFDDD6FE)

val LemonBackground = Color(0xFFFFFBEB)
val LemonSurface = Color(0xFFFFFFFF)
val LemonSurfaceVariant = Color(0xFFFEF3C7)
val LemonPrimary = Color(0xFFD97706)
val LemonContainer = Color(0xFFFDE68A)

val BubbleBackground = Color(0xFFFFF7FB)
val BubbleSurface = Color(0xFFFFFFFF)
val BubbleSurfaceVariant = Color(0xFFFFE4F2)
val BubblePrimary = Color(0xFFDB2777)
val BubbleContainer = Color(0xFFFBCFE8)

val AquaBackground = Color(0xFFF2FBFC)
val AquaSurface = Color(0xFFFFFFFF)
val AquaSurfaceVariant = Color(0xFFD6F0F4)
val AquaPrimary = Color(0xFF0F766E)
val AquaContainer = Color(0xFF99F6E4)

val SunsetBackground = Color(0xFF1C0D0A)
val SunsetSurface = Color(0xFF3A211B)
val SunsetSurfaceVariant = Color(0xFF52332C)
val SunsetPrimary = Color(0xFFFB923C)
val SunsetContainer = Color(0xFFB43E17)

val AutumnBackground = Color(0xFF1E170A)
val AutumnSurface = Color(0xFF3A2F1B)
val AutumnSurfaceVariant = Color(0xFF52442C)
val AutumnPrimary = Color(0xFFF59E0B)
val AutumnContainer = Color(0xFF92400E)

val RoseBackground = Color(0xFF1F1216)
val RoseSurface = Color(0xFF3B232D)
val RoseSurfaceVariant = Color(0xFF543745)
val RosePrimary = Color(0xFFF472B6)
val RoseContainer = Color(0xFF9D174D)

val BlueBackground = Color(0xFF0F172A)
val BlueSurface = Color(0xFF28364F)
val BlueSurfaceVariant = Color(0xFF3B4E6C)
val BluePrimary = Color(0xFF38BDF8)
val BlueContainer = Color(0xFF0284C7)

val NordicBackground = Color(0xFF0D1B2A)
val NordicSurface = Color(0xFF24344D)
val NordicSurfaceVariant = Color(0xFF364E6B)
val NordicPrimary = Color(0xFFE0E1DD)
val NordicContainer = Color(0xFF526D82)

val PsyBackground = Color(0xFF14051F)
val PsySurface = Color(0xFF2A1146)
val PsySurfaceVariant = Color(0xFF3F1B64)
val PsyPrimary = Color(0xFFFF3ED6)
val PsyContainer = Color(0xFF7A1FA8)
val PsyTextPrimary = Color(0xFFFFF0FA)
val PsyTextSecondary = Color(0xFFD8B4FE)

val GroovyBackground = Color(0xFF0A0E1A)
val GroovySurface = Color(0xFF1E2A4A)
val GroovySurfaceVariant = Color(0xFF33417A)
val GroovyPrimary = Color(0xFF00E5FF)
val GroovyContainer = Color(0xFF0E7AA0)
val GroovyTextPrimary = Color(0xFFECFEFF)
val GroovyTextSecondary = Color(0xFF7DE8FF)

val GalaxyBackground = Color(0xFF0E0818)
val GalaxySurface = Color(0xFF2A1C52)
val GalaxySurfaceVariant = Color(0xFF3D2C74)
val GalaxyPrimary = Color(0xFFC084FC)
val GalaxyContainer = Color(0xFF6D28D9)
val GalaxyTextPrimary = Color(0xFFF5F0FF)
val GalaxyTextSecondary = Color(0xFFC4B5FD)

val RetroBackground = Color(0xFF160B16)
val RetroSurface = Color(0xFF3A1230)
val RetroSurfaceVariant = Color(0xFF571C46)
val RetroPrimary = Color(0xFFFFB100)
val RetroContainer = Color(0xFFB45309)
val RetroTextPrimary = Color(0xFFFFFDF0)
val RetroTextSecondary = Color(0xFFFCD34D)

val CandyBackground = Color(0xFF1A0B1F)
val CandySurface = Color(0xFF3C1550)
val CandySurfaceVariant = Color(0xFF5A2178)
val CandyPrimary = Color(0xFFFF5FA2)
val CandyContainer = Color(0xFFA21C54)
val CandyTextPrimary = Color(0xFFFFF0F7)
val CandyTextSecondary = Color(0xFFF9A8FF)

val GlassBackground = Color(0xFF0A0C10)
val GlassSurface = Color(0x33FFFFFF)
val GlassSurfaceVariant = Color(0x22FFFFFF)
val GlassPrimary = Color(0xFF60A5FA)
val GlassContainer = Color(0x443B82F6)

private fun buildReProScheme(
    light: Boolean,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onBackground: Color,
    onSurface: Color,
    onSurfaceVariant: Color
): ColorScheme {
    val toward = if (light) Color.Black else Color.White
    val away = if (light) Color.White else Color.Black

    val secondary = if (light) lerp(primary, toward, 0.22f) else lerp(primary, toward, 0.32f)
    val onSecondary = if (light) Color.White else Color.Black
    val secondaryContainer = lerp(secondary, away, if (light) 0.60f else 0.72f)
    val onSecondaryContainer = if (light) toward.copy(alpha = 0.85f) else Color.White

    val tertiary = lerp(secondary, toward, 0.10f)
    val onTertiary = onSecondary
    val tertiaryContainer = lerp(secondary, away, if (light) 0.35f else 0.60f)
    val onTertiaryContainer = onSecondaryContainer

    val error = if (light) Color(0xFFBA1A1A) else Color(0xFFFFB4AB)
    val onError = if (light) Color.White else Color.Black
    val errorContainer = if (light) Color(0xFFFFDAD6) else Color(0xFF93000A)
    val onErrorContainer = if (light) Color(0xFF410002) else Color(0xFFFFDAD6)

    val outline = lerp(onSurfaceVariant, surface, 0.42f)
    val outlineVariant = lerp(onSurfaceVariant, surface, 0.68f)

    val surfaceContainerLowest = if (light) Color.White else lerp(surface, Color.Black, 0.10f)
    val surfaceContainerLow = surface
    val surfaceContainer = surfaceVariant
    val surfaceContainerHigh = lerp(surfaceVariant, toward, 0.05f)
    val surfaceContainerHighest = lerp(surfaceVariant, toward, if (light) 0.08f else 0.12f)

    val inverseSurface = if (light) Color(0xFF2F3036) else Color(0xFFE6E1E5)
    val inverseOnSurface = if (light) Color(0xFFF2F0F4) else Color(0xFF2F3036)
    val inversePrimary = if (light) lerp(primary, Color.White, 0.55f) else lerp(primary, Color.White, 0.30f)

    return if (light) {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = toward.copy(alpha = 0.85f),
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            error = error, onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline, outlineVariant = outlineVariant,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary, scrim = Color.Black
        )
    } else {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = if (light) toward.copy(alpha = 0.85f) else lerp(primary, Color.White, 0.85f),
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            error = error, onError = onError, errorContainer = errorContainer, onErrorContainer = onErrorContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline, outlineVariant = outlineVariant,
            inverseSurface = inverseSurface, inverseOnSurface = inverseOnSurface,
            inversePrimary = inversePrimary, scrim = Color.Black
        )
    }
}

fun getReProColorScheme(
    themeMode: AppThemeMode,
    isGlassActive: Boolean = false
): ColorScheme {
    val baseScheme = when (themeMode) {
        AppThemeMode.REPRO_NEON_BLACK -> buildReProScheme(
            light = false,
            background = NeonBackground, surface = NeonSurface, surfaceVariant = NeonSurfaceVariant,
            primary = NeonPrimary, onPrimary = Color.Black, primaryContainer = NeonContainer,
            onBackground = NeonTextPrimary, onSurface = NeonTextPrimary, onSurfaceVariant = NeonTextSecondary
        )
        AppThemeMode.AMOLED_PITCH_BLACK -> buildReProScheme(
            light = false,
            background = AmoledBackground, surface = AmoledSurface, surfaceVariant = AmoledSurfaceVariant,
            primary = AmoledPrimary, onPrimary = Color.Black, primaryContainer = AmoledContainer,
            onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color.Gray
        )
        AppThemeMode.CYBERPUNK_SALMON -> buildReProScheme(
            light = false,
            background = CyberBackground, surface = CyberSurface, surfaceVariant = CyberSurfaceVariant,
            primary = CyberPrimary, onPrimary = Color.White, primaryContainer = CyberContainer,
            onBackground = Color(0xFFF9FAFB), onSurface = Color(0xFFF9FAFB), onSurfaceVariant = Color(0xFF9CA3AF)
        )
        AppThemeMode.DARK_EMERALD -> buildReProScheme(
            light = false,
            background = EmeraldBackground, surface = EmeraldSurface, surfaceVariant = EmeraldSurfaceVariant,
            primary = EmeraldPrimary, onPrimary = Color.Black, primaryContainer = EmeraldContainer,
            onBackground = Color(0xFFECFDF5), onSurface = Color(0xFFECFDF5), onSurfaceVariant = Color(0xFFA7F3D0)
        )
        AppThemeMode.DEEP_SAPPHIRE -> buildReProScheme(
            light = false,
            background = SapphireBackground, surface = SapphireSurface, surfaceVariant = SapphireSurfaceVariant,
            primary = SapphirePrimary, onPrimary = Color.Black, primaryContainer = SapphireContainer,
            onBackground = Color(0xFFF0F9FF), onSurface = Color(0xFFF0F9FF), onSurfaceVariant = Color(0xFF7DD3FC)
        )
        AppThemeMode.MIDNIGHT_PURPLE -> buildReProScheme(
            light = false,
            background = PurpleBackground, surface = PurpleSurface, surfaceVariant = PurpleSurfaceVariant,
            primary = PurplePrimary, onPrimary = Color.White, primaryContainer = PurpleContainer,
            onBackground = Color(0xFFFAF5FF), onSurface = Color(0xFFFAF5FF), onSurfaceVariant = Color(0xFFE9D5FF)
        )
        AppThemeMode.PURE_WHITE_LIGHT -> buildReProScheme(
            light = true,
            background = LightBackground, surface = LightSurface, surfaceVariant = LightSurfaceVariant,
            primary = LightPrimary, onPrimary = Color.White, primaryContainer = LightContainer,
            onBackground = Color(0xFF0F172A), onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF475569)
        )
        AppThemeMode.PASTEL_CREAM_DAY -> buildReProScheme(
            light = true,
            background = CreamBackground, surface = CreamSurface, surfaceVariant = CreamSurfaceVariant,
            primary = CreamPrimary, onPrimary = Color.White, primaryContainer = CreamContainer,
            onBackground = Color(0xFF2B2D42), onSurface = Color(0xFF2B2D42), onSurfaceVariant = Color(0xFF6C757D)
        )
        AppThemeMode.SKY_BLUE_LIGHT -> buildReProScheme(
            light = true,
            background = SkyBackground, surface = SkySurface, surfaceVariant = SkySurfaceVariant,
            primary = SkyPrimary, onPrimary = Color.White, primaryContainer = SkyContainer,
            onBackground = Color(0xFF0C4A6E), onSurface = Color(0xFF0C4A6E), onSurfaceVariant = Color(0xFF0369A1)
        )
        AppThemeMode.MINT_SPRING -> buildReProScheme(
            light = true,
            background = MintBackground, surface = MintSurface, surfaceVariant = MintSurfaceVariant,
            primary = MintPrimary, onPrimary = Color.White, primaryContainer = MintContainer,
            onBackground = Color(0xFF064E3B), onSurface = Color(0xFF064E3B), onSurfaceVariant = Color(0xFF047857)
        )
        AppThemeMode.PEACH_BLOSSOM_DAY -> buildReProScheme(
            light = true,
            background = PeachBackground, surface = PeachSurface, surfaceVariant = PeachSurfaceVariant,
            primary = PeachPrimary, onPrimary = Color.White, primaryContainer = PeachContainer,
            onBackground = Color(0xFF4C0519), onSurface = Color(0xFF4C0519), onSurfaceVariant = Color(0xFF9D174D)
        )
        AppThemeMode.LAVENDER_MARSHMALLOW -> buildReProScheme(
            light = true,
            background = LavenderBackground, surface = LavenderSurface, surfaceVariant = LavenderSurfaceVariant,
            primary = LavenderPrimary, onPrimary = Color.White, primaryContainer = LavenderContainer,
            onBackground = Color(0xFF2E1065), onSurface = Color(0xFF2E1065), onSurfaceVariant = Color(0xFF6D28D9)
        )
        AppThemeMode.LEMON_SORBET_DAY -> buildReProScheme(
            light = true,
            background = LemonBackground, surface = LemonSurface, surfaceVariant = LemonSurfaceVariant,
            primary = LemonPrimary, onPrimary = Color.White, primaryContainer = LemonContainer,
            onBackground = Color(0xFF451A03), onSurface = Color(0xFF451A03), onSurfaceVariant = Color(0xFF8A6D3B)
        )
        AppThemeMode.BUBBLEGUM_CLOUD -> buildReProScheme(
            light = true,
            background = BubbleBackground, surface = BubbleSurface, surfaceVariant = BubbleSurfaceVariant,
            primary = BubblePrimary, onPrimary = Color.White, primaryContainer = BubbleContainer,
            onBackground = Color(0xFF500724), onSurface = Color(0xFF500724), onSurfaceVariant = Color(0xFF9D174D)
        )
        AppThemeMode.AQUA_LAGOON_DAY -> buildReProScheme(
            light = true,
            background = AquaBackground, surface = AquaSurface, surfaceVariant = AquaSurfaceVariant,
            primary = AquaPrimary, onPrimary = Color.White, primaryContainer = AquaContainer,
            onBackground = Color(0xFF134E4A), onSurface = Color(0xFF134E4A), onSurfaceVariant = Color(0xFF21857C)
        )
        AppThemeMode.SUNSET_ORANGE -> buildReProScheme(
            light = false,
            background = SunsetBackground, surface = SunsetSurface, surfaceVariant = SunsetSurfaceVariant,
            primary = SunsetPrimary, onPrimary = Color.Black, primaryContainer = SunsetContainer,
            onBackground = Color(0xFFFFF7ED), onSurface = Color(0xFFFFF7ED), onSurfaceVariant = Color(0xFFFDBA74)
        )
        AppThemeMode.GOLDEN_AUTUMN -> buildReProScheme(
            light = false,
            background = AutumnBackground, surface = AutumnSurface, surfaceVariant = AutumnSurfaceVariant,
            primary = AutumnPrimary, onPrimary = Color.Black, primaryContainer = AutumnContainer,
            onBackground = Color(0xFFFEF3C7), onSurface = Color(0xFFFEF3C7), onSurfaceVariant = Color(0xFFFCD34D)
        )
        AppThemeMode.ROSE_QUARTZ -> buildReProScheme(
            light = false,
            background = RoseBackground, surface = RoseSurface, surfaceVariant = RoseSurfaceVariant,
            primary = RosePrimary, onPrimary = Color.Black, primaryContainer = RoseContainer,
            onBackground = Color(0xFFFCE7F3), onSurface = Color(0xFFFCE7F3), onSurfaceVariant = Color(0xFFFBCFE8)
        )
        AppThemeMode.PASTEL_BLUE_PIRANHA -> buildReProScheme(
            light = false,
            background = BlueBackground, surface = BlueSurface, surfaceVariant = BlueSurfaceVariant,
            primary = BluePrimary, onPrimary = Color.Black, primaryContainer = BlueContainer,
            onBackground = Color(0xFFF0F9FF), onSurface = Color(0xFFF0F9FF), onSurfaceVariant = Color(0xFF94A3B8)
        )
        AppThemeMode.NORDIC_FROST -> buildReProScheme(
            light = false,
            background = NordicBackground, surface = NordicSurface, surfaceVariant = NordicSurfaceVariant,
            primary = NordicPrimary, onPrimary = Color.Black, primaryContainer = NordicContainer,
            onBackground = Color(0xE0F8FAFC), onSurface = Color(0xE0F8FAFC), onSurfaceVariant = Color(0xFFCBD5E1)
        )
        AppThemeMode.PSYCHEDELIC_RAINBOW -> buildReProScheme(
            light = false,
            background = PsyBackground, surface = PsySurface, surfaceVariant = PsySurfaceVariant,
            primary = PsyPrimary, onPrimary = Color.White, primaryContainer = PsyContainer,
            onBackground = PsyTextPrimary, onSurface = PsyTextPrimary, onSurfaceVariant = PsyTextSecondary
        )
        AppThemeMode.NEON_GROOVY -> buildReProScheme(
            light = false,
            background = GroovyBackground, surface = GroovySurface, surfaceVariant = GroovySurfaceVariant,
            primary = GroovyPrimary, onPrimary = Color.Black, primaryContainer = GroovyContainer,
            onBackground = GroovyTextPrimary, onSurface = GroovyTextPrimary, onSurfaceVariant = GroovyTextSecondary
        )
        AppThemeMode.GALAXY_TRIP -> buildReProScheme(
            light = false,
            background = GalaxyBackground, surface = GalaxySurface, surfaceVariant = GalaxySurfaceVariant,
            primary = GalaxyPrimary, onPrimary = Color.Black, primaryContainer = GalaxyContainer,
            onBackground = GalaxyTextPrimary, onSurface = GalaxyTextPrimary, onSurfaceVariant = GalaxyTextSecondary
        )
        AppThemeMode.RETRO_FIESTA -> buildReProScheme(
            light = false,
            background = RetroBackground, surface = RetroSurface, surfaceVariant = RetroSurfaceVariant,
            primary = RetroPrimary, onPrimary = Color.Black, primaryContainer = RetroContainer,
            onBackground = RetroTextPrimary, onSurface = RetroTextPrimary, onSurfaceVariant = RetroTextSecondary
        )
        AppThemeMode.CANDY_POP -> buildReProScheme(
            light = false,
            background = CandyBackground, surface = CandySurface, surfaceVariant = CandySurfaceVariant,
            primary = CandyPrimary, onPrimary = Color.Black, primaryContainer = CandyContainer,
            onBackground = CandyTextPrimary, onSurface = CandyTextPrimary, onSurfaceVariant = CandyTextSecondary
        )
        AppThemeMode.GLASSMORPHISM_APPLE -> buildReProScheme(
            light = false,
            background = GlassBackground, surface = GlassSurface, surfaceVariant = GlassSurfaceVariant,
            primary = GlassPrimary, onPrimary = Color.White, primaryContainer = GlassContainer,
            onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color.White.copy(alpha = 0.8f)
        )
    }

    return if (isGlassActive || themeMode == AppThemeMode.GLASSMORPHISM_APPLE) {
        baseScheme.copy(
            surface = Color.White.copy(alpha = 0.12f),
            surfaceVariant = Color.White.copy(alpha = 0.18f),
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.85f),
            onBackground = Color.White
        )
    } else baseScheme
}

@Composable
fun ReProAppTheme(
    themeMode: AppThemeMode = AppThemeMode.REPRO_NEON_BLACK,
    backgroundImageUri: String? = null,
    blurRadius: Float = 20f,
    opacity: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val isGlassActive = !backgroundImageUri.isNullOrBlank() || themeMode == AppThemeMode.GLASSMORPHISM_APPLE
    val colorScheme = getReProColorScheme(themeMode, isGlassActive)
    val hasBackgroundImage = !backgroundImageUri.isNullOrBlank()

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasBackgroundImage) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(backgroundImageUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background.copy(alpha = opacity.coerceIn(0.05f, 0.85f)))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.background)
                )
            }
            content()
        }
    }
}