package com.example.dynamicisland

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var widthValue: TextView
    private lateinit var heightValue: TextView
    private lateinit var topValue: TextView
    private var width = 230
    private var height = 38
    private var top = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        width = IslandSettings.width(this)
        height = IslandSettings.height(this)
        top = IslandSettings.top(this)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(17, 19, 26))
        }

        page.addView(TextView(this).apply {
            text = "Dynamic Island"
            textSize = 27f
            setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Atur tampilan pil dan aktifkan pembacaan notifikasi."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(20))
        })

        widthValue = addSlider(page, "Lebar Dynamic Island", 120, 340, width) {
            width = it
            widthValue.text = "$width dp"
            saveAndRefresh()
        }
        heightValue = addSlider(page, "Tinggi Dynamic Island", 28, 70, height) {
            height = it
            heightValue.text = "$height dp"
            saveAndRefresh()
        }
        topValue = addSlider(page, "Jarak dari atas layar", 0, 50, top) {
            top = it
            topValue.text = "$top dp"
            saveAndRefresh()
        }

        page.addView(button("1. Izinkan tampil di atas aplikasi lain") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            } else toast("Izin overlay sudah aktif")
        })

        page.addView(button("2. Aktifkan akses notifikasi") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })

        page.addView(button("3. Aktifkan Dynamic Island") {
            if (!Settings.canDrawOverlays(this)) {
                toast("Aktifkan izin overlay terlebih dahulu")
                return@button
            }
            IslandSettings.setEnabled(this, true)
            val intent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            toast("Dynamic Island diaktifkan")
        })

        page.addView(button("Matikan Dynamic Island") {
            IslandSettings.setEnabled(this, false)
            stopService(Intent(this, OverlayService::class.java))
            toast("Dynamic Island dimatikan")
        })

        page.addView(TextView(this).apply {
            text = "Catatan: akses notifikasi dan izin overlay harus diaktifkan secara manual. Isi pesan yang disembunyikan oleh aplikasi sumber tidak dapat dibaca."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(18), 0, 0)
        })

        setContentView(page)
    }

    private fun addSlider(
        parent: LinearLayout,
        label: String,
        min: Int,
        max: Int,
        initial: Int,
        onChanged: (Int) -> Unit
    ): TextView {
        parent.addView(TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), 0, dp(3))
        })
        val valueText = TextView(this).apply {
            text = "$initial dp"
            textSize = 13f
            setTextColor(Color.rgb(180, 165, 255))
        }
        parent.addView(valueText)
        parent.addView(SeekBar(this).apply {
            max = max - min
            progress = initial - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onChanged(progress + min)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })
        return valueText
    }

    private fun button(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(9) }
        }

    private fun saveAndRefresh() {
        IslandSettings.save(this, width, height, top)
        sendBroadcast(Intent(OverlayService.ACTION_SETTINGS).setPackage(packageName))
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
