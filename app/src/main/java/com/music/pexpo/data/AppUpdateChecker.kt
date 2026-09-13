package com.music.pexpo.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.music.pexpo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/** Checks the isolated Pexpo beta GitHub prerelease channel for newer APKs. */
object AppUpdateChecker {
    data class UpdateInfo(val version: String, val releaseUrl: String, val apkUrl: String?, val notes: String?)

    private const val CACHE_SUBDIR = "updates"
    private const val RELEASES_URL = "https://api.github.com/repos/frRitamDas/pexpo-beta/releases?per_page=20"
    private val json = Json { ignoreUnknownKeys = true }
    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }
    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()
    @Volatile private var downloadCancelled = false

    suspend fun check(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(RELEASES_URL)
                .header("Accept", "application/vnd.github+json").build()
            val body = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            } ?: return@runCatching
            val releases = json.parseToJsonElement(body) as? JsonArray ?: return@runCatching
            val release = releases.mapNotNull { it as? JsonObject }
                .filter { it["prerelease"]?.jsonPrimitive?.contentOrNull == "true" }
                .filter { it["tag_name"]?.jsonPrimitive?.contentOrNull?.contains("-beta.") == true }
                .maxByOrNull { versionKey(it["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")) }
                ?: return@runCatching
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val latest = tag.removePrefix("v")
            if (!isNewer(latest, BuildConfig.VERSION_NAME)) return@runCatching
            val url = release["html_url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
            val apkUrl = apkAssetUrl(release, installedVariant(context))
            val notes = release["body"]?.jsonPrimitive?.contentOrNull
            _available.value = UpdateInfo(latest, url, apkUrl, notes)
        }
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    private fun apkAssetUrl(release: JsonObject, variant: String): String? = runCatching {
        val version = release["tag_name"]?.jsonPrimitive?.contentOrNull?.removePrefix("v") ?: return@runCatching null
        val assets = release["assets"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
        fun urlFor(name: String) = assets.firstOrNull {
            it["name"]?.jsonPrimitive?.contentOrNull?.equals(name, ignoreCase = true) == true &&
                it["state"]?.jsonPrimitive?.contentOrNull == "uploaded"
        }?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
        urlFor("pexpo-$version-$variant.apk") ?: urlFor("pexpo-$version-universal.apk")
    }.getOrNull()

    suspend fun downloadApk(context: Context) = withContext(Dispatchers.IO) {
        val url = _available.value?.apkUrl
        if (url.isNullOrBlank()) {
            _download.value = DownloadState.Failed("No compatible Pexpo APK was found in the beta release.")
            return@withContext
        }
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f)
        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            val target = File(dir, "pexpo-update.apk")
            val temp = File(dir, "pexpo-update.apk.part")
            temp.delete()
            val request = Request.Builder().url(url).header("Accept", "application/octet-stream").build()
            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("The update response was empty.")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            if (downloadCancelled) {
                                temp.delete(); _download.value = DownloadState.Idle; return@withContext
                            }
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) _download.value = DownloadState.Downloading((downloaded.toDouble() / total).toFloat().coerceIn(0f, 1f))
                        }
                    }
                }
            }
            check(temp.exists() && temp.length() > 0L) { "The downloaded APK is empty." }
            validateApk(context, temp)
            if (target.exists()) target.delete()
            check(temp.renameTo(target)) { "Could not finalize the downloaded APK." }
            _download.value = DownloadState.Ready(target)
        }.onFailure { error -> _download.value = DownloadState.Failed(error.message ?: "Could not download the Pexpo update.") }
    }

    fun cancelDownload() { downloadCancelled = true }
    fun resetDownload() { _download.value = DownloadState.Idle }

    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun validateApk(context: Context, file: File) {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(file.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: error("The downloaded file is not a valid Android APK.")
        check(info.packageName == BuildConfig.APPLICATION_ID) { "The downloaded APK belongs to ${info.packageName}, not ${BuildConfig.APPLICATION_ID}." }
        check(info.longVersionCode > BuildConfig.VERSION_CODE) { "The downloaded APK is not newer than the installed Pexpo version." }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val installed = context.packageManager.packageInfoForCurrentApp(PackageManager.GET_SIGNING_CERTIFICATES.toLong()).signingInfo
            val candidate = info.signingInfo
            check(installed != null && candidate != null && installed.apkContentsSigners.contentEquals(candidate.apkContentsSigners)) {
                "The downloaded APK is not signed by the installed Pexpo signing certificate."
            }
        } else {
            @Suppress("DEPRECATION") val installed = context.packageManager.packageInfoForCurrentApp(PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION") val candidate = info.signatures.orEmpty()
            @Suppress("DEPRECATION") check(installed.signatures.orEmpty().contentEquals(candidate)) {
                "The downloaded APK is not signed by the installed Pexpo signing certificate."
            }
        }
    }

    private fun PackageManager.packageInfoForCurrentApp(flags: Int) = getPackageInfo(BuildConfig.APPLICATION_ID, flags)

    private fun installedVariant(context: Context): String {
        val joined = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).splitNames.orEmpty().joinToString(" ").lowercase() }.getOrDefault("")
        return when {
            "arm64" in joined || "arm64_v8a" in joined -> "arm64-v8a"
            "armeabi" in joined || "armeabi_v7a" in joined -> "armeabi-v7a"
            "x86_64" in joined -> "x86_64"
            else -> "universal"
        }
    }

    private data class BetaVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val beta: Int,
    ) : Comparable<BetaVersion> {
        override fun compareTo(other: BetaVersion): Int =
            compareValuesBy(this, other, BetaVersion::major, BetaVersion::minor, BetaVersion::patch, BetaVersion::beta)
    }

    private val betaPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)-beta\\.(\\d+)$")
    private fun versionKey(version: String): BetaVersion {
        val match = betaPattern.matchEntire(version)
            ?: return BetaVersion(0, 0, 0, 0)
        return BetaVersion(
            major = match.groupValues[1].toInt(),
            minor = match.groupValues[2].toInt(),
            patch = match.groupValues[3].toInt(),
            beta = match.groupValues[4].toInt(),
        )
    }

    private fun isNewer(latest: String, current: String): Boolean = versionKey(latest) > versionKey(current)
}
