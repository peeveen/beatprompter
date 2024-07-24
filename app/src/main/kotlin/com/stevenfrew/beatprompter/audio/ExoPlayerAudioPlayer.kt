package com.stevenfrew.beatprompter.audio

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.stevenfrew.beatprompter.R
import java.io.File

/**
 * ExoPlayer implementation of AudioPlayer interface.
 */
class ExoPlayerAudioPlayer private constructor(
	context: Context,
	uri: Uri,
	vol: Int,
	looping: Boolean
) : AudioPlayer {
	@OptIn(UnstableApi::class)
	private val internalPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
		setSeekParameters(SeekParameters.EXACT)
		setMediaItem(MediaItem.fromUri(uri))
		seekTo(0)
		volume = 0.01f * vol
		repeatMode = if (looping) ExoPlayer.REPEAT_MODE_ALL else ExoPlayer.REPEAT_MODE_OFF
		prepare()
	}

	constructor(context: Context) : this(
		context,
		Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path(R.raw.silence.toString())
			.build(),
		1,
		true
	)

	constructor(context: Context, file: File, volume: Int) : this(
		context,
		Uri.fromFile(file),
		volume,
		false
	)

	override fun seekTo(ms: Long) = internalPlayer.seekTo(ms)
	override fun stop() = internalPlayer.stop()
	override fun start() = internalPlayer.play()
	override fun pause() = internalPlayer.pause()

	override fun release() = internalPlayer.release()

	override val isPlaying: Boolean
		get() = internalPlayer.isPlaying

	override val duration: Long
		get() = internalPlayer.duration

	override var volume: Int
		get() = (internalPlayer.volume * 100.0).toInt()
		set(value) {
			internalPlayer.volume = value * 0.01f
		}
}