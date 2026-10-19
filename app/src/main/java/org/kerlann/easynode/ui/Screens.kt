package org.kerlann.easynode.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kerlann.easynode.BuildConfig
import org.kerlann.easynode.LocaleHelper
import org.kerlann.easynode.NodeService
import org.kerlann.easynode.NodeSettings
import org.kerlann.easynode.NodeState
import org.kerlann.easynode.NodeStatus
import org.kerlann.easynode.R
import org.kerlann.easynode.SettingsStore
import org.kerlann.easynode.Storage
import org.kerlann.easynode.TorManager
import org.kerlann.easynode.UpdateChecker
import org.kerlann.easynode.UpdatePhase
import org.kerlann.easynode.Updater
import org.kerlann.easynode.percent
import java.net.Inet4Address

// ── Squelette ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyNodeApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_node) to Icons.Filled.Home,
        stringResource(R.string.tab_settings) to Icons.Filled.Settings,
        stringResource(R.string.tab_log) to Icons.AutoMirrored.Filled.List,
        stringResource(R.string.tab_info) to Icons.Filled.Info,
    )
    Scaffold(
        topBar = { AppHeader() },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> NodeScreen()
                1 -> SettingsScreen()
                2 -> LogScreen()
                else -> InfoScreen()
            }
        }
    }
}

// ── Bandeau titre (« bandeau carte ») ─────────────────────────────────────────

