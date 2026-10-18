package org.kerlann.easynode

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val code: Int, val name: String, val apkUrl: String, val pageUrl: String)

/**
 * Vérifie la dernière "Release" GitHub du dépôt qui a fabriqué l'APK.
 * Les releases s'appellent r<numéro> ; le numéro est le versionCode de l'APK.
 */
object UpdateChecker {
    private const val PREFS = "easynode"

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available: StateFlow<UpdateInfo?> = _available

    fun enabled(ctx: Context): Boolean =
        BuildConfig.UPDATE_REPO.isNotBlank() && SettingsStore.load(ctx).checkUpdates

    suspend fun check(ctx: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!enabled(ctx)) {
            _available.value = null
            return@withContext null
        }
        val found = try {
            val conn = URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != 200) {
                conn.disconnect()
                null
            } else {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                conn.disconnect()
                val code = Regex("""^r(\d+)$""").find(json.optString("tag_name"))?.groupValues?.get(1)?.toIntOrNull()
                val assets = json.optJSONArray("assets")
                var apk: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) {
                            apk = a.optString("browser_download_url"); break
                        }
                    }
                }
                val page = json.optString("html_url")
                if (code != null && code > BuildConfig.VERSION_CODE) {
                    UpdateInfo(code, json.optString("name").ifBlank { "r$code" }, apk ?: page, page)
                } else null
            }
        } catch (e: Exception) {
            null
        }
        _available.value = found
        found
    }

    /** Pour ne notifier qu'une seule fois par nouvelle version. */
    fun shouldNotify(ctx: Context, info: UpdateInfo): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getInt("notifiedUpdate", 0) >= info.code) return false
        p.edit().putInt("notifiedUpdate", info.code).apply()
        return true
    }
}
