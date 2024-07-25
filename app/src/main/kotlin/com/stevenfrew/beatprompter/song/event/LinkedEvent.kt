package com.stevenfrew.beatprompter.song.event

/**
 * During song parsing, the other events are finally encapsulated in LinkedEvent objects,
 * creating a linked-list style of collection. This allows us to easily find the previous
 * beat event, etc.
 */
class LinkedEvent(eventList: List<BaseEvent>, private val previousEvent: LinkedEvent? = null) {
	private val nextBeatEvent: BeatEvent?

	val event: BaseEvent
	val previousLineEvent: LineEvent?
	val previousAudioEvent: AudioEvent?
	val previousBeatEvent: BeatEvent?
	val nextEvent: LinkedEvent?

	val time: Long
		get() = event.eventTime

	fun findLatestEventOnOrBefore(time: Long): LinkedEvent {
		var lastCheckedEvent = this
		var e: LinkedEvent = this
		while (true) {
			when {
				e.time > time -> {
					if (lastCheckedEvent.time < time)
						return lastCheckedEvent
					lastCheckedEvent = e
					if (e.previousEvent == null)
						return e.firstEventWithSameTime
					e = e.previousEvent!!
				}

				else -> { // e.mEventTime<=time
					if (lastCheckedEvent.time > time)
						return e.firstEventWithSameTime
					lastCheckedEvent = e
					if (e.nextEvent == null)
						return e
					e = e.nextEvent!!
				}
			}
		}
	}

	private val firstEventWithSameTime: LinkedEvent
		get() {
			var e = this
			while (e.previousEvent?.time == e.time)
				e = e.previousEvent!!
			return e
		}

	init {
		val eventsGroupedByTime = eventList.groupBy { it.eventTime }.toSortedMap()
		val firstGroup = eventsGroupedByTime.getValue(eventsGroupedByTime.firstKey())
		event = firstGroup.first()
		previousLineEvent =
			firstGroup.firstNotNullOfOrNull { it as? LineEvent } ?: previousEvent?.previousLineEvent
		previousAudioEvent =
			firstGroup.firstNotNullOfOrNull { it as? AudioEvent } ?: previousEvent?.previousAudioEvent
		previousBeatEvent =
			firstGroup.firstNotNullOfOrNull { it as? BeatEvent } ?: previousEvent?.previousBeatEvent
		nextBeatEvent = eventList.filter { it.eventTime > event.eventTime }
			.firstNotNullOfOrNull { it as? BeatEvent }
		val otherEvents = eventList.takeLast(eventList.size - 1)
		nextEvent = if (otherEvents.isNotEmpty()) LinkedEvent(otherEvents, this) else null
	}
}