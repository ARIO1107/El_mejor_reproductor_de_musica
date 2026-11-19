# Design: Icono, Widgets, Notificación Player y Modo HiFi

Date: 2026-11-19
Chosen approach: A — entrega por fases (icono → notificación → widgets → audio)
Repositorio: `RewindGitRepo2` (sync con `C:\Users\Usuario\AndroidStudioProjects\RePro_backup_2026-10-13`)
Dispositivo: Infinix HOT 60 Pro+ por USB (autorizado explícitamente por el usuario en esta sesión)

## Intent

Cumplir los cuatro bloques pedidos por el usuario sin romper lo ya arreglado
(portadas repetidas, scroll fluido de 5 columnas): cambiar el icono por
`image_a47006f8.jpg`, dejar los 3 widgets y la notificación totalmente
funcionales (corazón, X, tap → PlayerSheet, iluminación dinámica) y acercar la
fidelidad de audio con un modo HiFi **honesto**.

## What exists today (hallazgos verificados)

### Icono
- Imagen pedida: `C:\Users\Usuario\Downloads\image_a47006f8.jpg` (500.863 bytes).
- Manifest: `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`.
- Existen mipmaps `mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi` + `mipmap-anydpi-v26/ic_launcher*.xml`
  con `drawable/ic_launcher_foreground.png` y `drawable/ic_launcher_background.xml`.

### Notificación (`service/MusicService.kt`)
- ✅ Corazón (`ACTION_TOGGLE_FAVORITE`) y X (`ACTION_CLOSE`) ya presentes en compacta y expandida
  (líneas 343-414) y como `CommandButton` del MediaSession (líneas 152-166).
- ✅ Iluminación dinámica ya implementada: `NotificationGlowUtils.build(shownArt)` a 256 px
  (líneas 291, 324-341). NO hace falta agrandarla.
- ❌ `contentIntent` (447-454) solo lanza `MainActivity` con `CLEAR_TOP or SINGLE_TOP`, **sin extra**;
  `MainActivity.onNewIntent` (226-229) solo hace `setIntent(intent)`. No abre el PlayerSheet.
- ❌ `ACTION_TOGGLE_MODE` (562-563) hace `sendBroadcast("ACTION_TOGGLE_MODE")`; solo lo escucha un
  receiver dinámico dentro de `MainActivity` (109-124). Con la app en background no hace nada.
- ⚠️ `ACTION_CLOSE` (590-595) pausa, `stopForeground` y `stopSelf`, pero no limpia sesión/cola
  persistentes; puede dejar el widget mostrando una canción muerta.

### Widgets (`widget/` + `AndroidManifest.xml` 58-102)
- 3 providers Glance: `RewindWidgetReceiver` (mediano), `RewindWidgetCompactReceiver`,
  `RewindWidgetLargeReceiver`. Acciones prev/play/next/like/mode/open cableadas en
  `WidgetControls.kt` → `WidgetReceiver.kt` → `MusicService`.
- ❌ Progreso sintético: `scheduleWidgetTicker()` (627-640) suma `+2000` ms cada 2 s en vez de leer
  `player.currentPosition`. Se desincroniza con seek/crossfade/pausa.
- ❌ `ACTION_TOGGLE_MODE` depende de `MainActivity` viva (mismo defecto que la notificación).
- ❌ `MusicPlayerManager.restore(context)` (124-146) existe pero **nunca se llama** tras muerte de
  proceso; el widget tiene estado en disco (`RewindWidgetState.save/restore`) pero la cola real no
  se reconstruye, así que next/prev no funcionan con la app fría.
- ❌ Botón Open (`openApp`, WidgetReceiver 47-54) abre `MainActivity` pero no el PlayerSheet.
- ⚠️ Estética tipo Lark ya es base razonable; falta pulido/consistencia, no reescritura.

### Audio (`utils/MusicPlayerManager.kt`, `audio/`, `data/`)
- `ReplayGainReader` + `applyReplayGainFor(context, uri)` (324-336) existen pero **nunca se invocan**.
- `AudioFocusManager` es código muerto (ExoPlayer ya gestiona el foco con `setAudioAttributes(..., true)`).
- `VocalRemovalRenderersFactory` usa flags por defecto: **`enableFloatOutput=false`**, sin salida float.
- `equalizerEnabled = true` por defecto en `UserSettings.kt:18` → DSP colorido activo desde el inicio.
- No hay `hifiMode` en UI, DataStore, strings ni backup/import.
- Tidal/Qobuz usan másteres propios, DRM/MQA y catálogo hi-res; un reproductor local **no** puede
  igualarlos ni convertir MP3/AAC en lossless.

## Goals / Non-goals

- Goals: icono aplicado y visible; tap en notificación abre el PlayerSheet de la canción actual;
  corazón/X funcionando con app en foreground y background; glow dinámico conservado; 3 widgets con
  progreso real, cola restaurada y modos/like/next/prev/open funcionando en frío; switch HiFi
  persistente que desactive DSP colorido, active ReplayGain y salida float segura; diálogo de info
  de formato honesto.
