package com.stevenfrew.beatprompter.audio

import android.content.Context
import java.io.File

/**
 * Factory for audio players. Currently capable of creating
 * MediaPlayer or ExoPlayer implementations.
 */
class AudioPlayerFactory(
	private val mType: AudioPlayerType,
	private val mContext: Context
) {
	fun create(file: File, volume: Int): AudioPlayer {
		return if (mType == AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(file, volume)
		else
			ExoPlayerAudioPlayer(mContext, file, volume)
	}

	fun createSilencePlayer(): AudioPlayer {
		return if (mType == AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(mContext)
		else
			ExoPlayerAudioPlayer(mContext)
	}
}