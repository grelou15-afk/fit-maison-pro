package com.fitmaison.pro

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var immersive = false
    var specKeepOn = false

    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* rien */ }

    private val chooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
            fileCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySpec()
        web = WebView(this)
        setContentView(web)
        if (specFullscreen) toggleImmersive()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        web.addJavascriptInterface(FitBridge(this), "AndroidFit")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                val u = req.url
                if (u.scheme == "file") return false
                startActivity(Intent(Intent.ACTION_VIEW, u)); return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                v: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb
                return try { chooser.launch(params.createIntent()); true }
                catch (e: Exception) { fileCallback = null; false }
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        // Reprogramme le rappel à chaque ouverture
        Notifier.schedule(this)
    }

    private var specFullscreen = false

    /** Lit assets/spec.json (orientation, plein écran, écran allumé) généré par le constructeur d'APK. */
    private fun applySpec() {
        try {
            val j = org.json.JSONObject(assets.open("spec.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            requestedOrientation = when (j.optString("orientation", "auto")) {
                "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            specFullscreen = j.optBoolean("fullscreen", false)
            specKeepOn = j.optBoolean("keepScreenOn", false)
            if (specKeepOn) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {}
    }

    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun askExactAlarmIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }
                catch (_: Exception) {}
            }
        }
    }

    fun toggleImmersive() {
        immersive = !immersive
        val c = WindowInsetsControllerCompat(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, !immersive)
        if (immersive) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else c.show(WindowInsetsCompat.Type.systemBars())
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        web.evaluateJavascript("try{startReminderCheck()}catch(e){}", null)
    }
}
