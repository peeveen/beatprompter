package com.stevenfrew.beatprompter.audio

interface AudioPlayer {
	fun seekTo(ms: Long)
	fun stop()
	fun start()
	fun pause()
	val isPlaying: Boolean
	val duration: Long
	fun release()
	fun setVolume(volume: Int)
}