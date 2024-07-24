package com.stevenfrew.beatprompter.song.event

/**
 * Base class for all events. Only contains the time that the event will occur.
 */
abstract class BaseEvent protected constructor(val eventTime: Long) {
	abstract fun offset(nanoseconds: Long): BaseEvent
}