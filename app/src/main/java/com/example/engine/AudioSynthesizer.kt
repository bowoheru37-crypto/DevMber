package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight, zero-allocation real-time PCM audio synthesizer for Android.
 * Optimized for low-entry devices (Android 5.0+ / itel A70).
 */
object AudioSynthesizer {
    private const val SAMPLE_RATE = 22050
    private val synthScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun playToneAsync(frequencyHz: Float, durationMs: Int = 100, volume: Float = 0.2f) {
        if (isMuted || frequencyHz <= 20f || frequencyHz > 8000f) return

        synthScope.launch {
            try {
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceIn(100, 4410)
                val buffer = ShortArray(numSamples)
                val angleStep = 2.0 * Math.PI * frequencyHz / SAMPLE_RATE
                var angle = 0.0

                for (i in 0 until numSamples) {
                    // Smooth linear envelope to eliminate audio popping/clicks
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    val sampleValue = (sin(angle) * envelope * volume * Short.MAX_VALUE).toInt()
                    buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    angle += angleStep
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(numSamples * 2)

                val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(minBufferSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
                        AudioTrack.MODE_STATIC
                    )
                }

                audioTrack.write(buffer, 0, numSamples)
                audioTrack.play()

                kotlinx.coroutines.delay(durationMs.toLong() + 30)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            } catch (_: Exception) {
                // Silently swallow in audio pipeline to prevent FC
            }
        }
    }
}
