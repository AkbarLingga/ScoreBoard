package com.pointrecord.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Suara peluit wasit yang dibuat langsung dari gelombang, tanpa file audio. */
class Whistle {
    private var track: AudioTrack? = null

    fun play() {
        try {
            val t = track ?: create().also { track = it }
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: RuntimeException) {
            release() // Audio sedang tidak tersedia; tap berikutnya mencoba lagi.
        }
    }

    fun release() {
        track?.release()
        track = null
    }

    private fun create(): AudioTrack {
        val n = RATE * 7 / 10
        val pcm = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val time = i.toDouble() / RATE
            val trill = sin(2 * PI * 38 * time)
            phase += 2 * PI * (2750 + 170 * trill) / RATE
            val attack = min(1.0, time / 0.02)
            val decay = min(1.0, (n - i).toDouble() / RATE / 0.08)
            val wave = sin(phase) * 0.8 + sin(phase * 2) * 0.12
            pcm[i] = (wave * (0.75 + 0.25 * trill) * attack * decay * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(n * 2)
            .build()
        t.write(pcm, 0, n)
        return t
    }

    private companion object {
        const val RATE = 44_100
    }
}
