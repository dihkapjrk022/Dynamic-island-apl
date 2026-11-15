package com.example.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Dynamic Island Android", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Tekan tombol di bawah untuk mengaktifkan widget melayang di atas layar.")
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(onClick = {
                        // 1. Cek dan minta izin overlay jika belum diaktifkan
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        } else {
                            // 2. Jalankan Service Dynamic Island jika izin sudah ada
                            val serviceIntent = Intent(this@MainActivity, DynamicIslandService::class.java)
                            startService(serviceIntent)
                        }
                    }) {
                        Text(text = "Aktifkan Dynamic Island")
                    }
                }
            }
        }
    }
}
