package com.stevenfrew.beatprompter.audio

import android.content.Context
import android.media.MediaPlayer
import com.stevenfrew.beatprompter.R
import java.io.File
import java.io.FileInputStream

class MediaPlayerAudioPlayer:AudioPlayer {
	private val mInternalPlayer:MediaPlayer
	constructor(context:Context){
		// Silence player
		mInternalPlayer=MediaPlayer.create(context, R.raw.silence).apply{
			setVolume(0.01f,0.01f)
			isLooping = true
			prepare()
		}
	}

	constructor(file: File, volume: Int){
		// File player
		mInternalPlayer=MediaPlayer().apply {
			FileInputStream(file.absolutePath)
			.use { stream ->
				setDataSource(stream.fd)
				seekTo(0)
				setVolume(0.01f * volume, 0.01f * volume)
				isLooping = false
				prepare()
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

	override fun setVolume(volume: Int) {
		mInternalPlayer.setVolume(volume*0.01f,volume*0.01f)
	}
}