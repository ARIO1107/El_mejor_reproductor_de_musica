# Design: Stop Position Tracker Loop in PlayerViewModel

Date: 2026-11-03
Topic: Fix infinite position-polling coroutine

## Intent

Stop the playback position tracker in `PlayerViewModel` from running an
infinite `while(true)` loop with `delay(100)` even while paused, and stop
writing duplicated state.

## What exists today

- `app/src/main/java/com/example/re_pro/viewmodel/PlayerViewModel.kt` lines ~177-188:
  `viewModelScope.launch { while(true) { if (isPlaying) { ...update position... }; delay(100) } }`
  Writes to both `_uiState` and `PlaybackStateHolder` (duplicate source of truth).

## Approaches considered

1. **Stop tracker when paused** (minimal change): cancel the loop / scope when
   not playing, restart when playing.
2. **Replace polling with ExoPlayer `Player.Listener`** (recommended): use
   `onIsPlayingChanged` / position updates via listener — eliminates the polling
   loop entirely, precise updates, zero battery cost when paused.
3. **Both**: listener-driven updates + keep loop as fallback.

## Chosen approach

Option 2 — replace polling with `Player.Listener`-based updates. Rationale: the
`PERFORMANCE_GUIDE.md` already recommended moving off polling; listener approach
is the clean long-term fix and removes the wasted coroutine entirely.

## Goals / Non-goals

- Goals: tracker stops consuming resources when paused; position stays accurate.
- Non-goals: redesign playback architecture, change public behavior beyond this.

## Builder notes

Small change. Next step after approval: invoke writing-plans, then implement.
