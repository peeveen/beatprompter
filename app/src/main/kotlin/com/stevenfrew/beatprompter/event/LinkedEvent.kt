package com.stevenfrew.beatprompter.event

class LinkedEvent constructor(val mEvent:BaseEvent,private val mPrevEvent:LinkedEvent?){
    var mNextEvent:LinkedEvent?=null
    var mNextBeatEvent:BeatEvent?=null

    val mPrevLineEvent:LineEvent?
    val mPrevAudioEvent:AudioEvent?
    val mPrevBeatEvent: BeatEvent?

    val time:Long
        get()=mEvent.mEventTime

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

    fun cascadeSetNextEvents(nextEvent:LinkedEvent?=null,nextBeatEvent:BeatEvent?=null)
    {
        mNextEvent=nextEvent
        mNextBeatEvent=nextBeatEvent
        mPrevEvent?.cascadeSetNextEvents(this,
            if(mEvent is BeatEvent)
                mEvent
            else
                nextBeatEvent
        )
    }
}