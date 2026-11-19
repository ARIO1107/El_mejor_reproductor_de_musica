# Icono, Widgets, Notificación Player y Modo HiFi — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar la imagen pedida como icono, dejar notificación y 3 widgets totalmente funcionales (tap → PlayerSheet, corazón, X, glow, progreso real, cola restaurada) y añadir un modo HiFi persistente y honesto.

**Architecture:** Entrega secuencial en 4 fases; cada fase compila, se instala en el dispositivo por USB y se commitea por separado. Se extraen dos funciones puras (`PlaybackModeCycle`, `HifiAudioPolicy`) para poder probar con JUnit las reglas que hoy están duplicadas o son código muerto. La comunicación hacia la UI usa un extra de intent de un solo uso (`EXTRA_OPEN_PLAYER_SHEET`) consumido por `AppNavGraph`; la lógica de modo sale de `MainActivity` y vive en `MusicService`. No se toca `Song.toMediaItem()` ni la caché de miniaturas.

**Tech Stack:** Kotlin, Android SDK 37/minSdk 24, Media3 1.5.1, Glance 1.1.1, Hilt, DataStore, JUnit4 + mockk + Robolectric 4.16.1, adb sobre USB.

**Spec:** `docs/superpowers/specs/2026-11-19-icon-widgets-notification-hifi-design.md`

## Global Constraints

- Fuente (build/install): `C:\Users\Usuario\AndroidStudioProjects\RePro_backup_2026-10-13`
- Repo git (commit/push): `C:\Users\Usuario\AppData\Local\Tmp\opencode\RewindGitRepo2`, rama `main`, origin `https://github.com/ARIO1107/El_mejor_reproductor_de_musica.git`
- Sincronización obligatoria tras cada fase:
  ```powershell
  $src='C:\Users\Usuario\AndroidStudioProjects\RePro_backup_2026-10-13\app\src\main'
  $dst='C:\Users\Usuario\AppData\Local\Tmp\opencode\RewindGitRepo2\app\src\main'
  Copy-Item -Path "$src\*" -Destination $dst -Recurse -Force
  ```
- Build: `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; & '.\gradlew.bat' assembleDebug` (PowerShell 5.1: **no usar `&&`**).
- Tests: `& '.\gradlew.bat' testDebugUnitTest`
- Dispositivo: `adb -s 15187705CJ000364 install -r <apk>`. El usuario autorizó explícitamente la depuración USB en esta sesión (el `AGENTS.md` antiguo quedó obsoleto en ese punto).
- APK: `app\build\outputs\apk\debug\app-debug.apk`
- Imagen de entrada: `C:\Users\Usuario\Downloads\image_a47006f8.jpg` (1024×1024).
- Idioma de UI y respuestas: español. Sin etiquetas "Hi-Res" falsas.
- No prometer igualdad con Tidal/Qobuz; no convertir lossy en lossless.
- Cada fase termina con `assembleDebug` + install + prueba en dispositivo + commit + push.

## Review Focus

- Tap en la notificación con la app en background/fría → debe abrir el PlayerSheet de la canción actual, no solo la pantalla principal. Test: verificación en dispositivo en Task 3 (Step 5).
- Pulsar modo/like/next desde widget con `MainActivity` muerta → debe funcionar. Test: verificación en dispositivo en Task 4 (Step 5) y Task 6 (Step 5).
- Seek, pausa o crossfade → el progreso del widget no debe seguir avanzando solo. Test: verificación en dispositivo en Task 5 (Step 4).
- X de la notificación → no debe dejar el widget mostrando una canción muerta. Test: `RewindWidgetStateClearTest` en Task 4 (Step 1) + verificación en dispositivo.
- Activar HiFi con EQ/BassBoost/Virtualizer/VocalRemoval encendidos → deben apagarse y restaurarse al desactivar; además debe persistir tras reinicio. Test: `HifiAudioPolicyTest` en Task 8 (Step 1-2) + verificación en dispositivo en Task 10 (Step 5).

---

## Phase 1 — Icono

### Task 1: Generar e instalar los mipmaps de `image_a47006f8`

**Files:**
- Modify: `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`
- Modify: `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.png`
- Verify: `app/src/main/AndroidManifest.xml:17-19` (no debe cambiar)

