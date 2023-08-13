package com.stevenfrew.beatprompter.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.stevenfrew.beatprompter.R
import java.io.File

class ExoPlayerAudioPlayer : AudioPlayer {
	private val mInternalPlayer: ExoPlayer

	@OptIn(UnstableApi::class)
	constructor(context: Context) : this(
		context,
		RawResourceDataSource.buildRawResourceUri(R.raw.silence),
		1,
		true
	)

	constructor(context: Context, file: File, volume: Int) : this(
		context,
		Uri.fromFile(file),
		volume,
		false
	)

	@OptIn(UnstableApi::class)
	private constructor(context: Context, uri: Uri, vol: Int, looping: Boolean) {
		mInternalPlayer = ExoPlayer.Builder(context).build().apply {
			setSeekParameters(SeekParameters.CLOSEST_SYNC)
			setMediaItem(MediaItem.fromUri(uri))
			seekTo(0)
			volume = 0.01f * vol
			repeatMode = if (looping) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
			prepare()
		}
	}

	override fun seekTo(ms: Long) {
		mInternalPlayer.seekTo(ms)
	}

	override fun stop() {
		mInternalPlayer.stop()
	}

	override fun start() {
		mInternalPlayer.play()
	}

	override fun pause() {
		mInternalPlayer.pause()
	}

	override val isPlaying: Boolean
		get() = mInternalPlayer.isPlaying

	override val duration: Long
		get() = mInternalPlayer.duration

	override fun release() {
		mInternalPlayer.release()
	}

	override fun setVolume(volume: Int) {
		mInternalPlayer.volume = volume * 0.01f
	}
}