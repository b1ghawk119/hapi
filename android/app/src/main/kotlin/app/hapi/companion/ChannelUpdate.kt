package app.hapi.companion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal const val CHANNEL_RELEASES = "https://github.com/b1ghawk119/hapi/releases"
private const val HOUR = 60 * 60 * 1000L
private val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.MINUTES).build()
internal fun apkUrl(code: Int) = "$CHANNEL_RELEASES/download/android-$code/HAPI.apk"
internal fun newerCode(remote: Int, installed: Int) = remote.takeIf { it > installed }
private fun apk(context: Context, code: Int) = File(context.cacheDir, "channel-update/ready/HAPI-$code.apk")

private suspend fun latest(): Pair<Int, String> = withContext(Dispatchers.IO) {
    val request = Request.Builder().url("$CHANNEL_RELEASES/latest/download/update.json").build()
    client.newCall(request).execute().use {
        if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
        val info = JSONObject(it.body!!.string())
        info.getInt("versionCode") to info.getString("versionName")
    }
}

private suspend fun download(context: Context, code: Int) = withContext(Dispatchers.IO) {
    val dest = apk(context, code)
    val dir = dest.parentFile!!
    dir.mkdirs()
    if (dest.isFile) return@withContext
    val tmp = File.createTempFile("download-", ".part", dir.parentFile)
    try {
        client.newCall(Request.Builder().url(apkUrl(code)).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body!!.byteStream().use { source -> tmp.outputStream().use { sink ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    ensureActive()
                    val n = source.read(buffer)
                    if (n == -1) break
                    sink.write(buffer, 0, n)
                }
            } }
        }
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(tmp.path, 0)
        @Suppress("DEPRECATION")
        if (info?.packageName != context.packageName || info.versionCode != code) throw IOException("Unexpected APK")
        if (!tmp.renameTo(dest)) throw IOException("Cannot cache APK")
        dir.listFiles()?.filter { it != dest }?.forEach { it.delete() }
    } finally { tmp.delete() }
}

@Composable
internal fun ChannelUpdatePrompt() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("github-channel", Context.MODE_PRIVATE) }
    var code by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - prefs.getLong("checked", 0)
                if (elapsed !in 0 until HOUR) {
                    try {
                        val (remote, label) = latest()
                        prefs.edit().putLong("checked", now).apply()
                        if (newerCode(remote, BuildConfig.VERSION_CODE) != null &&
                            (prefs.getInt("later", 0) != remote || now >= prefs.getLong("until", 0))) {
                            code = remote; name = label
                        }
                    } catch (_: IOException) { /* Check again on the next foreground or timer. */ }
                    catch (_: org.json.JSONException) { /* An incomplete feed isn't an update. */ }
                }
                delay(HOUR)
            }
        }
    }
    if (code <= BuildConfig.VERSION_CODE) return
    fun later() {
        prefs.edit().putInt("later", code).putLong("until", System.currentTimeMillis() + 24 * HOUR).apply()
        code = 0; failed = false
    }
    fun install() {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", apk(context, code))
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: ActivityNotFoundException) { failed = true }
          catch (_: SecurityException) { failed = true }
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) install() else failed = true
    }
    AlertDialog(
        onDismissRequest = { if (!busy) later() },
        title = { Text(stringResource(R.string.channel_update_title)) },
        text = { Text(if (failed) stringResource(R.string.channel_update_fallback) else stringResource(R.string.channel_update_message, name)) },
        dismissButton = { TextButton(enabled = !busy, onClick = { later() }) { Text(stringResource(R.string.channel_update_later)) } },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (failed) {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl(code)))); later() }
                    catch (_: ActivityNotFoundException) { /* The dialog still offers dismissal. */ }
                } else scope.launch {
                    busy = true
                    try {
                        download(context, code)
                        if (context.packageManager.canRequestPackageInstalls()) install()
                        else settings.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                    } catch (_: IOException) { failed = true }
                      catch (_: ActivityNotFoundException) { failed = true }
                      catch (_: SecurityException) { failed = true }
                    finally { busy = false }
                }
            }) { Text(stringResource(if (busy) R.string.channel_update_downloading else if (failed) R.string.channel_update_browser else R.string.channel_update_install)) }
        },
    )
}
