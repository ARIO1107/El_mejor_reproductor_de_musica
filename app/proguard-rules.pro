# Add project specific ProGuard rules here.

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# DataStore
-keep class androidx.datastore.preferences.** { *; }

# Coil
-dontwarn coil.**

# Compose
-dontwarn androidx.compose.**

# Palette
-keep class androidx.palette.** { *; }

# Eliminación de voz (AudioProcessor custom)
-keep class com.example.re_pro.audio.** { *; }
