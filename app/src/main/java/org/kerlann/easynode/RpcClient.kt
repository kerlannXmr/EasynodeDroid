package org.kerlann.easynode

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Client JSON-RPC minimal pour monerod (127.0.0.1), avec authentification
 * HTTP Digest quand --rpc-login est actif (même logique que `curl --digest`).
 */
class RpcClient(
    private val port: Int,
    private val credentials: Pair<String, String>?,
) {
    private var nc = 0

    fun getInfo(): JSONObject? = call("get_info")

    fun call(method: String): JSONObject? {
        val body = """{"jsonrpc":"2.0","id":"0","method":"$method"}"""
        val path = "/json_rpc"
        return try {
            var conn = post(path, body, null)
            if (conn.responseCode == 401 && credentials != null) {
                val challenges = conn.headerFields.entries
                    .filter { it.key?.equals("WWW-Authenticate", ignoreCase = true) == true }
                    .flatMap { it.value }
                conn.disconnect()
                val auth = digestHeader(challenges, "POST", path) ?: return null
                conn = post(path, body, auth)
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            JSONObject(text).optJSONObject("result")
        } catch (e: Exception) {
            null
        }
    }

    private fun post(path: String, body: String, auth: String?): HttpURLConnection {
        val conn = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 20_000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        if (auth != null) conn.setRequestProperty("Authorization", auth)
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn
    }

    private fun digestHeader(challenges: List<String>, method: String, uri: String): String? {
        val (user, pass) = credentials ?: return null
        val digests = challenges.filter { it.trim().startsWith("Digest", ignoreCase = true) }
        // monerod propose MD5 et MD5-sess ; on prend MD5.
        val challenge = digests.firstOrNull { !it.contains("MD5-sess", ignoreCase = true) }
            ?: digests.firstOrNull() ?: return null
        val params = Regex("""(\w+)=(?:"([^"]*)"|([^,\s]+))""").findAll(challenge)
            .associate { m -> m.groupValues[1].lowercase() to m.groupValues[2].ifEmpty { m.groupValues[3] } }
        val realm = params["realm"] ?: return null
        val nonce = params["nonce"] ?: return null
        val qop = params["qop"]?.split(',')?.map { it.trim() }?.firstOrNull { it == "auth" }
        val cnonce = randomHex(16)
        val ncValue = String.format("%08x", ++nc)
        val ha1 = md5("$user:$realm:$pass")
        val ha2 = md5("$method:$uri")
        val response = if (qop != null) md5("$ha1:$nonce:$ncValue:$cnonce:$qop:$ha2") else md5("$ha1:$nonce:$ha2")
        val sb = StringBuilder("Digest username=\"$user\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", algorithm=MD5, response=\"$response\"")
        if (qop != null) sb.append(", qop=$qop, nc=$ncValue, cnonce=\"$cnonce\"")
        params["opaque"]?.let { sb.append(", opaque=\"$it\"") }
        return sb.toString()
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }
}
