package com.example.screenrecorder

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.app.NotificationCompat

class RecorderService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        @Volatile var isRecording = false
        private const val CHANNEL = "recording"
    }

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var uri: Uri? = null
    private var pfd: ParcelFileDescriptor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Screen recording", NotificationManager.IMPORTANCE_LOW)
        )
        val stopIntent = PendingIntent.getService(
            this, 0, Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Recording screen")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
        // Must be called before getMediaProjection() on Android 14+
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val code = intent.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED)
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        if (data == null) { stopSelf(); return }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(code, data).also {
            it.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopRecording() }
            }, null)
        }

        val m = resources.displayMetrics
        val w = m.widthPixels and 1.inv()
        val h = m.heightPixels and 1.inv()

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "rec_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ScreenRecordings")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        pfd = contentResolver.openFileDescriptor(uri!!, "w")

        val hasMic = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            if (hasMic) setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(pfd!!.fileDescriptor)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (hasMic) { setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setAudioEncodingBitRate(128_000); setAudioSamplingRate(44_100) }
            setVideoSize(w, h)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(8_000_000)
            prepare()
        }

        display = projection!!.createVirtualDisplay(
            "ScreenRecorder", w, h, m.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            recorder!!.surface, null, null
        )
        recorder!!.start()
        isRecording = true
    }

    private fun stopRecording() {
        try { recorder?.stop() } catch (_: RuntimeException) { /* stopped too quickly */ }
        recorder?.release(); recorder = null
        display?.release(); display = null
        projection?.stop(); projection = null
        pfd?.close(); pfd = null
        uri?.let {
            contentResolver.update(it, ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }, null, null)
        }
        uri = null
        isRecording = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (isRecording) stopRecording()
        super.onDestroy()
    }
}
