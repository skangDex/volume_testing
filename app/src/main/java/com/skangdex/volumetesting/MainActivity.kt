package com.skangdex.volumetesting

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.CheckBox
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var autoPlayCheckbox: CheckBox

    private val handler = Handler(Looper.getMainLooper())
    private val loopRunnables = mutableMapOf<Int, Runnable>()
    private val toneGenerators = mutableMapOf<Int, ToneGenerator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AudioManager::class.java)
        notificationManager = getSystemService(NotificationManager::class.java)
        autoPlayCheckbox = findViewById(R.id.autoPlayCheckbox)

        checkNotificationPolicyAccess()

        val controllers = listOf(
            StreamController(
                streamType = AudioManager.STREAM_MUSIC,
                seekBar = findViewById(R.id.mediaSeekBar),
                toggleButton = findViewById(R.id.mediaToggle),
                toneType = ToneGenerator.TONE_PROP_ACK
            ),
            StreamController(
                streamType = AudioManager.STREAM_RING,
                seekBar = findViewById(R.id.ringSeekBar),
                toggleButton = findViewById(R.id.ringToggle),
                toneType = ToneGenerator.TONE_SUP_RINGTONE
            ),
            StreamController(
                streamType = AudioManager.STREAM_ALARM,
                seekBar = findViewById(R.id.alarmSeekBar),
                toggleButton = findViewById(R.id.alarmToggle),
                toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            ),
            StreamController(
                streamType = AudioManager.STREAM_NOTIFICATION,
                seekBar = findViewById(R.id.notificationSeekBar),
                toggleButton = findViewById(R.id.notificationToggle),
                toneType = ToneGenerator.TONE_PROP_BEEP
            )
        )

        controllers.forEach { controller ->
            initializeController(controller)
        }
    }

    private fun initializeController(controller: StreamController) {
        val maxSystemVolume = audioManager.getStreamMaxVolume(controller.streamType)
        
        // Use 0-100 for a smooth UI experience
        controller.seekBar.max = 100
        
        // Map current system volume to 0-100 scale
        val currentVolume = audioManager.getStreamVolume(controller.streamType)
        controller.seekBar.progress = (currentVolume * 100) / maxSystemVolume

        controller.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                android.util.Log.i("VolumeTesting", "onProgressChanged: Stream=${controller.streamType}, progress=$progress, fromUser=$fromUser")
                if (!fromUser) return
                
                // Use float math for precise mapping and round to nearest integer
                val maxVol = maxSystemVolume.toFloat()
                var targetVolume = Math.round((progress.toFloat() * maxVol) / 100f)
                
                // Ensure that if progress is > 0, volume is at least 1 (unless max is 0)
                if (progress > 0 && targetVolume == 0 && maxSystemVolume > 0) {
                    targetVolume = 1
                }

                // Log the change for debugging
                val logMsg = "Stream: ${controller.streamType}, Progress: $progress, Target: $targetVolume / $maxSystemVolume, fromUser: $fromUser"
                android.util.Log.i("VolumeTesting", logMsg)
                
                // Set system volume with UI feedback and play system sound feedback
                try {
                    audioManager.setStreamVolume(
                        controller.streamType, 
                        targetVolume,
                        AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND or AudioManager.FLAG_ALLOW_RINGER_MODES
                    )
                } catch (e: Exception) {
                    android.util.Log.e("VolumeTesting", "Error setting volume", e)
                }

                if (autoPlayCheckbox.isChecked) {
                    playSample(controller.streamType, controller.toneType)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        controller.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startLoop(controller.streamType, controller.toneType)
            } else {
                stopLoop(controller.streamType)
            }
        }
    }

    private fun playSample(streamType: Int, toneType: Int) {
        val toneGenerator = toneGenerators.getOrPut(streamType) { ToneGenerator(streamType, 100) }
        toneGenerator.startTone(toneType, 160)
    }

    private fun startLoop(streamType: Int, toneType: Int) {
        stopLoop(streamType)
        val runnable = object : Runnable {
            override fun run() {
                playSample(streamType, toneType)
                handler.postDelayed(this, 500L)
            }
        }
        loopRunnables[streamType] = runnable
        handler.post(runnable)
    }

    private fun stopLoop(streamType: Int) {
        loopRunnables.remove(streamType)?.let { handler.removeCallbacks(it) }
        toneGenerators[streamType]?.stopTone()
    }

    private fun checkNotificationPolicyAccess() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Please grant DND access to change Ring/Notification volume", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loopRunnables.values.forEach { handler.removeCallbacks(it) }
        loopRunnables.clear()
        toneGenerators.values.forEach { it.release() }
        toneGenerators.clear()
    }

    private data class StreamController(
        val streamType: Int,
        val seekBar: VerticalSeekBar,
        val toggleButton: ToggleButton,
        val toneType: Int
    )
}
