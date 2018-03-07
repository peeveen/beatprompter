package com.stevenfrew.beatprompter;

class PauseEvent extends BaseEvent
{
    int mBeats;
    int mBeat;
    PauseEvent(long eventTime,int beats, int beat)
    {
        super(eventTime);
        mBeats=beats;
        mBeat=beat;
    }
}