@Composable
private fun AppHeader() {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        0f to Color(0xFF2A1A10),
                        0.6f to Color(0xFF1C1C1C),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                )
                .border(1.dp, Color(0xFF3A2A1F), shape)
                // liseré orange à gauche
                .drawBehind { drawRect(XmrOrange, size = Size(4.dp.toPx(), size.height)) }
                .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        append("Easynode")
                        withStyle(SpanStyle(color = XmrOrange)) { append("Droid") }
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                // Ligne 2 : badge de version puis sous-titre
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "v${BuildConfig.MONERO_VERSION}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = XmrOrange,
                        maxLines = 1,
                        modifier = Modifier
                            .border(1.dp, XmrOrange, RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.header_subtitle),
                        fontSize = 12.sp,
                        color = Color(0xFFA0A0A0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ── Petits composants ────────────────────────────────────────────────────────

@Composable
private fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, color = XmrOrange, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun KV(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun Hint(text: String, color: Color = Muted) {
    Text(text, color = color, fontSize = 13.sp, lineHeight = 17.sp)
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (hint != null) Hint(hint)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

private fun bytes(ctx: Context, b: Long): String = Formatter.formatFileSize(ctx, b)

/** Contrôles avant démarrage ; renvoie un message d'erreur ou null. */
private fun tryStart(ctx: Context): String? {
    val s = SettingsStore.load(ctx)
    if (s.rpcLogin && s.rpcPassword.length < 6) return ctx.getString(R.string.err_password)
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    if (!s.allowMetered && cm.isActiveNetworkMetered) return ctx.getString(R.string.err_metered)
    NodeService.start(ctx)
    return null
}

// ── Onglet Nœud ──────────────────────────────────────────────────────────────

@Composable
fun NodeScreen() {
    val ctx = LocalContext.current
    val status by NodeState.status.collectAsStateWithLifecycle()
    val info by NodeState.info.collectAsStateWithLifecycle()
    val error by NodeState.error.collectAsStateWithLifecycle()
    val settings = remember(status) { SettingsStore.load(ctx) }
    val storage = remember(settings) { Storage.selected(ctx, settings) }
    var used by remember { mutableLongStateOf(0L) }
    var free by remember { mutableLongStateOf(0L) }
    LaunchedEffect(storage.dir, status, info?.height) {
        val (u, f) = withContext(Dispatchers.IO) {
            Storage.usedBytes(storage.dir) to Storage.freeBytes(storage.dir)
        }
        used = u
        free = f
    }

    val update by UpdateChecker.available.collectAsStateWithLifecycle()
    val updState by Updater.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { UpdateChecker.check(ctx) }

    val (statusText, statusColor) = when (status) {
        NodeStatus.STOPPED -> stringResource(R.string.status_stopped) to Muted
        NodeStatus.STARTING -> stringResource(R.string.status_starting) to Warn
        NodeStatus.RUNNING -> stringResource(R.string.status_running) to Ok
        NodeStatus.STOPPING -> stringResource(R.string.status_stopping) to Warn
        NodeStatus.ERROR -> stringResource(R.string.status_error) to Err
    }
    val active = status == NodeStatus.RUNNING || status == NodeStatus.STARTING

    ScreenColumn {
        update?.let { u ->
            Section(stringResource(R.string.update_available)) {
                Text(u.name, fontWeight = FontWeight.SemiBold)
                when (updState.phase) {
                    UpdatePhase.DOWNLOADING -> {
                        Text(stringResource(R.string.upd_downloading, percent(updState.progress)))
                        LinearProgressIndicator(progress = { updState.progress }, modifier = Modifier.fillMaxWidth(), color = XmrOrange)
                    }
                    UpdatePhase.STOPPING_NODE -> {
                        Text(stringResource(R.string.upd_stopping))
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = XmrOrange)
                    }
                    UpdatePhase.INSTALLING -> {
                        Text(stringResource(R.string.upd_installing))
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = XmrOrange)
                    }
                    UpdatePhase.WAITING_USER -> Hint(updState.message ?: "", Warn)
                    UpdatePhase.ERROR, UpdatePhase.IDLE -> {
                        Hint(stringResource(R.string.update_hint))
                        updState.message?.let { Hint(it, Err) }
                    }
                }
                if (!Updater.busy) {
                    Button(
                        onClick = { Updater.start(ctx, u, interactive = true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.update_install)) }
                }
            }
        }

        Section {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(statusText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    if (active) NodeService.stop(ctx)
                    else tryStart(ctx)?.let { NodeState.setError(it) }
                },
                enabled = status != NodeStatus.STOPPING,
                colors = if (active) ButtonDefaults.buttonColors(containerColor = Err, contentColor = Color.White)
                else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(if (active) R.string.btn_stop else R.string.btn_start), fontSize = 16.sp)
            }
            error?.let { Hint(it, Err) }
        }

        if (status == NodeStatus.RUNNING) {
            Section {
                val i = info
                if (i == null) {
                    Text(stringResource(R.string.sync_waiting))
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = XmrOrange)
                } else {
                    Text(
                        if (i.synchronized) stringResource(R.string.synced)
                        else stringResource(R.string.sync_progress, percent(i.progress)),
                        color = if (i.synchronized) Ok else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress = { if (i.synchronized) 1f else i.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (i.synchronized) Ok else XmrOrange,
                    )
                    KV(stringResource(R.string.label_height), "${i.height} / ${maxOf(i.targetHeight, i.height)}")
                    KV(stringResource(R.string.label_peers), "${i.incoming} / ${i.outgoing}")
                    if (i.databaseSize > 0) KV(stringResource(R.string.label_db), bytes(ctx, i.databaseSize))
                    if (i.version.isNotBlank()) KV(stringResource(R.string.label_version), i.version)
                }
            }
        }

        Section(stringResource(R.string.label_storage)) {
            KV(
                stringResource(R.string.label_type),
                stringResource(R.string.type_pruned),
            )
            KV(storage.label, bytes(ctx, used))
            Hint(stringResource(R.string.free_space, bytes(ctx, free)))
            Hint(stringResource(R.string.space_needed, bytes(ctx, settings.requiredBytes)))
            if (free + used < settings.requiredBytes) Hint(stringResource(R.string.warn_space), Warn)
        }

        Hint(stringResource(R.string.warn_data))

        GithubLinks()
    }
}

private const val REPO_URL = "https://github.com/kerlannXmr/EasynodeDroid"
private const val DOWNLOAD_URL = "https://github.com/kerlannXmr/EasynodeDroid/releases/latest"

/** Petits liens discrets en bas de l'écran d'accueil. */
@Composable
private fun GithubLinks() {
    val ctx = LocalContext.current
    fun open() = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { open() }) {
                Text(stringResource(R.string.link_github), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("·", color = Muted)
            TextButton(onClick = { open() }) {
                Text(stringResource(R.string.link_star), fontSize = 13.sp, color = XmrOrange)
            }
        }
        Text(
            stringResource(R.string.link_star_hint),
            fontSize = 11.sp,
            color = Muted,
        )
        Spacer(Modifier.height(10.dp))
        val chooserTitle = stringResource(R.string.share_chooser)
        val shareText = stringResource(R.string.share_text, DOWNLOAD_URL)
        OutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                ctx.startActivity(Intent.createChooser(send, chooserTitle))
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = XmrOrange)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.link_share), fontSize = 13.sp)
        }
    }
}

