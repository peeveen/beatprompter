package com.stevenfrew.beatprompter.event

/**
 * Base class for all events. Only contains the time that the event will occur.
 */
open class BaseEvent protected constructor(var mEventTime: Long)
