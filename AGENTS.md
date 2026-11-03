# Re-Pro (app de música Android)

## Restricciones del dispositivo (IMPORTANTE - siempre)
- **NO usar `adb`/`logcat`** en este proyecto.
- Diagnóstico de fallos sin adb: la app guarda el stack trace en `filesDir/crash_log.txt` (ReProApplication) y al abrir muestra un diálogo con el fallo anterior (MainActivity). Pedir al usuario que lea ese texto.
- Instalación: transferir el APK al teléfono manualmente (formatos: Drive/WhatsApp/Telegram/USB MTP) y abrirlo con el explorador de archivos.

## Comandos de build (Windows / PowerShell)
- `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'`
- `& '.\gradlew.bat' assembleDebug`
- `& '.\gradlew.bat' testDebugUnitTest` (59 tests)
- `& '.\gradlew.bat' assembleRelease` (requiere el keystore local y las propiedades Gradle `REPRO_KEYSTORE_PASSWORD` y `REPRO_KEYSTORE_KEY_PASSWORD`)
- No usar `&&` (PowerShell 5.1). Usar `;` o `if ($?) { ... }`.

## Notas
- El proyecto usa Git para control de versiones.
- Para las playlists privadas de Spotify se requiere `SPOTIFY_CLIENT_ID` en `gradle.properties` (BuildConfig).
- minSdk 24, targetSdk 37, Compose Material3 + Hilt + Room + Media3.