package com.stevenfrew.beatprompter.song.event

/**
 * Base class for all events. Only contains the time that the event will occur.
 */
open class BaseEvent protected constructor(val mEventTime: Long)
