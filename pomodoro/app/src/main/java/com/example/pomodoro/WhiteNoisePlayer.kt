package com.example.pomodoro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Generates and plays white / brown noise with [AudioTrack] — no audio asset required.
 * White noise = random samples; brown noise = a leaky random walk (softer, "rain"-like).
 */
class WhiteNoisePlayer {

    @Volatile private var playing = false
    private var track: AudioTrack? = null
    private var worker: Thread? = null

    fun start(brown: Boolean) {
        if (playing) return
        playing = true

        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val at = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = at
        at.play()

        worker = thread(name = "white-noise") {
            val buf = ShortArray(minBuf)
            val rnd = Random(System.nanoTime())
            var last = 0f
            while (playing) {
                for (i in buf.indices) {
                    val white = rnd.nextFloat() * 2f - 1f
                    val sample = if (brown) {
                        last = (last + 0.02f * white) / 1.02f
                        last * 11000f
                    } else {
                        white * 6000f
                    }
                    buf[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                }
                try {
                    if (at.write(buf, 0, buf.size) < 0) break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun stop() {
        playing = false
        worker?.join(300)
        worker = null
        try { track?.pause(); track?.flush(); track?.stop() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        track = null
    }
}
