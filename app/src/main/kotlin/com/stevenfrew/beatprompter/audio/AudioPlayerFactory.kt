package com.stevenfrew.beatprompter.audio

import android.content.Context
import java.io.File

/**
 * Factory for audio players. Currently capable of creating
 * MediaPlayer or ExoPlayer implementations.
 */
class AudioPlayerFactory(
	private val type: AudioPlayerType,
	private val context: Context
) {
	fun create(file: File, volume: Int): AudioPlayer =
		if (type == AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(file, volume)
		else
			ExoPlayerAudioPlayer(context, file, volume)

	fun createSilencePlayer(): AudioPlayer =
		if (type == AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(context)
		else
			ExoPlayerAudioPlayer(context)
}