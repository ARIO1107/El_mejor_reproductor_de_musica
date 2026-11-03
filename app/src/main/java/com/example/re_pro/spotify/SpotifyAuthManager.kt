package com.example.re_pro.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.re_pro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

sealed class HttpResponse {
    class Ok(val body: String) : HttpResponse()
    class Fail(val code: Int) : HttpResponse()
}

object SpotifyAuthManager {
    private const val PREFS = "spotify_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val REDIRECT_URI = "repro://spotify-callback"
    private const val AUTH_ENDPOINT = "https://accounts.spotify.com/authorize"
    private const val TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"

    val clientId: String get() = BuildConfig.SPOTIFY_CLIENT_ID
    val isConfigured: Boolean get() = clientId.isNotBlank()

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _loginInProgress = MutableStateFlow(false)
    val loginInProgress: StateFlow<Boolean> = _loginInProgress.asStateFlow()

    private val _authResult = MutableStateFlow<String?>(null)
    val authResult: StateFlow<String?> = _authResult.asStateFlow()

    private var pendingVerifier: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _loggedIn.value = !prefs.getString(KEY_ACCESS, null).isNullOrBlank()
    }

    fun accessToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ACCESS, null)

    fun launchLogin(context: Context) {
        if (clientId.isBlank()) return
        val verifier = randomVerifier()
        pendingVerifier = verifier
        val url = AUTH_ENDPOINT + "?" + listOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to REDIRECT_URI,
            "code_challenge" to codeChallenge(verifier),
            "code_challenge_method" to "S256",
            "scope" to "playlist-read-private playlist-read-collaborative"
        ).joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
        _loginInProgress.value = true
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    suspend fun handleAuthResult(context: Context, code: String?) {
        if (code.isNullOrBlank()) {
            finishLogin(ok = false, message = "No se completo el inicio de sesion en Spotify.")
            return
        }
        val verifier = pendingVerifier
        if (verifier == null) {
            finishLogin(ok = false, message = "La sesion expiro. Intenta de nuevo.")
            return
        }
        val result = withContext(Dispatchers.IO) {
            val form = "grant_type=authorization_code" +
                "&code=" + URLEncoder.encode(code, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8") +
                "&client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&code_verifier=" + URLEncoder.encode(verifier, "UTF-8")
            postForm(TOKEN_ENDPOINT, form)
        }
        if (result !is HttpResponse.Ok) {
            finishLogin(ok = false, message = "Spotify rechazo el inicio de sesion. Verifica que el Client ID y el Redirect URI (repro://spotify-callback) esten correctos.")
            return
        }
        val access = parseTokenField(result.body, "access_token")
        if (access.isNullOrBlank()) {
            finishLogin(ok = false, message = "Spotify rechazo el inicio de sesion. Intenta de nuevo.")
            return
        }
        val refresh = parseTokenField(result.body, "refresh_token")
        persist(context, access, refresh)
        pendingVerifier = null
        finishLogin(ok = true, message = "Sesion de Spotify iniciada correctamente.")
    }

    suspend fun refreshAccessToken(context: Context): Boolean {
        val refresh = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH, null) ?: return false
        val body = withContext(Dispatchers.IO) {
            val form = "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refresh, "UTF-8") +
                "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
            (postForm(TOKEN_ENDPOINT, form) as? HttpResponse.Ok)?.body
        } ?: return false
        val access = parseTokenField(body, "access_token")
        if (access.isNullOrBlank()) return false
        persist(context, access, refresh)
        return true
    }

    suspend fun apiGet(context: Context, url: String): String? {
        val token = accessToken(context) ?: return null
        var response = httpGet(url, "Authorization" to "Bearer $token")
        if (response is HttpResponse.Fail && response.code == 401) {
            if (!refreshAccessToken(context)) return null
            response = httpGet(url, "Authorization" to "Bearer ${accessToken(context)}")
        }
        return (response as? HttpResponse.Ok)?.body
    }

    fun consumeAuthResult() {
        _authResult.value = null
    }

    suspend fun logout(context: Context) {
        withContext(Dispatchers.IO) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
        _loggedIn.value = false
        _authResult.value = "Sesion de Spotify cerrada."
    }

    private fun finishLogin(ok: Boolean, message: String) {
        _loginInProgress.value = false
        _loggedIn.value = ok
        _authResult.value = message
    }

    private fun persist(context: Context, access: String, refresh: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
        _loggedIn.value = true
    }

    private fun parseTokenField(body: String, field: String): String? =
        Regex("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)

    private fun randomVerifier(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
        val random = SecureRandom()
        return buildString(64) {
            repeat(64) { append(chars[random.nextInt(chars.length)]) }
        }
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun postForm(url: String, form: String): HttpResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return if (code in 200..299) HttpResponse.Ok(body) else HttpResponse.Fail(code)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(url: String, vararg headers: Pair<String, String>): HttpResponse {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Re-Pro)")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return if (code in 200..299) HttpResponse.Ok(body) else HttpResponse.Fail(code)
        } finally {
            conn.disconnect()
        }
    }
}