**Interfaces:**
- Consumes: JPG 1024×1024 de Descargas.
- Produces: recursos de icono listos; el manifest sigue apuntando a `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.

- [ ] **Step 1: Generar los iconos legacy**

Con `System.Drawing` (o ImageMagick), hacer center-crop 1:1 de la JPG y resize a: mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192 px. Escribir el mismo PNG en `ic_launcher.png` e `ic_launcher_round.png` de cada carpeta.
Check: los 10 archivos existen y sus dimensiones son exactamente las anteriores.

- [ ] **Step 2: Generar el foreground adaptativo**

Center-crop 1:1, resize a 285×285 px y pegar centrado en un canvas transparente de 432×432 px; guardar como `res/drawable/ic_launcher_foreground.png` (PNG con alpha).
Check: el PNG mide 432×432 y los bordes de 74 px son transparentes (zona de seguridad del mask adaptable).

- [ ] **Step 3: Sincronizar al repo y compilar**

Ejecutar el comando de sincronización de Global Constraints; luego `assembleDebug`.
Expected: BUILD SUCCESSFUL, sin errores `duplicate resource` ni `9-patch`.

- [ ] **Step 4: Instalar y verificar en dispositivo**

`adb -s 15187705CJ000364 install -r <apk>`; abrir el launcher y el apartado de apps de Ajustes.
Expected: el nuevo icono aparece en launcher, en el cajón y en Ajustes; no hay icono viejo cacheado tras reiniciar el launcher.

- [ ] **Step 5: Commit y push**

```powershell
git -C 'C:\Users\Usuario\AppData\Local\Tmp\opencode\RewindGitRepo2' add app/src/main/res
git -C 'C:\Users\Usuario\AppData\Local\Tmp\opencode\RewindGitRepo2' commit -m "feat: apply image_a47006f8 as app icon"
git -C 'C:\Users\Usuario\AppData\Local\Tmp\opencode\RewindGitRepo2' push origin main
```

---

## Phase 2 — Notificación Player

### Task 2: Extraer el ciclo de modo de reproducción

**Files:**
- Create: `app/src/main/java/com/example/rewind1137/utils/PlaybackModeCycle.kt`
- Create: `app/src/test/java/com/example/rewind1137/utils/PlaybackModeCycleTest.kt`
- Modify: `app/src/main/java/com/example/rewind1137/viewmodel/PlayerViewModel.kt:454-459`

**Interfaces:**
- Consumes: `com.example.rewind1137.viewmodel.PlaybackMode` (`SEQUENTIAL`, `SHUFFLE`, `REPEAT_ONE`).
- Produces: `object PlaybackModeCycle { fun next(mode: PlaybackMode): PlaybackMode }` con ciclo `SEQUENTIAL → SHUFFLE → REPEAT_ONE → SEQUENTIAL`.

- [ ] **Step 1: Escribir el test que falla**

`PlaybackModeCycleTest` con tres aserciones: `next(SEQUENTIAL) == SHUFFLE`, `next(SHUFFLE) == REPEAT_ONE`, `next(REPEAT_ONE) == SEQUENTIAL`.

- [ ] **Step 2: Verificar que falla**

`& '.\gradlew.bat' testDebugUnitTest --tests "com.example.rewind1137.utils.PlaybackModeCycleTest"`
Expected: FAIL (referencia no resuelta).

- [ ] **Step 3: Implementar `PlaybackModeCycle.next` y usarla en `PlayerViewModel.togglePlaybackMode()`**

Reemplazar el `when` de las líneas 455-459 por `PlaybackModeCycle.next(_uiState.value.playbackMode)`. El resto del método (cola, shuffle, persistencia) queda igual.

- [ ] **Step 4: Verificar que pasa**

Mismo comando del Step 2. Expected: PASS (3 tests). Luego `testDebugUnitTest` completo: los 59 tests existentes siguen pasando.

- [ ] **Step 5: Commit**

```powershell
git -C '<repo>' add app/src/main/java/com/example/rewind1137/utils/PlaybackModeCycle.kt app/src/test/java/com/example/rewind1137/utils/PlaybackModeCycleTest.kt app/src/main/java/com/example/rewind1137/viewmodel/PlayerViewModel.kt
git -C '<repo>' commit -m "refactor: share playback mode cycle logic"
```

### Task 3: Tap en la notificación abre el PlayerSheet

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/MainActivity.kt:56-65,226-229`
- Modify: `app/src/main/java/com/example/rewind1137/ui/navigation/NavGraph.kt:70-90,192-196`
- Modify: `app/src/main/java/com/example/rewind1137/service/MusicService.kt:447-454`

