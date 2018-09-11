package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.song.Line

/**
 * A LineEvent tells the event processor to advance the song to the next line.
 */
class LineEvent constructor(eventTime:Long,val mLine: Line) : BaseEvent(eventTime)