// ── Onglet Réglages ──────────────────────────────────────────────────────────

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by NodeState.status.collectAsStateWithLifecycle()
    val editable = status == NodeStatus.STOPPED || status == NodeStatus.ERROR
    var s by remember { mutableStateOf(SettingsStore.load(ctx)) }
    fun update(n: NodeSettings) { s = n; SettingsStore.save(ctx, n) }
    val options = remember { Storage.options(ctx) }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenColumn {
        Section(stringResource(R.string.set_language)) {
            val current = remember { LocaleHelper.get(ctx) }
            listOf(
                LocaleHelper.SYSTEM to stringResource(R.string.lang_system),
                "fr" to "Français",
                "en" to "English",
            ).forEach { (code, label) ->
                RadioRow(label, current == code, true) {
                    if (code != current) {
                        LocaleHelper.set(ctx, code)
                        (ctx as? Activity)?.recreate()
                    }
                }
            }
        }

        if (!editable) Hint(stringResource(R.string.settings_running_hint), Warn)

        Section(stringResource(R.string.set_storage)) {
            options.forEachIndexed { i, o ->
                val sel = s.storageIndex == i || (s.storageIndex >= options.size && i == 0)
                RadioRow("${o.label} — ${bytes(ctx, Storage.freeBytes(o.dir))}", sel, editable) {
                    update(s.copy(storageIndex = i))
                }
            }
            Hint(stringResource(R.string.storage_hint))
        }

        Section("RPC") {
            SwitchRow(stringResource(R.string.set_rpc_lan), stringResource(R.string.set_rpc_lan_hint), s.rpcLan, editable) {
                update(s.copy(rpcLan = it))
            }
            HorizontalDivider()
            SwitchRow(stringResource(R.string.set_rpc_login), null, s.rpcLogin, editable) { update(s.copy(rpcLogin = it)) }
            if (s.rpcLogin) {
                OutlinedTextField(
                    value = s.rpcUser,
                    onValueChange = { v -> update(s.copy(rpcUser = v.filter { it.isLetterOrDigit() || it == '_' || it == '-' })) },
                    label = { Text(stringResource(R.string.set_rpc_user)) },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = s.rpcPassword,
                    onValueChange = { v -> update(s.copy(rpcPassword = v.filter { it != ':' && !it.isWhitespace() })) },
                    label = { Text(stringResource(R.string.set_rpc_pass)) },
                    singleLine = true,
                    enabled = editable,
                    isError = s.rpcPassword.length < 6,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (s.rpcLan && !s.rpcLogin) Hint(stringResource(R.string.warn_lan_no_login), Warn)
        }

        Section(stringResource(R.string.label_peers).substringBefore(" (")) {
            SwitchRow(stringResource(R.string.set_banlist), stringResource(R.string.set_banlist_hint), s.useBanList, editable) {
                update(s.copy(useBanList = it))
            }
            HorizontalDivider()
            SwitchRow(stringResource(R.string.set_tor), stringResource(R.string.set_tor_hint), s.torTx, editable) {
                update(s.copy(torTx = it))
            }
            if (s.torTx) OnionInfo()
        }

        Section {
            SwitchRow(stringResource(R.string.set_metered), stringResource(R.string.set_metered_hint), s.allowMetered, true) {
                update(s.copy(allowMetered = it))
            }
            HorizontalDivider()
            SwitchRow(stringResource(R.string.set_updates), stringResource(R.string.set_updates_hint), s.checkUpdates, true) {
                update(s.copy(checkUpdates = it))
            }
            if (s.checkUpdates) {
                SwitchRow(stringResource(R.string.set_auto_update), stringResource(R.string.set_auto_update_hint), s.autoInstall, true) {
                    update(s.copy(autoInstall = it))
                }
            }
            val pm = ctx.getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                HorizontalDivider()
                Hint(stringResource(R.string.set_battery_hint))
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.set_battery)) }
            }
        }

        OutlinedButton(
            onClick = { confirmDelete = true },
            enabled = editable,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Err),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.set_delete)) }

        Hint("monerod v${BuildConfig.MONERO_VERSION} · EasynodeDroid ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }

    if (confirmDelete) {
        val dir = Storage.selected(ctx, s).dir
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, dir.path)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        withContext(Dispatchers.IO) { dir.deleteRecursively() }
                        Toast.makeText(ctx, R.string.deleted, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.btn_delete), color = Err) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.btn_cancel)) }
            },
        )
    }
}