**Interfaces:**
- Consumes: `playerViewModel.setPlayerSheetExpanded(Boolean)` (PlayerViewModel:593).
- Produces: `MainActivity.EXTRA_OPEN_PLAYER_SHEET = "open_player_sheet"` (constante pública); `AppNavGraph(openPlayerSheetRequest: Boolean, onOpenPlayerSheetConsumed: () -> Unit)`.

- [ ] **Step 1: Declarar la constante y el estado one-shot en `MainActivity`**

Añadir `companion object { const val EXTRA_OPEN_PLAYER_SHEET = "open_player_sheet" }` y un campo `private val openPlayerSheetState = mutableStateOf(false)` (ya están importados `mutableStateOf`, `getValue`, `setValue`).

- [ ] **Step 2: Alimentar el estado desde `onCreate` y `onNewIntent`**

En `onCreate`, antes de `setContent`: asignar `intent.getBooleanExtra(EXTRA_OPEN_PLAYER_SHEET, false)`. En `onNewIntent`, tras `setIntent(intent)`: asignar lo mismo. `onNewIntent` ya existe en la línea 226.

- [ ] **Step 3: Consumir el flag en `AppNavGraph`**

Añadir los dos parámetros nuevos a `AppNavGraph` y un `LaunchedEffect(openPlayerSheetRequest)` que, si es `true`, llame a `playerViewModel.setPlayerSheetExpanded(true)` y después a `onOpenPlayerSheetConsumed()`. En `MainActivity` pasar `onOpenPlayerSheetConsumed = { openPlayerSheetState.value = false }` y observar el estado con `val openPlayerSheetRequest by openPlayerSheetState`.

- [ ] **Step 4: Añadir el extra al `contentIntent` de la notificación**

En `MusicService.buildMediaNotification`, al `Intent(this, MainActivity::class.java)` de la línea 450 añadir `.putExtra(MainActivity.EXTRA_OPEN_PLAYER_SHEET, true)`. Mantener `CLEAR_TOP or SINGLE_TOP` y `FLAG_IMMUTABLE`.

- [ ] **Step 5: Compilar, instalar y verificar en dispositivo**

Sync + `assembleDebug` + install. Reproducir música, ocultar la app y tocar el cuerpo de la notificación.
Expected: se abre el PlayerSheet con la canción que suena (no solo la pantalla de biblioteca). Repetir con la app recién iniciada desde cero.

- [ ] **Step 6: Commit y push**

Mensaje: `feat: open player sheet when tapping the media notification`.

### Task 4: Modo sin `MainActivity` y X que limpia el widget

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/service/MusicService.kt:562-563,590-595`
- Modify: `app/src/main/java/com/example/rewind1137/widget/RewindWidgetState.kt` (añadir `clear(context)`)
- Modify: `app/src/main/java/com/example/rewind1137/MainActivity.kt:109-124,141-145`
- Create: `app/src/test/java/com/example/rewind1137/widget/RewindWidgetStateClearTest.kt`

**Interfaces:**
- Consumes: `PlaybackModeCycle.next` (Task 2), `MusicPlayerManager.applyModeFlag(mode)`, `settingsRepository.setPlaybackMode(mode)`.
- Produces: `RewindWidgetState.clear(context: Context)` que reinicia título/artista/álbum/`isPlaying`/`hasSong`/`progressMs`/`durationMs`/`currentSongId`/`isFavorite`/`upNext`/`queueCount`/`songUri`/`albumArtUri`/`albumArt` y escribe `KEY_HAS_SONG=false` en SharedPreferences (no usar `save()`, que hace early-return cuando `!hasSong`).

- [ ] **Step 1: Escribir el test de limpieza**

Con Robolectric (`@Config(sdk = [28])`), llenar `RewindWidgetState` con una canción, llamar `save(context)`, luego `clear(context)`, crear un `Context` nuevo (o borrar el singleton vía reflexión) y llamar `restore(context)`.
Assert: `!hasSong`, `currentSongId == -1L`, `songTitle == "Rewind 1137"`, `upNext.isEmpty()`.

- [ ] **Step 2: Verificar que falla y luego implementar**

`& '.\gradlew.bat' testDebugUnitTest --tests "com.example.rewind1137.widget.RewindWidgetStateClearTest"` → FAIL; implementar `clear(context)` → PASS.

- [ ] **Step 3: Mover `ACTION_TOGGLE_MODE` a `MusicService.handleAction`**

Reemplazar el `sendBroadcast` por: leer `playbackStateHolder.state.value.playbackMode`, calcular `PlaybackModeCycle.next`, llamar `MusicPlayerManager.applyModeFlag(next)`, `playbackStateHolder.update { it.copy(playbackMode = next) }` y `serviceScope.launch { settingsRepository.setPlaybackMode(next) }`. La respuesta ya existente (`onCustomCommand` → `refreshWidgetState()`) se encarga de notificación y widgets.

- [ ] **Step 4: Quitar el receiver dinámico de `MainActivity` y limpiar en `ACTION_CLOSE`**

Eliminar `notifFilter`/`notifReceiver` y su `registerReceiver`/`unregisterReceiver` (líneas 112-124, 143). En `closePlayback()` añadir `RewindWidgetState.clear(this)` antes de `stopSelf()`.

- [ ] **Step 5: Compilar, instalar y verificar en dispositivo**

Con la app en background: pulsar modo en la notificación → el icono cambia; pulsar X → la notificación desaparece y el widget pasa a "Sin reproducción". Tras reiniciar el launcher, el widget sigue sin mostrar la canción muerta.

- [ ] **Step 6: Commit y push**

Mensaje: `fix: handle playback mode and close from the media service`.

---

## Phase 3 — Widgets tipo Lark

### Task 5: Progreso real y tick de 1 segundo

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/service/MusicService.kt:627-640`
- Modify: `app/src/main/java/com/example/rewind1137/widget/RewindWidgetState.kt:85-92`

