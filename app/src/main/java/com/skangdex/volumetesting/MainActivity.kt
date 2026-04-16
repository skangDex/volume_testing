package com.skangdex.volumetesting

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var autoPlayCheckbox: CheckBox

    private val handler = Handler(Looper.getMainLooper())
    private val loopRunnables = mutableMapOf<Int, Runnable>()
    private val toneGenerators = mutableMapOf<Int, ToneGenerator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AudioManager::class.java)
        autoPlayCheckbox = findViewById(R.id.autoPlayCheckbox)

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
        controller.seekBar.max = audioManager.getStreamMaxVolume(controller.streamType)
        controller.seekBar.progress = audioManager.getStreamVolume(controller.streamType)

        controller.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                audioManager.setStreamVolume(controller.streamType, progress, 0)
                if (autoPlayCheckbox.isChecked) {
                    playSample(controller.streamType, controller.toneType)
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        controller.toggleButton.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                startLoop(controller.streamType, controller.toneType)
            } else {
                stopLoop(controller.streamType)
            }
            buttonView.text = if (isChecked) getString(R.string.stop) else getString(R.string.play)
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