- Non-goals: igualar Tidal/Qobuz, DRM/MQA, conversión lossy→lossless, bit-perfect garantizado,
  rediseñar la arquitectura de reproducción.

## Decisiones por fase

### Fase 1 — Icono
1. Generar mipmaps desde la JPG con `magick`/Pillow en todos los densities.
2. Reemplazar `mipmap-*/ic_launcher.png`, `ic_launcher_round.png` y
   `drawable/ic_launcher_foreground.png`; ajustar background si la imagen lo requiere.
3. Instalar y verificar en el launcher del dispositivo.

### Fase 2 — Notificación
1. `contentIntent` con `Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_PLAYER_SHEET, true)`
   + `CLEAR_TOP or SINGLE_TOP`.
2. `MainActivity`: leer el extra en `onCreate` y `onNewIntent`; exponer un flag *one-shot* a
   `AppNavGraph`; un `LaunchedEffect` llama a `playerViewModel.setPlayerSheetExpanded(true)` y limpia
   el flag. (Ambos `hiltViewModel()` comparten el mismo ViewModel activity-scoped.)
3. Mover `ACTION_TOGGLE_MODE` a `MusicService.handleAction`: usa `playbackStateHolder` +
   `MusicPlayerManager.applyModeFlag(...)`, persiste con `settingsRepository.setPlaybackMode(...)`
   y actualiza notificación/widgets. Eliminar el receiver dinámico de `MainActivity`.
4. `ACTION_CLOSE`: además de pausar, limpiar `RewindWidgetState` (hasSong=false) y persistir para que
   el widget no muestre una canción muerta. No liberar el MediaSession si el servicio sigue vivo.
5. Glow se mantiene a 256 px.

### Fase 3 — Widgets (primero funcionales, luego tipo Lark)
1. `scheduleWidgetTicker`: reemplazar `progressMs + 2000` por
   `MusicPlayerManager.getPlayer(ctx).currentPosition`; intervalo 1 s; actualizar
   `RewindWidgetState.progressMs` y Guardar. Usar posición real en pausa/seek/crossfade.
2. `WidgetReceiver`: si no hay player o no hay cola, invocar un `ACTION_RESTORE_SESSION` en
   `MusicService` que llame a `MusicPlayerManager.restore(context)` y sincronice
   `PlaybackStateHolder` (modo, canción, favorito, progreso) antes de ejecutar la acción.
3. `ACTION_TOGGLE_MODE` pasa a manejarse 100 % en `MusicService` (Fase 2).
4. Botón Open reutiliza `EXTRA_OPEN_PLAYER_SHEET` para abrir directamente el PlayerSheet.
5. Pulido tipo Lark sobre los 3 tamaños: arte protagonista, título/artista con buen contraste,
   progreso real, controles con estado activo y "up next"; reutilizar el color dominante ya calculado.
   Sin cambiar el tamaño de caché de arte (512 px).

### Fase 4 — Audio HiFi (transparente y honesto)
1. `hifiModeEnabled: Boolean = false` en `UserSettings`, `SettingsRepository` (PreferencesKeys,
   flow, setter, export/import backup) y switch en `SettingsScreen` con texto honesto.
2. Al activar HiFi: desactivar EQ/BassBoost/Virtualizer/VocalRemoval (guardando valores previos para
   restaurarlos al desactivar); llamar `applyReplayGainFor` en `onMediaItemTransition`, al arrancar y
   al restaurar sesión; activar `enableFloatOutput=true` en el RenderersFactory dentro de try/catch
   con fallback a PCM 16/24-bit.
3. Respetar el crossfade configurado por el usuario pero documentar que no es bit-perfect.
4. `AudioInfoDialog`: mostrar MIME, sample rate, canales y bitrate reales desde MediaStore/MediaFormat;
   etiqueta explícita de que Tidal/Qobuz usan másteres propios/DRM.
5. Persistir todo en DataStore; ningún ajuste se resetea al reiniciar.

## Verificación por fase

- `assembleDebug` → `adb install -r` en el dispositivo → prueba manual:
  - F1: icono en launcher y en ajustes de la app.
  - F2: tap cuerpo notificación → PlayerSheet; corazón cambia; X cierra; glow sigue reaccionando.
  - F3: widget en segundo plano y tras matar proceso → next/prev/like/mode/open; progreso coincide
    con la app; scroll y RAM sin regresiones.
  - F4: switch HiFi persiste tras reinicio; ReplayGain se aplica; info muestra formato real.
- Commit atómico por fase + push a `main`, reportando hash y ruta del APK.

## Riesgos y mitigaciones

- `enableFloatOutput` no soportado en algún DAC → try/catch + fallback.
- El extra one-shot podría disparar dos veces (onCreate + onNewIntent) → consumirlo una sola vez.
- Restaurar sesión desde widget necesita permiso de lectura ya concedido → la app lo pide al arrancar;
  si falta, abrir la app en vez de fallar silenciosamente.
- No tocar la lógica de caché de miniaturas ni `Song.toMediaItem()` para no revivir el bug de portadas.