**Interfaces:**
- Consumes: `MusicPlayerManager.getPlayer(Context): ExoPlayer`, `RewindWidgetRegistry.updateAll(Context)`.
- Produces: `RewindWidgetState.refreshPlayback(player)` sigue siendo la única fuente de progreso; `scheduleWidgetTicker()` lee `player.currentPosition` en cada tick.

- [ ] **Step 1: Reemplazar el progreso sintético**

En `scheduleWidgetTicker()`, obtener `val player = MusicPlayerManager.getPlayer(this)`. En cada iteración: `delay(1000)`, salir si `!player.isPlaying`, y asignar `RewindWidgetState.progressMs = player.currentPosition.coerceAtLeast(0L)` y `durationMs = player.duration.coerceAtLeast(0L)`. Eliminar la suma `progressMs + 2000`.

- [ ] **Step 2: Persistir el progreso real**

Tras actualizar, llamar `RewindWidgetState.save(this)` antes de `RewindWidgetRegistry.updateAll(this)`.

- [ ] **Step 3: Compilar, instalar y verificar en dispositivo**

Reproducir, hacer seek al 70 %, pausar, reanudar.
Expected: la barra del widget salta al 70 % inmediatamente, se congela al pausar y no vuelve a 0 al reanudar. Comprobar en los 3 tamaños.

- [ ] **Step 4: Commit y push**

Mensaje: `fix: drive widget progress from the real player position`.

