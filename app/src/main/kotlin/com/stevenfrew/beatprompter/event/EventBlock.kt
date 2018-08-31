package com.stevenfrew.beatprompter.event

/**
 * An "event block" is simply a list of events, in chronological order, and a time that marks the point
 * at which the block ends. Note that the end time is not necessarily the same as the time of the last
 * event. For example, a block of five beat events (where each beat last n nanoseconds) will contain
 * five events with the times of n*0, n*1, n*2, n*3, n*4, and the end time will be n*5, as a "beat event"
 * actually covers the duration of the beat.
 */
data class EventBlock(val mEvents:List<BaseEvent>,val mBlockEndTime:Long)