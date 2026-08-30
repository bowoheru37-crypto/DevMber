package com.example.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Algorithmic Binary Bytebeat & Chiptune DSP Audio Synthesizer.
 * Inspired by Ville-Matias Heikkilä (viznut), IBNIZ, and classic demoscene sound engines.
 * Generates rich retro musical compositions directly from 1-line bitwise binary mathematical expressions (t-formulas).
 * Zero memory allocation during streaming loop for itel A70.
 */
enum class BytebeatFormula(val title: String, val formulaStr: String) {
    SIERPINSKI_HARMONY("Sierpinski Cyber Wave", "t * (t >> 9 | t >> 13) & 32"),
    ANCIENT_AUTOMATON("Ancient 8-Bit Machine", "(t * 5 & t >> 7) | (t * 3 & t >> 10)"),
    COSMIC_PULSE("Cosmic Bitwise Pulse", "t * ((t >> 12 | t >> 8) & 63 & t >> 4)"),
    GLITCH_MATRIX("Glitch Synth Matrix", "(t * (t >> 8 | t >> 9) & 46 & t >> 8) ^ (t & t >> 13 | t >> 6)")
}

class BytebeatDspSynthesizer(val sampleRate: Int = 8000) {
    private val synthScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var playbackJob: Job? = null
    private var isPlaying = false
    private var timeStep: Long = 0

    var currentFormula: BytebeatFormula = BytebeatFormula.SIERPINSKI_HARMONY

    // Visualizer ring buffer (128 samples for 60fps UI drawing)
    val visualizerBuffer = FloatArray(128) { 0f }
    private var visualizerIdx = 0

    fun isPlaying(): Boolean = isPlaying

    fun play(formula: BytebeatFormula = currentFormula) {
        if (isPlaying) stop()

        currentFormula = formula
        isPlaying = true
        timeStep = 0

        playbackJob = synthScope.launch {
            val bufferSize = 1024
            val pcmBuffer = ByteArray(bufferSize)

            val minAudioBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_8BIT
            ).coerceAtLeast(bufferSize * 2)

            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minAudioBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_8BIT,
                    minAudioBuf,
                    AudioTrack.MODE_STREAM
                )
            }

            try {
                track.play()
                while (isActive && isPlaying) {
                    for (i in 0 until bufferSize) {
                        val t = timeStep++
                        val sampleByte = evaluateSample(t, currentFormula)
                        pcmBuffer[i] = sampleByte.toByte()

                        // Feed visualizer
                        if (i % 8 == 0) {
                            visualizerBuffer[visualizerIdx] = (sampleByte - 128) / 128f
                            visualizerIdx = (visualizerIdx + 1) % visualizerBuffer.size
                        }
                    }
                    track.write(pcmBuffer, 0, bufferSize)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
    }

    companion object {
        /**
         * Fast bitwise evaluation of Bytebeat expressions
         */
        fun evaluateSample(t: Long, formula: BytebeatFormula): Int {
            val sample = when (formula) {
                BytebeatFormula.SIERPINSKI_HARMONY -> (t * (t shr 9 or (t shr 13)) and 32)
                BytebeatFormula.ANCIENT_AUTOMATON -> ((t * 5 and (t shr 7)) or (t * 3 and (t shr 10)))
                BytebeatFormula.COSMIC_PULSE -> (t * ((t shr 12 or (t shr 8)) and 63 and (t shr 4)))
                BytebeatFormula.GLITCH_MATRIX -> ((t * (t shr 8 or (t shr 9)) and 46 and (t shr 8)) xor (t and (t shr 13) or (t shr 6)))
            }
            return (sample.toInt() and 0xFF)
        }
    }
}
