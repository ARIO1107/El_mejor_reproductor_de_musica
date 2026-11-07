# Performance & Battery Optimization Guide — Re-Pro Music Player

---

## Table of Contents

1. [Battery Optimization Analysis](#1-battery-optimization-analysis)
2. [Known Battery Drains](#2-known-battery-drains)
3. [Recommendations](#3-recommendations)
4. [Profiling Steps](#4-profiling-steps)
5. [Memory Leak Prevention](#5-memory-leak-prevention)

---

## 1. Battery Optimization Analysis

### Current Architecture

| Component | Implementation | Battery Impact |
|-----------|---------------|----------------|
| **ExoPlayer** | Media3 `ExoPlayer` via `MusicPlayerManager` singleton | Low (hardware-accelerated decoding) |
| **MediaSessionService** | `MusicService` extends `MediaSessionService` | Medium (foreground service w/ notification) |
| **Position Polling** | Coroutine `while(true)` loop with `delay(500)` in `MusicViewModel.startPositionTracker()` | **High** — see §2 |
| **DynamicBar Overlay** | `DynamicBarService` with `ComposeView` overlay | Medium — continuous recomposition |
| **Audio Effects** | `AudioEffectManager` (Equalizer, BassBoost, Virtualizer) | Low (DSP hardware) |

### Foreground Service Requirements

`MusicService` is declared with `android:foregroundServiceType="mediaPlayback"` and the manifest includes:
- `FOREGROUND_SERVICE` permission
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission
- `POST_NOTIFICATIONS` permission

This is **required** for background playback on Android 9+. The `MediaSessionService` base class handles the foreground notification lifecycle automatically. Battery impact is inherent to any music app with background playback.

### Audio Focus Handling

`MusicPlayerManager` configures `setHandleAudioBecomingNoisy(true)`, which pauses playback when headphones are disconnected. This is good practice — it prevents silent playback that wastes battery.

No explicit `AudioFocusRequest` handling was found beyond what `ExoPlayer` provides internally. This is acceptable since ExoPlayer manages audio focus when `setHandleAudioBecomingNoisy` is enabled.

### Position Polling Interval

The current polling interval is **500ms** (`delay(500)` in `MusicViewModel.startPositionTracker()` at line 219). For a music player with a seekbar, 500ms is reasonable but has measurable impact over long sessions:

- **1,200 state updates/hour** × ~200 bytes per `StateFlow` emission = ~240 KB/hour of heap allocations
- The coroutine stays active even when paused (only updates position conditionally)
- Each emission triggers Compose recomposition across all collectors (`DynamicBarOverlay`, player screen, notification)

---

## 2. Known Battery Drains

### 2.1 startPositionTracker Polling (HIGH)

**File:** `MusicViewModel.kt:219-229`

```kotlin
private fun startPositionTracker() {
    viewModelScope.launch {
        while (true) {
            if (_uiState.value.isPlaying) {
                val pos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
            delay(500)
        }
    }
}
```

**Issues:**
- Runs unconditionally even when paused (wakes every 500ms to check `isPlaying`)
- Coroutine coroutine scope tied to `viewModelScope` — destroyed only when ViewModel is cleared
- Each emission triggers recomposition in all Compose collectors
- `delay(500)` is not exact — actual interval varies with system load

**Optimization:** Use ExoPlayer's built-in `createMessage` API or `Player.Listener` callbacks for position updates, eliminating the polling loop entirely.

### 2.2 DynamicBarService Overlay Window (MEDIUM)

**File:** `DynamicBarService.kt`

- `ComposeView` used for an overlay window — full Compose recomposition pipeline
- Collects `MusicViewModel.globalUiState` which updates every 500ms
- Overlay stays visible as long as the service is running
- No throttling on recomposition — every state change triggers full recompose

**Impact:** Each position update causes the entire `DynamicBarOverlay` composable to recompose, including gradient recalculation and animation interpolation.

### 2.3 Audio Visualizer (MEDIUM)

If an `AudioVisualizer` component exists (referenced in analysis context):
- Continuous waveform rendering via `Visualizer` API
- `setCaptureSize()` and `setDataCaptureListener()` with high rates drain battery
- Background rendering when not visible is wasteful

### 2.4 Notification Updates (LOW-MEDIUM)

**File:** `MusicService.kt:76-130`

- Custom `RemoteViews` notification includes progress bar and album art
- `loadAlbumArt()` decodes a `Bitmap` on every notification rebuild
- Notification is rebuilt when `MusicViewModel.globalUiState` changes
- Album art decoding uses `ImageDecoder` (API 28+) or deprecated `MediaStore.Images.Media.getBitmap`

### 2.5 Singleton ExoPlayer Lifecycle (LOW)

**File:** `MusicPlayerManager.kt`

- `ExoPlayer` is held as a singleton — never released unless `releasePlayer()` is called
- This is intentional (shared player), but `releasePlayer()` is never called from the ViewModel or service lifecycle
- ExoPlayer holds native resources (codecs, audio track) that consume battery when initialized

---

## 3. Recommendations

### 3.1 Replace Coroutine Polling with ExoPlayer Position Tracking

**Priority: HIGH**

Replace the `while(true)` + `delay(500)` pattern with ExoPlayer's event-based position tracking:

```kotlin
// Option A: Use ExoPlayer's createMessage API
private fun startPositionTracker() {
    val player = exoPlayer ?: return
    val message = player.createMessage { _, _ ->
        val pos = player.currentPosition.coerceAtLeast(0L)
        _uiState.update { it.copy(currentPositionMs = pos) }
    }.apply {
        setPeriodIndex(0, 0) // current period
        setPositionMs(0)
        // Repeat every 500ms
    }
    // Alternatively, use a Handler:
    val handler = Handler(Looper.getMainLooper())
    val updater = object : Runnable {
        override fun run() {
            if (_uiState.value.isPlaying) {
                val pos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
            handler.postDelayed(this, 500)
        }
    }
    handler.post(updater)
}
```

**Option B (Recommended): Handler + postDelayed**

```kotlin
private val positionHandler = Handler(Looper.getMainLooper())
private val positionUpdater = object : Runnable {
    override fun run() {
        if (_uiState.value.isPlaying) {
            val pos = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
            _uiState.update { it.copy(currentPositionMs = pos) }
        }
        positionHandler.postDelayed(this, 500)
    }
}

fun startPositionTracker() {
    positionHandler.post(positionUpdater)
}

fun stopPositionTracker() {
    positionHandler.removeCallbacks(positionUpdater)
}
```

**Why:** `Handler.postDelayed` avoids coroutine overhead, doesn't allocate a coroutine per tick, and can be cancelled cleanly. The coroutine-based loop allocates a `Job` and `Delay` continuation on every iteration.

### 3.2 Use ExoPlayer.Listener for State Changes

**Priority: HIGH**

Instead of polling `isPlaying`, register a `Player.Listener`:

```kotlin
exoPlayer?.addListener(object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) startPositionTracker() else stopPositionTracker()
    }
    override fun onPlaybackStateChanged(state: Int) {
        // Handle STATE_ENDED, STATE_READY, etc.
    }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Update current song index
    }
})
```

This eliminates the need to poll `isPlaying` state.

### 3.3 Throttle DynamicBar Recomposition

**Priority: MEDIUM**

The `DynamicBarOverlay` composable should use `derivedStateOf` or `snapshotFlow` to throttle updates:

```kotlin
@Composable
fun DynamicBarOverlay(...) {
    // Only recompose when position crosses a meaningful threshold (e.g., every second)
    val displayPosition by remember {
        derivedStateOf { (currentPositionMs / 1000) * 1000 }
    }
    // Use displayPosition instead of currentPositionMs for the progress bar
}
```

Alternatively, debounce position updates in the ViewModel before emitting to the overlay:

```kotlin
private val throttledPosition = _uiState
    .map { it.currentPositionMs / 1000 * 1000 }
    .distinctUntilChanged()
```

### 3.4 Lazy Album Art Loading

**Priority: MEDIUM**

The notification `loadAlbumArt()` function decodes a full bitmap on the main thread:

```kotlin
// Current (blocking main thread):
private fun loadAlbumArt(albumArtUri: android.net.Uri?): Bitmap? { ... }

// Recommended: Cache and decode off-main-thread
private var cachedAlbumArtUri: Uri? = null
private var cachedBitmap: Bitmap? = null

private fun loadAlbumArt(albumArtUri: Uri?): Bitmap? {
    if (albumArtUri == cachedAlbumArtUri) return cachedBitmap
    cachedBitmap = decodeInParallel(albumArtUri) // Use Coil or background thread
    cachedAlbumArtUri = albumArtUri
    return cachedBitmap
}
```

Since the project uses **Coil** (`io.coil-kt:coil-compose:2.7.0`), consider loading album art via Coil's async pipeline and caching the result.

### 3.5 Wake Lock Management

**Priority: LOW**

`MusicService` does not currently request a `WakeLock`. For background playback:
- `MediaSessionService` handles the foreground service notification, which prevents process killing
- A `PARTIAL_WAKE_LOCK` is only needed if the app must survive Doze mode
- Since `MediaSessionService` already promotes the process, a wake lock is unnecessary for normal playback
- **Recommendation:** Do not add a `WAKE_LOCK` unless there is a specific need (e.g., playback after screen lock with network streaming)

### 3.6 WorkManager for Background Sync

**Priority: LOW**

If the app performs any background data operations (e.g., syncing playlists, fetching metadata):
- Use `WorkManager` with `PeriodicWorkRequest` instead of manual coroutines
- `WorkManager` respects Doze mode and battery optimizations
- Batch operations to minimize wake-ups

### 3.7 Notification Channel Optimization

Already implemented correctly:
- Channel uses `IMPORTANCE_LOW` (no sound/vibration)
- `setShowBadge(false)` prevents notification dot

---

## 4. Profiling Steps

### 4.1 Android Studio Battery Profiler

1. **Connect device** via USB with developer options enabled
2. Open **View > Tool Windows > Profiler** (or bottom toolbar)
3. Click **Battery** in the profiler timeline
4. Reproduce the scenario:
   - Start playback → let it play for 10 minutes
   - Pause playback → let it idle for 10 minutes
   - Switch songs, scrub seekbar, open DynamicBar
5. Analyze:
   - **CPU wake locks** — look for long orange bars
   - **Network activity** — should be minimal for local playback
   - **GPS/sensors** — should be absent
   - **Alarm Manager** — should be minimal
6. Export the trace for comparison across builds

### 4.2 dumpsys batterystats

```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use the app for 10-15 minutes

# Get full report
adb shell dumpsys batterystats > battery_report.txt

# Get charge level info
adb shell dumpsys battery
```

**Key metrics in the report:**
- `Estimated power use (mAh)` — app-specific drain
- `Wake lock` entries — identify held locks
- `Job` entries — scheduled work
- `Sensor` entries — active sensors
- `Wifi/Bluetooth` — radio usage

### 4.3 Real-Time Monitoring

```bash
# Start real-time monitoring
adb shell dumpsys batterystats --charged

# Watch battery level changes
adb shell dumpsys battery

# Monitor CPU usage per process
adb shell top -d 1 | grep -i "re_pro"

# Watch wake locks in real-time
adb shell dumpsys power | grep -i "wake lock"

# Monitor audio focus
adb shell dumpsys audio | grep -i "focus"
```

### 4.4 Baseline Metrics to Track

| Metric | Target | How to Measure |
|--------|--------|----------------|
| **mAh/hour during playback** | < 5% battery/hour | Battery Profiler over 1 hour |
| **mAh/hour idle (no playback)** | < 0.5% battery/hour | Battery Profiler over 1 hour |
| **CPU usage during playback** | < 3% average | `top` command or Profiler |
| **CPU usage when paused** | < 0.5% average | `top` command or Profiler |
| **Memory usage (PSS)** | < 150 MB | Android Studio Memory Profiler |
| **Wake lock held duration** | < 90% of playback time | `dumpsys batterystats` |
| **GC frequency** | < 2 per minute | Logcat GC messages |

### 4.5 Memory Profiling

```
1. Open Memory Profiler in Android Studio
2. Take heap dump after:
   - App launch (baseline)
   - 10 minutes of playback
   - After closing DynamicBar
   - After stopping service
3. Look for:
   - Bitmap accumulation (album art)
   - Compose recomposition scope leaks
   - ExoPlayer native memory (not in Java heap)
```

---

## 5. Memory Leak Prevention

### 5.1 Service Lifecycle Management

**MusicService:**
- `mediaSession?.release()` is called in `onDestroy()` ✓
- `stopSelf()` is called on close action ✓
- Service is bound via `MediaSessionService` intent filter ✓

**DynamicBarService:**
- `overlayView` is removed from `WindowManager` in `onDestroy()` ✓
- `serviceViewModelStore.clear()` is called in `onDestroy()` ✓
- Service uses `LifecycleService` with proper `ViewModelStoreOwner` ✓

**Potential issue:** If `DynamicBarService` is destroyed while Compose is still recomposing, the `ComposeView` may reference a dead lifecycle. Add a `try-catch` around the overlay removal (already present).

### 5.2 ViewModel Scope vs Service Scope

| Scope | Lifetime | Use Case |
|-------|----------|----------|
| `viewModelScope` | Tied to Activity/Fragment lifecycle | UI state, position tracking |
| `MusicPlayerManager` (singleton) | Process lifetime | ExoPlayer instance |
| `DynamicBarService.viewModelStore` | Service lifetime | Overlay composables |

**Risk:** The `startPositionTracker` coroutine lives in `viewModelScope`. When the Activity is destroyed (e.g., app backgrounded), the coroutine stops. However, if a new Activity is created, `startPositionTracker()` is called again (line 108 in `MusicViewModel`), which is correct.

**No leak detected** — the coroutine scope is properly bounded.

### 5.3 ExoPlayer Release Patterns

`MusicPlayerManager.releasePlayer()` calls `exoPlayer?.release()` and sets it to `null`. However:

- `releasePlayer()` is **never called** from any lifecycle callback
- `MusicService.onDestroy()` releases the `MediaSession` but not the player
- The singleton pattern means ExoPlayer persists for the process lifetime

**Recommendation:** This is acceptable for a music player. ExoPlayer's native resources are freed when the process dies. For stricter cleanup:

```kotlin
// In MusicService.onDestroy():
override fun onDestroy() {
    mediaSession?.run {
        release()
        mediaSession = null
    }
    MusicPlayerManager.releasePlayer() // Optional: release when service stops
    super.onDestroy()
}
```

**Trade-off:** Releasing ExoPlayer means re-initializing it on next play, which adds ~200ms latency. For a music app, keeping the player alive is usually preferred.

### 5.4 Image Loading (Coil) Cache Management

The project uses `io.coil-kt:coil-compose:2.7.0`. Coil manages its own disk and memory cache.

**Current usage:**
- Album art in the notification is decoded via `ImageDecoder` (not Coil)
- Compose UI likely uses Coil for image loading

**Recommendations:**
- Ensure Coil is configured with `MemoryCache` limits appropriate for the app
- Use `rememberAsyncImagePainter` with `size()` to avoid decoding full-resolution images
- Call `ImageLoader.memoryCache?.clear()` on low-memory conditions if needed
- Use `DiskCache.Builder().maxSizePercent(0.02)` to limit disk cache to 2% of storage

### 5.5 AudioEffectManager Cleanup

`AudioEffectManager.release()` properly releases all audio effects:
- `equalizer?.release()`
- `bassBoost?.release()`
- `virtualizer?.release()`

All references are set to `null` after release. This is correct.

**Verify:** Ensure `AudioEffectManager.release()` is called when the player session ends or the service is destroyed. Currently, it's unclear who calls `release()`. If it's only called from the ViewModel, the effects may persist after the ViewModel is cleared.

### 5.6 ComposeView in Overlay

`DynamicBarService` creates a `ComposeView` that registers itself with the service's lifecycle and ViewModel store:

```kotlin
overlayView = ComposeView(this).apply {
    setViewTreeLifecycleOwner(this@DynamicBarService)
    setViewTreeViewModelStoreOwner(this@DynamicBarService)
    setContent { ... }
}
```

This is the correct pattern for overlay ComposeViews. The `ComposeView` will be cleaned up when the service is destroyed, and the `ViewModelStore` is explicitly cleared in `onDestroy()`.

**No memory leak detected** for this pattern.

---

## Summary of Action Items

| Priority | Action | Effort | Impact |
|----------|--------|--------|--------|
| **HIGH** | Replace coroutine polling with Handler + postDelayed | Low | High |
| **HIGH** | Add `Player.Listener` for state change events | Low | High |
| **MEDIUM** | Throttle DynamicBar recomposition with `derivedStateOf` | Low | Medium |
| **MEDIUM** | Cache album art / decode off-main-thread | Medium | Medium |
| **LOW** | Call `releasePlayer()` in service lifecycle (optional) | Low | Low |
| **LOW** | Add `WorkManager` for any future background sync | Low | Low |
| **INFO** | Document baseline battery metrics for regression testing | Low | N/A |

---

*Last updated: Generated for Re-Pro Music Player project*
