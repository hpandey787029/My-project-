package com.example.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private lateinit var button: Button
    private lateinit var status: TextView

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { requestCapture() } // record even if mic is denied (video only)

    private val capture = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val i = Intent(this, RecorderService::class.java)
                .setAction(RecorderService.ACTION_START)
                .putExtra(RecorderService.EXTRA_CODE, result.resultCode)
                .putExtra(RecorderService.EXTRA_DATA, result.data)
            startForegroundService(i)
            RecorderService.isRecording = true
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
        refreshUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER }
        button = Button(this).apply { setOnClickListener { toggle() } }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(status)
            addView(button)
        })
    }

    override fun onResume() { super.onResume(); refreshUi() }

    private fun toggle() {
        if (RecorderService.isRecording) {
            startService(Intent(this, RecorderService::class.java)
                .setAction(RecorderService.ACTION_STOP))
            RecorderService.isRecording = false
            refreshUi()
            Toast.makeText(this, "Saved to Movies/ScreenRecordings", Toast.LENGTH_LONG).show()
        } else {
            val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
            permissions.launch(needed.toTypedArray())
        }
    }

    private fun requestCapture() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        capture.launch(mpm.createScreenCaptureIntent())
    }

    private fun refreshUi() {
        val rec = RecorderService.isRecording
        status.text = if (rec) "● Recording…" else "Ready"
        button.text = if (rec) "Stop recording" else "Start recording"
    }
}
