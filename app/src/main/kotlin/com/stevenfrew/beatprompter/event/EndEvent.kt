package com.stevenfrew.beatprompter.event

/**
 * The EndEvent only matters in beat mode. It tells the event processor to stop the song.
 */
class EndEvent(time: Long) : BaseEvent(time)
