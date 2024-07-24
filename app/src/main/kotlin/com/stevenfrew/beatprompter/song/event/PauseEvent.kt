package com.stevenfrew.beatprompter.song.event

/**
 * A PauseEvent tells the event processor to wait for a certain amount of time, showing a
 * gauge onscreen for the duration of the pause.
 */
class PauseEvent(
	eventTime: Long,
	val beats: Int,
	val beat: Int
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent =
		PauseEvent(eventTime + nanoseconds, beats, beat)
}