### Task 6: Restaurar cola en frío y Open → PlayerSheet

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/service/MusicService.kt` (constante + `handleAction` + método `restoreSession()`)
- Modify: `app/src/main/java/com/example/rewind1137/widget/WidgetReceiver.kt:10-54`

**Interfaces:**
- Consumes: `MusicPlayerManager.restore(Context): Boolean`, `currentQueue(): List<Song>`, `currentIndex(): Int`, `MainActivity.EXTRA_OPEN_PLAYER_SHEET` (Task 3), `PlaybackStateHolder.State`.
- Produces: `MusicService.ACTION_RESTORE_SESSION = "ACTION_RESTORE_SESSION"`; `WidgetReceiver.openApp` añade el extra del PlayerSheet.

- [ ] **Step 1: Añadir `ACTION_RESTORE_SESSION` y su manejo**

En `handleAction`, el nuevo action llama a este método privado `restoreSession()`:
```kotlin
private fun restoreSession() {
    if (!MusicPlayerManager.restore(this)) return
    val player = MusicPlayerManager.getPlayer(this)
    val queue = MusicPlayerManager.currentQueue()
    val index = MusicPlayerManager.currentIndex().coerceIn(0, queue.lastIndex)
    val song = queue.getOrNull(index)
    playbackStateHolder.update { state ->
        state.copy(
            filteredSongs = queue, currentSong = song, currentSongId = song?.id,
            currentSongIndex = index, currentPositionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            playbackMode = MusicPlayerManager.currentPlaybackMode()
        )
    }
    refreshWidgetState()
}
```
Si `MusicPlayerManager` no expone `currentPlaybackMode()`, añadir `fun currentPlaybackMode(): PlaybackMode = playbackMode`.

- [ ] **Step 2: Invocar la restauración desde el widget**

En `WidgetReceiver.onReceive`, después de `RewindWidgetState.restore(context)`, si `!RewindWidgetState.hasSong`, lanzar `ACTION_RESTORE_SESSION` como foreground service **antes** de decidir abrir la app; repetir la comprobación de `RewindWidgetState.hasSong` solo después de sincronizar `PlaybackStateHolder` desde el player (usar `MusicPlayerManager.currentQueue().isNotEmpty()` como criterio real).

- [ ] **Step 3: Open abre el PlayerSheet**

En `WidgetReceiver.openApp`, añadir `.putExtra(MainActivity.EXTRA_OPEN_PLAYER_SHEET, true)` al launch intent.

- [ ] **Step 4: Compilar, instalar y verificar en dispositivo**

Matar el proceso (`adb shell am force-stop com.example.rewind1137`), esperar 2 s, pulsar Next en el widget.
Expected: la canción cambia a la siguiente de la cola guardada, el título del widget se actualiza y la notificación reaparece. Pulsar Open → PlayerSheet de esa canción.

- [ ] **Step 5: Commit y push**

Mensaje: `fix: restore playback queue and open player sheet from widgets`.

### Task 7: Pulido visual tipo Lark en los 3 widgets

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/widget/RewindWidgetCompact.kt:54-107`
- Modify: `app/src/main/java/com/example/rewind1137/widget/RewindWidget.kt:65-158`
- Modify: `app/src/main/java/com/example/rewind1137/widget/RewindWidgetLarge.kt:69-243`

**Interfaces:**
- Consumes: `WidgetControls.kt` (todos los botones), `RewindWidgetState` (ya con progreso real).
- Produces: mismos composables privados, sin cambios de firma pública.

- [ ] **Step 1: Compact: añadir Next**

Tras `WidgetPlayPauseButton` (línea 106), añadir `WidgetNextButton(size = buttonSize)` con un `Spacer(GlanceModifier.width(4.dp))`. Si `buttonSize > 38.dp`, reducir `artSize` en 4.dp para que quepa.

- [ ] **Step 2: Mediano: añadir tiempos bajo la barra**

Bajo `WidgetProgress` (líneas 130-134), cuando `hasSong`, añadir un `Row` con `formatWidgetTime(progressMs)` a la izquierda y `formatWidgetTime(durationMs)` a la derecha, `fontSize = 9.sp`, color `R.color.widget_subtext` (mismo patrón que Large:150-166).

- [ ] **Step 3: Grande: reforzar jerarquía**

Cambiar el título "A CONTINUACION" a "A CONTINUACIÓN" y el número de cada fila a `fontWeight = FontWeight.Bold` con `R.color.widget_accent` (ya está; solo verificar que no se corte con `maxUpNext`). No cambiar tamaños de arte ni caché.

- [ ] **Step 4: Compilar, instalar y verificar en dispositivo con captura**

Añadir los 3 tamaños a la home. Expected: sin texto cortado ni botones fuera del área en 1×1, 4×2 y 4×4; el progreso y los tiempos coinciden con la app.

- [ ] **Step 5: Commit y push**

Mensaje: `polish: Lark-style layout across the three player widgets`.

---

## Phase 4 — Audio HiFi

### Task 8: Política HiFi pura

**Files:**
- Create: `app/src/main/java/com/example/rewind1137/audio/HifiAudioPolicy.kt`
- Create: `app/src/test/java/com/example/rewind1137/audio/HifiAudioPolicyTest.kt`

