package com.stevenfrew.beatprompter.song.event

/**
 * An ClickEvent signals the processor to make a click sound (metronome).
 */
class ClickEvent(
	eventTime: Long,
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent =
		ClickEvent(eventTime + nanoseconds)
}