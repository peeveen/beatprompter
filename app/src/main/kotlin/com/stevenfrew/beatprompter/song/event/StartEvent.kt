package com.stevenfrew.beatprompter.song.event

/**
 * The StartEvent simply gives the song a "current event" to start with.
 */
class StartEvent(time: Long = 0) : BaseEvent(time) {
	override fun offset(nanoseconds: Long): BaseEvent = StartEvent(eventTime + nanoseconds)
}
