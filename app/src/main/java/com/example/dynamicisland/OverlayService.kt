package com.example.dynamicisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.example.dynamicisland.SHOW"
        const val ACTION_SETTINGS = "com.example.dynamicisland.SETTINGS"
        private const val CHANNEL_ID = "dynamic_island_service"
        private const val NOTIFICATION_ID = 7201
    }

    private lateinit var windowManager: WindowManager
    private var islandView: LinearLayout? = null
    private var titleView: TextView? = null
    private var bodyView: TextView? = null
    private var iconView: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideIsland() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SHOW -> showIsland(
                    intent.getStringExtra("title").orEmpty(),
                    intent.getStringExtra("body").orEmpty(),
                    intent.getStringExtra("type").orEmpty()
                )
                ACTION_SETTINGS -> updateLayout()
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra("level", -1)
                    val scale = intent.getIntExtra("scale", 100).coerceAtLeast(1)
                    val percent = if (level >= 0) (level * 100 / scale) else -1
                    val status = intent.getIntExtra("status", -1)
                    val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    if (percent >= 0 && IslandSettings.enabled(this@OverlayService)) {
                        showIsland(
                            if (charging) "Sedang mengisi daya" else "Baterai",
                            "$percent%",
                            "battery"
                        )
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, serviceNotification())

        val filter = IntentFilter().apply {
            addAction(ACTION_SHOW)
            addAction(ACTION_SETTINGS)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
        createIslandView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (IslandSettings.enabled(this) && android.provider.Settings.canDrawOverlays(this)) {
            attachIsland()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun createIslandView() {
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), 0, dp(13), 0)
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(100).toFloat()
                setStroke(dp(1), Color.rgb(35, 35, 39))
            }
            elevation = dp(8).toFloat()
        }

        iconView = TextView(this).apply {
            text = "●"
            textSize = 13f
            setTextColor(Color.rgb(90, 220, 150))
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }
        titleView = TextView(this).apply {
            text = "Dynamic Island"
            textSize = 11f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        bodyView = TextView(this).apply {
            text = "Siap menerima notifikasi"
            textSize = 9f
            setTextColor(Color.LTGRAY)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        texts.addView(titleView)
        texts.addView(bodyView)
        pill.addView(iconView)
        pill.addView(texts)
        islandView = pill

        params = WindowManager.LayoutParams(
            dp(IslandSettings.width(this)),
            dp(IslandSettings.height(this)),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(IslandSettings.top(this@OverlayService))
        }
    }

    private fun attachIsland() {
        val view = islandView ?: return
        if (view.parent == null) {
            val layoutParams = params ?: return
            try {
                windowManager.addView(view, layoutParams)
            } catch (_: Exception) {
                // The user may have revoked overlay permission while the service was running.
                stopSelf()
            }
        }
    }

    private fun showIsland(title: String, body: String, type: String) {
        if (!IslandSettings.enabled(this)) return
        attachIsland()
        titleView?.text = title.ifBlank { labelFor(type) }
        bodyView?.text = body.ifBlank { labelFor(type) }
        iconView?.text = iconFor(type)
        islandView?.alpha = 0f
        islandView?.visibility = View.VISIBLE
        islandView?.animate()?.alpha(1f)?.setDuration(180)?.start()
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 6000)
    }

    private fun hideIsland() {
        islandView?.animate()?.alpha(0f)?.setDuration(220)?.withEndAction {
            islandView?.visibility = View.GONE
        }?.start()
    }

    private fun updateLayout() {
        val p = params ?: return
        p.width = dp(IslandSettings.width(this))
        p.height = dp(IslandSettings.height(this))
        p.y = dp(IslandSettings.top(this))
        try {
            islandView?.let { view ->
                if (view.parent != null) windowManager.updateViewLayout(view, p)
            }
        } catch (_: Exception) {}
    }

    private fun iconFor(type: String) = when (type) {
        "chat" -> "✉"
        "call" -> "☎"
        "music" -> "♫"
        "calendar" -> "▣"
        "timer" -> "◷"
        "battery" -> "⚡"
        else -> "●"
    }

    private fun labelFor(type: String) = when (type) {
        "chat" -> "Pesan baru"
        "call" -> "Panggilan"
        "music" -> "Musik"
        "calendar" -> "Agenda"
        "timer" -> "Timer"
        "battery" -> "Baterai"
        else -> "Notifikasi"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dynamic Island berjalan",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun serviceNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Dynamic Island aktif")
            .setContentText("Overlay siap menampilkan notifikasi")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        try {
            islandView?.let {
                if (it.parent != null) windowManager.removeView(it)
            }
        } catch (_: Exception) {}
        islandView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
