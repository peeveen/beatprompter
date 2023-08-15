package com.stevenfrew.beatprompter.audio

/**
 * Interface for audio players.
 */
interface AudioPlayer {
	fun seekTo(ms: Long)
	fun stop()
	fun start()
	fun pause()
	val isPlaying: Boolean
	val duration: Long
	fun release()
	var volume: Int
}