package com.tessera.mockmbway.data

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object Sounder {

    private const val TAG = "Sounder"
    private const val VOLUME = 100
    private const val BEEP_DURATION_MS = 200

    private var generator: ToneGenerator? = null

    fun init() {
        try {
            generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ToneGenerator", e)
        }
    }

    fun destroy() {
        try {
            generator?.release()
        } catch (e: Exception) {
            Log.d(TAG, "release: ${e.message}")
        }
        generator = null
    }

    fun incoming() = play(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)

    private fun play(tone: Int, durationMs: Int) {
        try {
            generator?.startTone(tone, durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play tone $tone", e)
        }
    }
}
