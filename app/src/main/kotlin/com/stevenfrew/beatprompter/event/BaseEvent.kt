package com.stevenfrew.beatprompter.event

open class BaseEvent protected constructor(var mEventTime: Long // Time at which this event occurs (in nanoseconds AFTER the globalStart time)
) {
    var mPrevEvent: BaseEvent? = null
    var mNextEvent: BaseEvent? = null
    var mPrevColorEvent: ColorEvent? = null
    var mPrevTrackEvent: TrackEvent? = null
    //ScrollEvent mPrevScrollEvent;
    var mPrevBeatEvent: BeatEvent? = null
    var mPrevLineEvent: LineEvent? = null

    val lastEvent: BaseEvent
        get() {
            var e: BaseEvent? = this
            while (e!!.mNextEvent != null)
                e = e.mNextEvent
            return e
        }

    val firstBeatEvent: BeatEvent?
        get() {
            var event: BaseEvent? = this
            while (event != null && event !is BeatEvent)
                event = event.mNextEvent
            return if (event != null) event as BeatEvent? else null
        }

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
        event.mPrevColorEvent = mPrevColorEvent
        event.mPrevBeatEvent = mPrevBeatEvent
        event.mPrevTrackEvent = mPrevTrackEvent
        event.mPrevLineEvent = mPrevLineEvent
        //event.mPrevScrollEvent=mPrevScrollEvent;
        when (event) {
            is ColorEvent -> event.mPrevColorEvent = event
            is BeatEvent -> event.mPrevBeatEvent = event
            is TrackEvent -> event.mPrevTrackEvent = event
            is LineEvent -> event.mPrevLineEvent = event
            //if(event instanceof ScrollEvent)
            //     event.mPrevScrollEvent=(ScrollEvent)event;
        }
        //if(event instanceof ScrollEvent)
        //     event.mPrevScrollEvent=(ScrollEvent)event;
    }

    fun insertAfter(event: BaseEvent) {
        val mTempNextEvent = mNextEvent
        mNextEvent = event
        event.mNextEvent = mTempNextEvent
        if (mTempNextEvent != null)
            mTempNextEvent.mPrevEvent = event
        event.mPrevEvent = this
        if (event is ColorEvent) {
            event.mPrevColorEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is ColorEvent) {
                currentEvent.mPrevColorEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevColorEvent = mPrevColorEvent

        /*if(event instanceof ScrollEvent)
        {
            event.mPrevScrollEvent = (ScrollEvent) event;
            BaseEvent currentEvent=event.mNextEvent;
            while((currentEvent!=null)&&(!(currentEvent instanceof ScrollEvent)))
            {
                currentEvent.mPrevScrollEvent=(ScrollEvent)event;
                currentEvent=currentEvent.mNextEvent;
            }
        }
        else
            event.mPrevScrollEvent=mPrevScrollEvent;*/

        if (event is BeatEvent) {
            event.mPrevBeatEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is BeatEvent) {
                currentEvent.mPrevBeatEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevBeatEvent = mPrevBeatEvent

        if (event is TrackEvent) {
            event.mPrevTrackEvent = event
            var currentEvent = event.mNextEvent
            while (currentEvent != null && currentEvent !is TrackEvent) {
                currentEvent.mPrevTrackEvent = event
                currentEvent = currentEvent.mNextEvent
            }
        } else
            event.mPrevTrackEvent = mPrevTrackEvent

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

    fun offsetLaterEvents(amount: Long) {
        var event = this.mNextEvent
        while (event != null) {
            event.offset(amount)
            event = event.mNextEvent
        }
    }

    protected open fun offset(amount: Long) {
        mEventTime += amount
    }

    fun findEventOnOrBefore(time: Long): BaseEvent? {
        var lastCheckedEvent = this
        var e: BaseEvent? = this
        while (e != null) {
            when {
                e.mEventTime > time -> {
                    if (lastCheckedEvent.mEventTime < time)
                        return lastCheckedEvent
                    lastCheckedEvent = e
                    e = e.mPrevEvent
                }
                e.mEventTime < time -> {
                    if (lastCheckedEvent.mEventTime > time)
                        return e
                    lastCheckedEvent = e
                    if (e.mNextEvent == null)
                        return e
                    e = e.mNextEvent
                }
                e.mEventTime == time -> return e
            }
        }
        return null
    }

    fun insertEvent(event: BaseEvent) {
        val eventBefore = findEventOnOrBefore(event.mEventTime)
        eventBefore!!.insertAfter(event)
    }

    fun remove() {
        val eventBefore = mPrevEvent
        val eventAfter = mNextEvent
        if (mPrevEvent != null)
            mPrevEvent!!.mNextEvent = eventAfter
        if (mNextEvent != null) {
            mNextEvent!!.mPrevEvent = eventBefore
            if (eventBefore != null) {
                if (mNextEvent !is TrackEvent)
                    mNextEvent!!.mPrevTrackEvent = eventBefore.mPrevTrackEvent
                if (mNextEvent !is ColorEvent)
                    mNextEvent!!.mPrevColorEvent = eventBefore.mPrevColorEvent
                if (mNextEvent !is LineEvent)
                    mNextEvent!!.mPrevLineEvent = eventBefore.mPrevLineEvent
                if (mNextEvent !is BeatEvent)
                    mNextEvent!!.mPrevBeatEvent = eventBefore.mPrevBeatEvent
            } else {
                if (mNextEvent !is TrackEvent)
                    mNextEvent!!.mPrevTrackEvent = null
                if (mNextEvent !is ColorEvent)
                    mNextEvent!!.mPrevColorEvent = null
                if (mNextEvent !is LineEvent)
                    mNextEvent!!.mPrevLineEvent = null
                if (mNextEvent !is BeatEvent)
                    mNextEvent!!.mPrevBeatEvent = null
            }
        }
    }
}