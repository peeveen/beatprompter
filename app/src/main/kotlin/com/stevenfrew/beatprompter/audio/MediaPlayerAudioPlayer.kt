package com.stevenfrew.beatprompter.audio

import android.content.Context
import android.media.MediaPlayer
import com.stevenfrew.beatprompter.R
import java.io.File
import java.io.FileInputStream

/**
 * MediaPlayer implementation of AudioPlayer interface.
 */
class MediaPlayerAudioPlayer : AudioPlayer {
	private val internalPlayer: MediaPlayer
	private var currentVolume: Int = 1

	constructor(context: Context) {
		// Silence player
		internalPlayer = MediaPlayer.create(context, R.raw.silence).apply {
			initialize(1, true)
		}
	}

	constructor(file: File, volume: Int) {
		// File player
		currentVolume = volume
		internalPlayer = MediaPlayer().apply {
			FileInputStream(file.absolutePath)
				.use { stream ->
					setDataSource(stream.fd)
					prepare()
					initialize(volume, false)
				}
		}
	}

	override fun seekTo(ms: Long) = internalPlayer.seekTo(ms.toInt())
	override fun stop() = internalPlayer.stop()
	override fun start() = internalPlayer.start()
	override fun pause() = internalPlayer.pause()
	override fun release() = internalPlayer.release()

	override val isPlaying: Boolean
		get() = internalPlayer.isPlaying

	override val duration: Long
		get() = internalPlayer.duration.toLong()

	override var volume: Int
		get() = currentVolume
		set(value) {
			currentVolume = value
			internalPlayer.setVolume(value * 0.01f, value * 0.01f)
		}

	companion object {
		private fun MediaPlayer.initialize(volume: Int, looping: Boolean) {
			setVolume(0.01f * volume, 0.01f * volume)
			seekTo(0)
			isLooping = looping
		}
	}
}