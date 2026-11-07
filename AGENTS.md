# Re-Pro (app de música Android)

## Restricciones del dispositivo (IMPORTANTE - siempre)
- El teléfono del usuario está financiado por **Krece**: **NO se puede activar la depuración USB/adb**. Nunca usar `adb`/`logcat` en este proyecto.
- Diagnóstico de fallos sin adb: la app guarda el stack trace en `filesDir/crash_log.txt` (RewindApplication) y al abrir muestra un diálogo con el fallo anterior (MainActivity). Pedir al usuario que lea ese texto.
- Instalación: transferir el APK al teléfono manualmente (formatos: Drive/WhatsApp/Telegram/USB MTP) y abrirlo con el explorador de archivos.

## Comandos de build (Windows / PowerShell)
- `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'`
- `& '.\gradlew.bat' assembleDebug`
- `& '.\gradlew.bat' testDebugUnitTest` (59 tests)
- `& '.\gradlew.bat' assembleRelease` (APK firmado en `keystore/re-pro-release.jks`, alias `re-pro`, pass por defecto `rePro2026`)
- No usar `&&` (PowerShell 5.1). Usar `;` o `if ($?) { ... }`.

## Notas
- Sin Control de Versiones (no es repo git).
- Para las playlists privadas de Spotify se requiere `SPOTIFY_CLIENT_ID` en `gradle.properties` (BuildConfig).
- minSdk 24, targetSdk 37, Compose Material3 + Hilt + Room + Media3.