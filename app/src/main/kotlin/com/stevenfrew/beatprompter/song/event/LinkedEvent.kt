package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.song.ScrollingMode

/**
 * During song parsing, the other events are finally encapsulated in LinkedEvent objects,
 * creating a linked-list style of collection. This allows us to easily find the previous
 * beat event, etc.
 */
class LinkedEvent	(eventList: List<BaseEvent>, private val mPrevEvent: LinkedEvent? = null) {
	val mEvent: BaseEvent
	val mNextBeatEvent: BeatEvent?
	val mPrevLineEvent: LineEvent?
	val mPrevAudioEvent: AudioEvent?
	val mPrevBeatEvent: BeatEvent?
	val mNextEvent: LinkedEvent?

	val time: Long
		get() = mEvent.mEventTime

	fun findLatestEventOnOrBefore(time: Long): LinkedEvent {
		var lastCheckedEvent = this
		var e: LinkedEvent = this
		while (true) {
			when {
				e.time > time -> {
					if (lastCheckedEvent.time < time)
						return lastCheckedEvent
					lastCheckedEvent = e
					if (e.mPrevEvent == null)
						return e.firstEventWithSameTime
					e = e.mPrevEvent!!
				}

				else -> { // e.mEventTime<=time
					if (lastCheckedEvent.time > time)
						return e.firstEventWithSameTime
					lastCheckedEvent = e
					if (e.mNextEvent == null)
						return e
					e = e.mNextEvent!!
				}
			}
		}
	}

	private val firstEventWithSameTime:LinkedEvent
		get() {
			var e=this
			while(e.mPrevEvent?.time==e.time)
				e=e.mPrevEvent!!
			return e
		}

	init {
		val eventsGroupedByTime = eventList.groupBy { it.mEventTime }.toSortedMap()
		val firstGroup = eventsGroupedByTime.getValue(eventsGroupedByTime.firstKey())
		mEvent = firstGroup.first()
		mPrevLineEvent = firstGroup.firstNotNullOfOrNull { it as? LineEvent } ?: mPrevEvent?.mPrevLineEvent
		mPrevAudioEvent = firstGroup.firstNotNullOfOrNull { it as? AudioEvent } ?: mPrevEvent?.mPrevAudioEvent
		mPrevBeatEvent = firstGroup.firstNotNullOfOrNull { it as? BeatEvent } ?: mPrevEvent?.mPrevBeatEvent
		mNextBeatEvent = eventList.filter { it.mEventTime > mEvent.mEventTime }.firstNotNullOfOrNull{ it as? BeatEvent }
		val otherEvents = eventList.takeLast(eventList.size - 1)
		mNextEvent = if (otherEvents.isNotEmpty()) LinkedEvent(otherEvents, this) else null
	}
}