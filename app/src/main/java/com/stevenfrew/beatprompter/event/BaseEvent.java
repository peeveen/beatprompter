package com.stevenfrew.beatprompter.event;

public class BaseEvent {
    public BaseEvent mPrevEvent;
    public BaseEvent mNextEvent;
    public ColorEvent mPrevColorEvent;
    public TrackEvent mPrevTrackEvent;
    //ScrollEvent mPrevScrollEvent;
    public BeatEvent mPrevBeatEvent;
    public LineEvent mPrevLineEvent;

    public long mEventTime; // Time at which this event occurs (in nanoseconds AFTER the globalStart time)

    protected BaseEvent(long eventTime)
    {
        mEventTime=eventTime;
    }

    public void add(BaseEvent event)
    {
        mNextEvent=event;
        event.mPrevEvent=this;
        event.mPrevColorEvent=mPrevColorEvent;
        event.mPrevBeatEvent=mPrevBeatEvent;
        event.mPrevTrackEvent=mPrevTrackEvent;
        event.mPrevLineEvent=mPrevLineEvent;
        //event.mPrevScrollEvent=mPrevScrollEvent;
        if(event instanceof ColorEvent)
            event.mPrevColorEvent=(ColorEvent)event;
        //if(event instanceof ScrollEvent)
       //     event.mPrevScrollEvent=(ScrollEvent)event;
        else if(event instanceof BeatEvent)
            event.mPrevBeatEvent=(BeatEvent)event;
        else if(event instanceof TrackEvent)
            event.mPrevTrackEvent=(TrackEvent)event;
        else if(event instanceof LineEvent)
            event.mPrevLineEvent=(LineEvent)event;
    }

    public void insertAfter(BaseEvent event)
    {
        BaseEvent mTempNextEvent=mNextEvent;
        mNextEvent=event;
        event.mNextEvent=mTempNextEvent;
        if(mTempNextEvent!=null)
            mTempNextEvent.mPrevEvent=event;
        event.mPrevEvent=this;
        if(event instanceof ColorEvent)
        {
            event.mPrevColorEvent = (ColorEvent) event;
            BaseEvent currentEvent=event.mNextEvent;
            while((currentEvent!=null)&&(!(currentEvent instanceof ColorEvent)))
            {
                currentEvent.mPrevColorEvent=(ColorEvent)event;
                currentEvent=currentEvent.mNextEvent;
            }
        }
        else
            event.mPrevColorEvent=mPrevColorEvent;

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

        if(event instanceof BeatEvent)
        {
            event.mPrevBeatEvent = (BeatEvent) event;
            BaseEvent currentEvent=event.mNextEvent;
            while((currentEvent!=null)&&(!(currentEvent instanceof BeatEvent)))
            {
                currentEvent.mPrevBeatEvent=(BeatEvent)event;
                currentEvent=currentEvent.mNextEvent;
            }
        }
        else
            event.mPrevBeatEvent=mPrevBeatEvent;

        if(event instanceof TrackEvent)
        {
            event.mPrevTrackEvent = (TrackEvent) event;
            BaseEvent currentEvent=event.mNextEvent;
            while((currentEvent!=null)&&(!(currentEvent instanceof TrackEvent)))
            {
                currentEvent.mPrevTrackEvent=(TrackEvent)event;
                currentEvent=currentEvent.mNextEvent;
            }
        }
        else
            event.mPrevTrackEvent=mPrevTrackEvent;

        if(event instanceof LineEvent)
        {
            event.mPrevLineEvent = (LineEvent) event;
            BaseEvent currentEvent=event.mNextEvent;
            while((currentEvent!=null)&&(!(currentEvent instanceof LineEvent)))
            {
                currentEvent.mPrevLineEvent=(LineEvent)event;
                currentEvent=currentEvent.mNextEvent;
            }
        }
        else
            event.mPrevLineEvent=mPrevLineEvent;
    }

    public void offsetLaterEvents(long amount)
    {
        BaseEvent event=this.mNextEvent;
        while(event!=null)
        {
            event.offset(amount);
            event=event.mNextEvent;
        }
    }

    protected void offset(long amount)
    {
        mEventTime+=amount;
    }

    public BaseEvent findEventOnOrBefore(long time)
    {
        BaseEvent lastCheckedEvent=this;
        BaseEvent e=this;
        while(e!=null)
        {
            if(e.mEventTime>time)
            {
                if(lastCheckedEvent.mEventTime<time)
                    return lastCheckedEvent;
                lastCheckedEvent=e;
                e = e.mPrevEvent;
            }
            else if(e.mEventTime<time)
            {
                if(lastCheckedEvent.mEventTime>time)
                    return e;
                lastCheckedEvent=e;
                if(e.mNextEvent==null)
                    return e;
                e = e.mNextEvent;
            }
            else if(e.mEventTime==time)
                return e;
        }
        return null;
    }

    public BaseEvent getLastEvent()
    {
        BaseEvent e=this;
        while(e.mNextEvent!=null)
            e=e.mNextEvent;
        return e;
    }

    public BeatEvent getFirstBeatEvent()
    {
        BaseEvent event=this;
        while((event!=null)&&(!(event instanceof BeatEvent)))
            event=event.mNextEvent;
        if(event!=null)
            return (BeatEvent)event;
        return null;
    }

    public void insertEvent(BaseEvent event)
    {
        BaseEvent eventBefore=findEventOnOrBefore(event.mEventTime);
        eventBefore.insertAfter(event);
    }

    public void remove()
    {
        BaseEvent eventBefore=mPrevEvent;
        BaseEvent eventAfter=mNextEvent;
        if(mPrevEvent!=null)
            mPrevEvent.mNextEvent=eventAfter;
        if(mNextEvent!=null)
        {
            mNextEvent.mPrevEvent=eventBefore;
            if(eventBefore!=null) {
                if(!(mNextEvent instanceof TrackEvent))
                    mNextEvent.mPrevTrackEvent = eventBefore.mPrevTrackEvent;
                if(!(mNextEvent instanceof ColorEvent))
                    mNextEvent.mPrevColorEvent = eventBefore.mPrevColorEvent;
                if(!(mNextEvent instanceof LineEvent))
                    mNextEvent.mPrevLineEvent = eventBefore.mPrevLineEvent;
                if(!(mNextEvent instanceof BeatEvent))
                    mNextEvent.mPrevBeatEvent = eventBefore.mPrevBeatEvent;
            }
            else {
                if(!(mNextEvent instanceof TrackEvent))
                    mNextEvent.mPrevTrackEvent = null;
                if(!(mNextEvent instanceof ColorEvent))
                    mNextEvent.mPrevColorEvent = null;
                if(!(mNextEvent instanceof LineEvent))
                    mNextEvent.mPrevLineEvent = null;
                if(!(mNextEvent instanceof BeatEvent))
                    mNextEvent.mPrevBeatEvent = null;
            }
        }
    }

    public BeatEvent getNextBeatEvent() {
        BaseEvent event = mNextEvent;
        if(mNextEvent==null)
            return null;
        do {
            if (event instanceof BeatEvent)
                return (BeatEvent) event;
            event = event.mNextEvent;
        } while (event != null);
        return null;
    }
}