**Interfaces:**
- Consumes: `UserSettings` (`equalizerEnabled`, `bassBoostEnabled`, `virtualizerEnabled`, `replayGainEnabled`, `replayGainPreamp`, `crossfadeSeconds`), `VocalRemovalState.enabled`.
- Produces:
```kotlin
object HifiAudioPolicy {
    data class Effective(
        val equalizer: Boolean, val bassBoost: Boolean, val virtualizer: Boolean,
        val vocalRemoval: Boolean, val replayGain: Boolean, val floatOutput: Boolean
    )
    fun resolve(settings: UserSettings, vocalRemovalEnabled: Boolean): Effective
}
```
Regla: si `settings.hifiModeEnabled` → todos los DSP a `false`, `replayGain = true`, `floatOutput = true`; si no → devolver los valores de `settings` y `floatOutput = false`.

- [ ] **Step 1: Escribir tests que fallan**

`HifiAudioPolicyTest`: (a) con HiFi activo y todos los DSP activos, `Effective` tiene todo a `false` salvo `replayGain`/`floatOutput` en `true`; (b) con HiFi apagado, se respetan los valores de `UserSettings` y `floatOutput == false`; (c) `vocalRemovalEnabled` se respeta solo cuando HiFi está apagado.

- [ ] **Step 2: Verificar fallo, implementar, verificar PASS**

`& '.\gradlew.bat' testDebugUnitTest --tests "com.example.rewind1137.audio.HifiAudioPolicyTest"`

- [ ] **Step 3: Commit**

Mensaje: `test: define transparent HiFi audio policy`.