/** Adresse .onion + port, avec bouton copier (Réglages et onglet Wallet). */
@Composable
private fun OnionInfo() {
    val ctx = LocalContext.current
    val status by NodeState.status.collectAsStateWithLifecycle()
    val onion = remember(status) { TorManager.savedOnion(ctx) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onion == null) {
            Hint(stringResource(R.string.tor_pending), Warn)
        } else {
            Text(stringResource(R.string.tor_address), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            CopyRow(onion, small = true)
            Text(stringResource(R.string.tor_port, TorManager.ONION_PORT), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Hint(stringResource(R.string.tor_wallet_hint))
        }
    }
}

// ── Onglet Journal ───────────────────────────────────────────────────────────

@Composable
fun LogScreen() {
    val lines by NodeState.log.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size, lines.lastOrNull()) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }
    if (lines.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Hint(stringResource(R.string.log_empty)) }
    } else LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        itemsIndexed(lines) { _, line ->
            Text(
                line,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = when {
                    line.startsWith("!!") || line.contains(" E ") || line.contains("ERROR") -> Err
                    line.contains(" W ") || line.contains("WARN") -> Warn
                    line.contains("Synced") || line.contains("SYNCHRONIZED") -> Ok
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

// ── Onglet Wallet ────────────────────────────────────────────────────────────

private fun localIp(ctx: Context): String? {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val lp = cm.getLinkProperties(cm.activeNetwork) ?: return null
    return lp.linkAddresses.map { it.address }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
}

@Composable
private fun CopyRow(value: String, small: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, fontFamily = FontFamily.Monospace, fontSize = if (small) 13.sp else 18.sp, color = XmrOrange, modifier = Modifier.weight(1f))
        TextButton(onClick = {
            clipboard.setText(AnnotatedString(value))
            Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
        }) { Text("⧉") }
    }
}

/**
 * Mot de passe masqué (••••••••) : un toucher l'affiche, un second le masque.
 * Il se re-masque tout seul quand on change d'onglet ou qu'on quitte l'app.
 * Le bouton copier fonctionne même masqué.
 */
@Composable
private fun PasswordRow(password: String) {
    var shown by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { shown = false }
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current
    val masked = "•".repeat(8)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.info_pass, if (shown) password else masked),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).clickable { shown = !shown },
        )
        TextButton(onClick = { shown = !shown }) {
            Text(stringResource(if (shown) R.string.pass_hide else R.string.pass_show), fontSize = 13.sp)
        }
        TextButton(onClick = {
            clipboard.setText(AnnotatedString(password))
            Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
        }) { Text("⧉") }
    }
}

@Composable
fun InfoScreen() {
    val ctx = LocalContext.current
    val s = remember { SettingsStore.load(ctx) }
    val ip = remember { localIp(ctx) }

    ScreenColumn {
        Section(stringResource(R.string.info_title)) {
            Text(stringResource(R.string.info_local), fontWeight = FontWeight.SemiBold)
            CopyRow("127.0.0.1:${NodeService.RPC_PORT}")
            HorizontalDivider()
            Text(stringResource(R.string.info_lan), fontWeight = FontWeight.SemiBold)
            when {
                !s.rpcLan -> Hint(stringResource(R.string.info_lan_off), Warn)
                ip == null -> Hint(stringResource(R.string.info_no_ip), Warn)
                else -> CopyRow("$ip:${NodeService.RESTRICTED_PORT}")
            }
            HorizontalDivider()
            val cred = s.rpcCredentials
            if (cred != null) {
                Text(stringResource(R.string.info_user, cred.first), fontFamily = FontFamily.Monospace)
                PasswordRow(cred.second)
            } else {
                Hint(stringResource(R.string.info_noauth))
            }
        }
        if (s.torTx) {
            Section(stringResource(R.string.info_tor_title)) { OnionInfo() }
        }
        Section(stringResource(R.string.info_steps_title)) { Text(stringResource(R.string.info_steps), lineHeight = 22.sp) }
        Section(stringResource(R.string.info_p2p_title)) { Hint(stringResource(R.string.info_p2p)) }
        Section(stringResource(R.string.info_about_title)) { Hint(stringResource(R.string.info_about)) }
    }
}
