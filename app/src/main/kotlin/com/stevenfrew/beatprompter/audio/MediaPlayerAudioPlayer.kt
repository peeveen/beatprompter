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
	private val mInternalPlayer: MediaPlayer
	private var mCurrentVolume: Int = 0

	constructor(context: Context) {
		// Silence player
		mInternalPlayer = MediaPlayer.create(context, R.raw.silence).apply {
			setVolume(0.01f, 0.01f)
			seekTo(0)
			isLooping = true
		}
	}

	constructor(file: File, volume: Int) {
		// File player
		mCurrentVolume = volume
		mInternalPlayer = MediaPlayer().apply {
			FileInputStream(file.absolutePath)
				.use { stream ->
					setDataSource(stream.fd)
					prepare()
					seekTo(0)
					setVolume(0.01f * volume, 0.01f * volume)
					isLooping = false
				}
		}
	}

	override fun seekTo(ms: Long) {
		mInternalPlayer.seekTo(ms.toInt())
	}

	override fun stop() {
		mInternalPlayer.stop()
	}

	override fun start() {
		mInternalPlayer.start()
	}

	override fun pause() {
		mInternalPlayer.pause()
	}

	override val isPlaying: Boolean
		get() = mInternalPlayer.isPlaying

	override val duration: Long
		get() = mInternalPlayer.duration.toLong()

	override fun release() {
		mInternalPlayer.release()
	}


	override var volume: Int
		get() = mCurrentVolume
		set(value) {
			mCurrentVolume = value
			mInternalPlayer.setVolume(value * 0.01f, value * 0.01f)
		}

}