package com.fitmaison.pro

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File

/** Pont JS ↔ Android : window.AndroidFit.* (utilisé par fit_maison_pro_3_android.html) */
class FitBridge(private val act: MainActivity) {

    @JavascriptInterface
    fun showNotification(title: String, body: String) {
        Notifier.show(act, title, body)
    }

    @JavascriptInterface
    fun requestNotifications() {
        act.runOnUiThread { act.askNotificationPermission() }
    }

    @JavascriptInterface
    fun scheduleReminder(hhmm: String) {
        Notifier.saveAndSchedule(act, hhmm)
        act.runOnUiThread { act.askExactAlarmIfNeeded() }
    }

    @JavascriptInterface
    fun setKeepScreenOn(on: Boolean) {
        act.runOnUiThread {
            if (on || act.specKeepOn) act.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else act.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @JavascriptInterface
    fun fullscreen() {
        act.runOnUiThread { act.toggleImmersive() }
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        try {
            act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("Impossible d’ouvrir le lien")
        }
    }

    @JavascriptInterface
    fun saveFile(name: String, data: String, mime: String?) {
        try {
            val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val type = if (mime.isNullOrBlank()) "application/octet-stream" else mime
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safe)
                    put(MediaStore.Downloads.MIME_TYPE, type)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = act.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore")
                act.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray(Charsets.UTF_8)) }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, safe).writeText(data)
            }
            toast("Enregistré dans Téléchargements : $safe")
        } catch (e: Exception) {
            toast("Échec de l’enregistrement")
        }
    }

    private fun toast(msg: String) = act.runOnUiThread { Toast.makeText(act, msg, Toast.LENGTH_LONG).show() }
}