### Task 9: Persistencia y UI del switch HiFi

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/data/model/UserSettings.kt:6-23`
- Modify: `app/src/main/java/com/example/rewind1137/data/repository/SettingsRepository.kt:55-110,200-216,430-475`
- Modify: `app/src/main/java/com/example/rewind1137/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/example/rewind1137/ui/navigation/NavGraph.kt:365-404`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `HifiAudioPolicy.Effective` (Task 8).
- Produces: `UserSettings.hifiModeEnabled: Boolean = false`; `SettingsRepository.setHifiMode(enabled: Boolean)`; `SettingsScreen(onHifiModeChange: (Boolean) -> Unit)`; `SettingsViewModel.setHifiMode(Boolean)`.

- [ ] **Step 1: Añadir el campo a `UserSettings` y al DataStore**

`hifiModeEnabled: Boolean = false` al final del data class. En `SettingsRepository`: nuevo `PreferencesKeys.HIFI_MODE_ENABLED`, lectura en `userSettingsFlow`, `setHifiMode(enabled)`, e inclusión en `exportBackup`/`importBackup` (junto a `replayGainEnabled` en las líneas 433 y 472).

- [ ] **Step 2: Añadir el switch en `SettingsScreen`**

Un `Switch` etiquetado "Modo HiFi (sin procesar)" con subtítulo: "Desactiva ecualizador, graves, virtualizador y eliminación de voz; activa ReplayGain. No iguala los másteres de Tidal/Qobuz." Cablear `onHifiModeChange` desde `NavGraph` → `settingsViewModel.setHifiMode`.

- [ ] **Step 3: Verificar persistencia**

`assembleDebug`, instalar, activar el switch, force-stop, reabrir.
Expected: el switch sigue activo; el valor exportado en el backup contiene `hifiModeEnabled`.

- [ ] **Step 4: Commit y push**

Mensaje: `feat: persist HiFi mode setting`.

### Task 10: Cablear ReplayGain, DSP y salida float

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/audio/VocalRemovalRenderersFactory.kt`
- Modify: `app/src/main/java/com/example/rewind1137/utils/MusicPlayerManager.kt:71-89,124-146,315-336`
- Modify: `app/src/main/java/com/example/rewind1137/service/MusicService.kt:192-207`
- Modify: `app/src/main/java/com/example/rewind1137/viewmodel/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `HifiAudioPolicy.resolve` (Task 8), `settingsRepository.userSettingsFlow`, `MusicPlayerManager.applyReplayGainFor(Context, Uri)`, `AudioEffectManager.setEqualizerEnabled/setBassBoostEnabled/setVirtualizerEnabled`.
- Produces: `MusicPlayerManager.setHifiMode(enabled: Boolean, settings: UserSettings)` que aplica la política y reconfigura el player; `MusicPlayerManager.setFloatOutput(enabled: Boolean)` con fallback.

- [ ] **Step 1: Habilitar salida float de forma segura**

En `VocalRemovalRenderersFactory`, añadir un constructor/flag `enableFloatOutput` y pasarlo a `super`. En `MusicPlayerManager.setFloatOutput`, reconstruir el `ExoPlayer` con el flag dentro de `runCatching`; si falla, reintentar con `false` y no propagar la excepción.

- [ ] **Step 2: Cablear ReplayGain en la transición de pista**

En el `Player.Listener` de `MusicService.onCreate`, `onMediaItemTransition` → además de `updateNotification()`/`refreshWidgetState()`, llamar `MusicPlayerManager.applyReplayGainFor(this@MusicService, mediaItem?.localConfiguration?.uri ?: return)`. Llamar también al arrancar el servicio y justo después de `restoreSession()` (Task 6).

- [ ] **Step 3: Aplicar la política al cambiar el switch**

`SettingsViewModel.setHifiMode` guarda el valor y llama a `MusicPlayerManager.setHifiMode(...)` con el `UserSettings` resultante. En HiFi: desactivar EQ/BassBoost/Virtualizer/VocalRemoval; al desactivar HiFi: restaurar los valores guardados de `settings`. El crossfade del usuario se respeta pero se documenta en el diálogo.

- [ ] **Step 4: Compilar y verificar que no hay regresión**

`assembleDebug` + `testDebugUnitTest` completo + install. Expected: BUILD SUCCESSFUL, 59+ tests en verde, sin crash al alternar HiFi 5 veces seguidas.

- [ ] **Step 5: Verificar en dispositivo**

Reproducir una pista con tag ReplayGain, activar HiFi → el volumen cambia al esperado; desactivar → vuelve al volumen base. Reiniciar la app → el modo sigue aplicado. `crash_log.txt` no contiene nuevas entradas.

- [ ] **Step 6: Commit y push**

Mensaje: `feat: wire ReplayGain, transparent DSP and float output for HiFi mode`.

### Task 11: Diálogo de información de formato honesto

**Files:**
- Modify: `app/src/main/java/com/example/rewind1137/ui/navigation/NavGraph.kt:338,525` (`onAudioInfoClick` es un no-op en ambas)
- Create: `app/src/main/java/com/example/rewind1137/ui/components/AudioInfoDialog.kt`
- Modify: `app/src/main/java/com/example/rewind1137/utils/AudioFormatReader.kt` (crear si no existe)

**Interfaces:**
- Consumes: `Song` (id, uri, mimeType), `MediaStore` / `MediaFormat`.
- Produces: `AudioFormatReader.read(context: Context, song: Song): AudioFormatInfo` con `mime`, `sampleRateHz`, `channelCount`, `bitrateKbps`; `AudioInfoDialog(info: AudioFormatInfo, onDismiss: () -> Unit)`.

- [ ] **Step 1: Implementar el lector de formato real**

Leer de `MediaStore.Audio.Media` columns `MIME_TYPE`, `SAMPLERATE`, `CHANNELS`, `BITRATE` cuando estén disponibles; valores ausentes → "—". Nunca inventar bit depth ni etiqueta Hi-Res.

- [ ] **Step 2: Crear el diálogo y cablearlo**

`AudioInfoDialog` muestra los cuatro campos y una nota: "Tidal y Qobuz usan másteres propios con DRM; este reproductor trabaja con tus archivos locales." Sustituir ambos `onAudioInfoClick = { /* ... */ }` por `showAudioInfoDialog = true` + render del diálogo.

- [ ] **Step 3: Compilar, instalar y verificar en dispositivo**

Abrir PlayerSheet → info. Expected: el MIME y el sample rate coinciden con los del archivo; no aparece ninguna etiqueta falsa de alta resolución.

- [ ] **Step 4: Commit y push**

Mensaje: `feat: show honest local audio format info in the player sheet`.

---

## Self-review checklist (ejecutor)

- [ ] Cada uno de los 4 bloques del spec tiene al menos una tarea.
- [ ] Los 5 ítems de Review Focus tienen test o verificación en dispositivo asignados.
- [ ] `EXTRA_OPEN_PLAYER_SHEET`, `ACTION_RESTORE_SESSION`, `hifiModeEnabled` y `PlaybackModeCycle.next` mantienen el mismo nombre en todas las tareas.
- [ ] Ninguna tarea modifica `Song.toMediaItem()`, la caché de 288 px ni el glow de 256 px.
- [ ] Todo ajuste configurable se persiste en DataStore o SharedPreferences.
- [ ] Cada fase tiene commit, push y APK verificado en el dispositivo antes de pasar a la siguiente.
