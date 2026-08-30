package com.octomind.booksreader.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.octomind.booksreader.domain.AmbientSoundscape
import java.io.Closeable
import kotlin.concurrent.thread
import kotlin.math.max

internal class AmbientAudioController(context: Context) : Closeable {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    private val focusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(::handleAudioFocusChange, mainHandler)
            .build()

    @Volatile
    private var playbackRequested = false

    @Volatile
    private var playbackGeneration = 0

    @Volatile
    private var soundscape = AmbientSoundscape.CONCENTRATION

    @Volatile
    private var volumePercent = DEFAULT_VOLUME_PERCENT

    @Volatile
    private var audioTrack: AudioTrack? = null

    var onPlaybackChanged: ((Boolean) -> Unit)? = null

    @Synchronized
    fun play(
        soundscape: AmbientSoundscape,
        volumePercent: Int,
    ) {
        update(soundscape, volumePercent)
        if (playbackRequested) return
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            notifyPlaybackChanged(false)
            return
        }

        playbackRequested = true
        playbackGeneration += 1
        val generation = playbackGeneration
        notifyPlaybackChanged(true)
        thread(name = AUDIO_THREAD_NAME) { streamAudio(generation) }
    }

    fun update(
        soundscape: AmbientSoundscape,
        volumePercent: Int,
    ) {
        this.soundscape = soundscape
        this.volumePercent = volumePercent.coerceIn(MINIMUM_VOLUME_PERCENT, MAXIMUM_VOLUME_PERCENT)
    }

    @Synchronized
    fun pause() {
        val wasPlaying = playbackRequested
        playbackRequested = false
        playbackGeneration += 1
        runCatching { audioTrack?.pause() }
        audioManager.abandonAudioFocusRequest(focusRequest)
        if (wasPlaying) notifyPlaybackChanged(false)
    }

    override fun close() {
        pause()
        onPlaybackChanged = null
    }

    private fun streamAudio(generation: Int) {
        val track =
            runCatching { createAudioTrack() }.getOrNull() ?: run {
                finishGeneration(generation, null)
                return
            }
        synchronized(this) {
            if (!playbackRequested || generation != playbackGeneration) {
                track.release()
                return
            }
            audioTrack = track
        }

        val generator = AmbientWaveformGenerator()
        val samples = ShortArray(STREAM_BUFFER_SAMPLES)
        try {
            track.play()
            while (playbackRequested && generation == playbackGeneration) {
                generator.fill(samples, soundscape, volumePercent)
                if (track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING) <= 0) break
            }
        } catch (_: IllegalStateException) {
            // El dispositivo puede retirar la salida de audio durante una interrupción.
        } finally {
            runCatching { track.stop() }
            track.release()
            finishGeneration(generation, track)
        }
    }

    @Synchronized
    private fun finishGeneration(
        generation: Int,
        track: AudioTrack?,
    ) {
        if (audioTrack === track) audioTrack = null
        if (playbackRequested && generation == playbackGeneration) {
            playbackRequested = false
            audioManager.abandonAudioFocusRequest(focusRequest)
            notifyPlaybackChanged(false)
        }
    }

    private fun createAudioTrack(): AudioTrack? {
        val format =
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        val minimumBufferBytes =
            AudioTrack.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        val track =
            AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(max(minimumBufferBytes, STREAM_BUFFER_BYTES))
                .build()
        return track.takeIf { it.state == AudioTrack.STATE_INITIALIZED }
            ?: run {
                track.release()
                null
            }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        if (focusChange != AudioManager.AUDIOFOCUS_GAIN) pause()
    }

    private fun notifyPlaybackChanged(isPlaying: Boolean) {
        val callback = onPlaybackChanged ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback(isPlaying)
        } else {
            mainHandler.post { onPlaybackChanged?.invoke(isPlaying) }
        }
    }

    private companion object {
        const val AUDIO_SAMPLE_RATE = 22_050
        const val STREAM_BUFFER_SAMPLES = 2_048
        const val BYTES_PER_SAMPLE = 2
        const val STREAM_BUFFER_BYTES = STREAM_BUFFER_SAMPLES * BYTES_PER_SAMPLE * 4
        const val DEFAULT_VOLUME_PERCENT = 15
        const val MINIMUM_VOLUME_PERCENT = 0
        const val MAXIMUM_VOLUME_PERCENT = 50
        const val AUDIO_THREAD_NAME = "OctomindAmbientAudio"
    }
}
