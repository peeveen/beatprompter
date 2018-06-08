package com.stevenfrew.beatprompter.event;

public class PauseEvent extends BaseEvent
{
    public int mBeats;
    public int mBeat;
    public PauseEvent(long eventTime,int beats, int beat)
    {
        super(eventTime);
        mBeats=beats;
        mBeat=beat;
    }
}
