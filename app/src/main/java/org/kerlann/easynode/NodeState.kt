package org.kerlann.easynode

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class NodeStatus { STOPPED, STARTING, RUNNING, STOPPING, ERROR }

data class NodeInfo(
    val height: Long,
    val targetHeight: Long,
    val incoming: Int,
    val outgoing: Int,
    val synchronized: Boolean,
    val databaseSize: Long,
    val version: String,
) {
    val progress: Float
        get() {
            val target = maxOf(targetHeight, height)
            return if (target <= 0) 0f else (height.toFloat() / target).coerceIn(0f, 1f)
        }
}

/** État partagé entre le service (qui fait tourner monerod) et l'interface. */
object NodeState {
    private const val MAX_LOG_LINES = 3000

    private val _status = MutableStateFlow(NodeStatus.STOPPED)
    val status: StateFlow<NodeStatus> = _status

    private val _info = MutableStateFlow<NodeInfo?>(null)
    val info: StateFlow<NodeInfo?> = _info

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    fun setStatus(s: NodeStatus) {
        _status.value = s
        if (s == NodeStatus.STARTING) _error.value = null
        if (s == NodeStatus.STOPPED || s == NodeStatus.ERROR) _info.value = null
    }

    fun setInfo(i: NodeInfo?) { _info.value = i }

    fun fail(message: String) {
        _error.value = message
        appendLog("!! $message")
        setStatus(NodeStatus.ERROR)
    }

    fun setError(message: String?) { _error.value = message }

    fun appendLog(line: String) {
        _log.update { (it + line).takeLast(MAX_LOG_LINES) }
        writeToFile(line)
    }

    // ── Journal enregistré dans un fichier : il survit aux redémarrages et aux plantages de l'app ──

    /** Taille max du fichier ; au-delà il devient journal.old.log (on garde donc 2 fichiers). */
    private const val MAX_FILE_BYTES = 2_000_000L
    private var logFile: File? = null
    private var writer: BufferedWriter? = null

    /**
     * À appeler au démarrage de l'activité et du service (une seule fois par lancement de l'app) :
     * recharge les dernières lignes du fichier, ajoute une ligne de séparation
     * et enregistre les plantages de l'app dans le journal.
     */
    @Synchronized
    fun init(ctx: Context) {
        if (logFile != null) return
        val f = File(ctx.applicationContext.filesDir, "journal.log")
        logFile = f
        if (f.exists()) {
            val old = try { f.readLines() } catch (_: Exception) { emptyList() }
            _log.value = old.takeLast(MAX_LOG_LINES)
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        appendLog("===== $stamp · app started =====")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                Log.getStackTraceString(e).lines().filter { it.isNotBlank() }.take(40).forEachIndexed { i, l ->
                    appendLog(if (i == 0) "!! CRASH [${t.name}] $l" else "!!   $l")
                }
            } catch (_: Exception) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    @Synchronized
    private fun writeToFile(line: String) {
        val f = logFile ?: return
        try {
            if (writer == null) writer = BufferedWriter(FileWriter(f, true))
            writer?.apply { write(line); newLine(); flush() }
            if (f.length() > MAX_FILE_BYTES) {
                writer?.close(); writer = null
                f.renameTo(File(f.parentFile, "journal.old.log"))
            }
        } catch (_: Exception) {
            writer = null
        }
    }
}
