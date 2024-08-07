package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.song.line.Line

/**
 * A LineEvent tells the event processor to advance the song to the next line.
 */
class LineEvent(
	eventTime: Long,
	val line: Line
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent = LineEvent(eventTime + nanoseconds, line)
}