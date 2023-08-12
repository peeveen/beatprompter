package com.stevenfrew.beatprompter.audio

import android.content.Context
import java.io.File

class AudioPlayerFactory(private val mType:AudioPlayerType,
                         private val mContext:Context) {
	fun create(file:File, volume:Int):AudioPlayer{
		return if(mType==AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(file, volume)
		else
			ExoPlayerAudioPlayer(mContext,file,volume)

	}

	fun createSilencePlayer(): AudioPlayer {
		return if(mType==AudioPlayerType.MediaPlayer)
			MediaPlayerAudioPlayer(mContext)
		else
			ExoPlayerAudioPlayer(mContext)
	}
}