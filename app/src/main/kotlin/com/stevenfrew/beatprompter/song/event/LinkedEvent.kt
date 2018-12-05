package com.stevenfrew.beatprompter.song.event

/**
 * During song parsing, the other events are finally encapsulated in LinkedEvent objects,
 * creating a linked-list style of collection. This allows us to easily find the previous
 * beat event, etc.
 */
class LinkedEvent constructor(val mEvent: BaseEvent,
                              val mPrevEvent: LinkedEvent?) {
    var mNextEvent: LinkedEvent? = null
    var mNextBeatEvent: BeatEvent? = null

    val mPrevLineEvent: LineEvent?
    val mPrevAudioEvent: AudioEvent?
    val mPrevBeatEvent: BeatEvent?

    val time: Long
        get() = mEvent.mEventTime

    init {
        mPrevLineEvent = mEvent as? LineEvent ?: mPrevEvent?.mPrevLineEvent
        mPrevAudioEvent = mEvent as? AudioEvent ?: mPrevEvent?.mPrevAudioEvent
        mPrevBeatEvent = mEvent as? BeatEvent ?: mPrevEvent?.mPrevBeatEvent
    }

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
                        return e
                    e = e.mPrevEvent!!
                }
                else -> { // e.mEventTime<=time
                    if (lastCheckedEvent.time > time)
                        return e
                    lastCheckedEvent = e
                    if (e.mNextEvent == null)
                        return e
                    e = e.mNextEvent!!
                }
            }
        }
    }
}