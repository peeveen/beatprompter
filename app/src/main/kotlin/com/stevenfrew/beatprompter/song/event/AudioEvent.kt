package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.cache.AudioFile

/**
 * An AudioEvent signals the event processor to start playing an audio file.
 */
class AudioEvent(
	eventTime: Long,
	val audioFile: AudioFile,
	val volume: Int,
	val isBackingTrack: Boolean
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent =
		AudioEvent(eventTime + nanoseconds, audioFile, volume, isBackingTrack)
}