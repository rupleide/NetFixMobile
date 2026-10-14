package com.rupleide.netfix.core.update

import com.rupleide.netfix.core.dpibypass.launchIntentWithLeanback
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val NETFIX_REPO = "rupleide/NetFixMobile"
    private const val SMART_TUBE_REPO = "yuliskov/SmartTube"
    private const val SMART_TUBE_FALLBACK_URL = "https://github.com/yuliskov/SmartTube/releases/download/31.94/SmartTube_stable_31.94_universal.apk"
    private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val description: String
    )

    private class LatestRelease(
        val tag: String,
        val apkUrl: String,
        val description: String
    )

    suspend fun checkUpdate(context: Context): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val isNetFixApk: (String) -> Boolean = { it.endsWith(".apk") }
                val release = fetchLatestReleaseViaWeb(NETFIX_REPO, isNetFixApk)
                    ?: fetchLatestReleaseViaApi(NETFIX_REPO, isNetFixApk)
                    ?: return@withContext null

                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"

                if (isNewerVersion(currentVersion, release.tag)) {
                    UpdateInfo(release.tag, release.apkUrl, release.description)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun fetchLatestReleaseViaWeb(repo: String, isWantedApk: (String) -> Boolean): LatestRelease? {
        return try {
            val tag = resolveLatestTag(repo) ?: return null
            val assetsHtml = readText(
                "https://github.com/$repo/releases/expanded_assets/$tag",
                BROWSER_USER_AGENT
            ) ?: return null
            val apkPaths = Regex("""href="(/[^"]+/releases/download/[^"]+\.apk)"""", RegexOption.IGNORE_CASE)
                .findAll(assetsHtml)
                .map { it.groupValues[1] }
                .toList()
            val apkPath = apkPaths.firstOrNull { isWantedApk(it.substringAfterLast('/')) } ?: return null
            LatestRelease(tag, "https://github.com$apkPath", "")
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveLatestTag(repo: String): String? {
        val latestUrl = "https://github.com/$repo/releases/latest"
        for (method in listOf("HEAD", "GET")) {
            val connection = URL(latestUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", BROWSER_USER_AGENT)
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val location = connection.getHeaderField("Location") ?: continue
                val tagMarker = "/tag/"
                val tagStart = location.lastIndexOf(tagMarker)
                if (tagStart >= 0) {
                    val tag = location.substring(tagStart + tagMarker.length).trim().trimEnd('/')
                    if (tag.isNotEmpty()) return Uri.decode(tag)
                }
            } catch (_: Exception) {
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun fetchLatestReleaseViaApi(repo: String, isWantedApk: (String) -> Boolean): LatestRelease? {
        return try {
            val connection = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "NetFixMobile")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode != 200) return null

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "")
            val body = json.optString("body", "")
            val assets = json.optJSONArray("assets") ?: return null

            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (isWantedApk(asset.optString("name", ""))) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            if (tagName.isEmpty() || downloadUrl.isEmpty()) null else LatestRelease(tagName, downloadUrl, body)
        } catch (_: Exception) {
            null
        }
    }

    private fun readText(url: String, userAgent: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != 200) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentClean = current.replace("v", "").split(".")
        val latestClean = latest.replace("v", "").split(".")
        val maxLength = maxOf(currentClean.size, latestClean.size)
        for (i in 0 until maxLength) {
            val currVal = currentClean.getOrNull(i)?.toIntOrNull() ?: 0
            val lateVal = latestClean.getOrNull(i)?.toIntOrNull() ?: 0
            if (lateVal > currVal) return true
            if (currVal > lateVal) return false
        }
        return false
    }

    suspend fun getSmartTubeLatestUrl(): String {
        return withContext(Dispatchers.IO) {
            val isUniversalStableApk: (String) -> Boolean = { it.contains("_stable_") && it.endsWith("_universal.apk") }
            val release = fetchLatestReleaseViaWeb(SMART_TUBE_REPO, isUniversalStableApk)
                ?: fetchLatestReleaseViaApi(SMART_TUBE_REPO, isUniversalStableApk)
            release?.apkUrl ?: SMART_TUBE_FALLBACK_URL
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        withContext(Dispatchers.Main) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        onError("Permission required")
                        return@withContext
                    }
                }

                val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = false
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == 302 || responseCode == 301) {
                    val redirectUrl = connection.getHeaderField("Location")
                    downloadAndInstallApk(context, redirectUrl, fileName, onProgress, onError)
                    return@withContext
                }

                val fileLength = connection.contentLength
                val cacheFile = File(context.externalCacheDir ?: context.cacheDir, fileName)
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }

                connection.inputStream.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        val data = ByteArray(8192)
                        var total = 0L
                        var count: Int
                        var lastPercent = -1
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            output.write(data, 0, count)
                            if (fileLength > 0) {
                                val percent = ((total * 100) / fileLength).toInt()
                                if (percent > lastPercent) {
                                    lastPercent = percent
                                    withContext(Dispatchers.Main) {
                                        onProgress(total.toFloat() / fileLength.toFloat())
                                    }
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    installApk(context, cacheFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Download failed")
                }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun isAutoUpdateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_update_enabled", true)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_update_enabled", enabled).apply()
    }

    fun isSmartTubeInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo("org.smarttube.stable", 0)
            true
        } catch (e: Exception) {
            try {
                pm.getPackageInfo("org.smarttube.beta", 0)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun openSmartTube(context: Context) {
        val pm = context.packageManager
        val intent = pm.launchIntentWithLeanback("org.smarttube.stable")
            ?: pm.launchIntentWithLeanback("org.smarttube.beta")
        if (intent != null) {
            context.startActivity(intent)
        }
    }
}
