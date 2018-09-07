package com.stevenfrew.beatprompter.event

/**
 * A PauseEvent tells the event processor to wait for a certain amount of time, showing a
 * gauge onscreen for the duration of the pause.
 */
class PauseEvent(eventTime: Long, var mBeats: Int, var mBeat: Int) : BaseEvent(eventTime)