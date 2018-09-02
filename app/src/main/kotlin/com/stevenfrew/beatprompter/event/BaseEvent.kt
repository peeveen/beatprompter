package com.stevenfrew.beatprompter.event

open class BaseEvent protected constructor(var mEventTime: Long // Time at which this event occurs (in nanoseconds AFTER the globalStart time)
) {
    var mPrevEvent: BaseEvent? = null
    var mNextEvent: BaseEvent? = null
    var mPrevAudioEvent: AudioEvent? = null
    var mPrevBeatEvent: BeatEvent? = null
    var mPrevLineEvent: LineEvent? = null

    val nextBeatEvent: BeatEvent?
        get() {
            var event = mNextEvent
            if (mNextEvent == null)
                return null
            do {
                if (event is BeatEvent)
                    return event
                event = event!!.mNextEvent
            } while (event != null)
            return null
        }

    fun add(event: BaseEvent) {
        mNextEvent = event
        event.mPrevEvent = this
        event.mPrevBeatEvent = mPrevBeatEvent
        event.mPrevAudioEvent = mPrevAudioEvent
        event.mPrevLineEvent = mPrevLineEvent
        //event.mPrevScrollEvent=mPrevScrollEvent;
        when (event) {
            is BeatEvent -> event.mPrevBeatEvent = event
            is AudioEvent -> event.mPrevAudioEvent = event
            is LineEvent -> event.mPrevLineEvent = event
            //if(event instanceof ScrollEvent)
            //     event.mPrevScrollEvent=(ScrollEvent)event;
        }
        //if(event instanceof ScrollEvent)
        //     event.mPrevScrollEvent=(ScrollEvent)event;
    }

    private fun insertAfter(event: BaseEvent) {
        val mTempNextEvent = mNextEvent
        mNextEvent = event
        event.mNextEvent = mTempNextEvent
        if (mTempNextEvent != null)
            mTempNextEvent.mPrevEvent = event
        event.mPrevEvent = this

        if (event is BeatEvent) {
            event.mPrevBeatEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is BeatEvent) {
                currentEvent.mPrevBeatEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevBeatEvent = mPrevBeatEvent

        if (event is AudioEvent) {
            event.mPrevAudioEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is AudioEvent) {
                currentEvent.mPrevAudioEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevAudioEvent = mPrevAudioEvent

        if (event is LineEvent) {
            event.mPrevLineEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is LineEvent) {
                currentEvent.mPrevLineEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevLineEvent = mPrevLineEvent
    }

    fun findLatestEventOnOrBefore(time: Long): BaseEvent {
        var lastCheckedEvent = this
        var e: BaseEvent = this
        while (true) {
            when {
                e.mEventTime > time -> {
                    if (lastCheckedEvent.mEventTime < time)
                        return lastCheckedEvent
                    lastCheckedEvent = e
                    if (e.mPrevEvent == null)
                        return e
                    e = e.mPrevEvent!!
                }
                else -> { // e.mEventTime<=time
                    if (lastCheckedEvent.mEventTime > time)
                        return e
                    lastCheckedEvent = e
                    if (e.mNextEvent == null)
                        return e
                    e = e.mNextEvent!!
                }
            }
        }
    }

    fun insertEvent(event: BaseEvent) {
        val eventBefore = findLatestEventOnOrBefore(event.mEventTime)
        eventBefore.insertAfter(event)
    }

    fun remove() {
        val eventBefore = mPrevEvent
        val eventAfter = mNextEvent
        if (mPrevEvent != null)
            mPrevEvent!!.mNextEvent = eventAfter
        if (mNextEvent != null) {
            mNextEvent!!.mPrevEvent = eventBefore
            if (eventBefore != null) {
                if (mNextEvent !is AudioEvent)
                    mNextEvent!!.mPrevAudioEvent = eventBefore.mPrevAudioEvent
                if (mNextEvent !is LineEvent)
                    mNextEvent!!.mPrevLineEvent = eventBefore.mPrevLineEvent
                if (mNextEvent !is BeatEvent)
                    mNextEvent!!.mPrevBeatEvent = eventBefore.mPrevBeatEvent
            } else {
                if (mNextEvent !is AudioEvent)
                    mNextEvent!!.mPrevAudioEvent = null
                if (mNextEvent !is LineEvent)
                    mNextEvent!!.mPrevLineEvent = null
                if (mNextEvent !is BeatEvent)
                    mNextEvent!!.mPrevBeatEvent = null
            }
        }
    }
}
