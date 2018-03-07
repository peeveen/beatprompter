package com.stevenfrew.beatprompter;

class BeatEvent extends BaseEvent
{
    double mBPM;
    int mBPB;
    int mBPL;
    int mBeat;
    boolean mClick;
    int mWillScrollOnBeat;

    BeatEvent(long eventTime,double bpm,int bpb,int bpl, int beat,boolean click,int willScrollOnBeat)
    {
        super(eventTime);
        mBPL=bpl;
        mPrevBeatEvent=this;
        mBPM=bpm;
        mBPB=bpb;
        mBeat=beat;
        mClick=click;
        mWillScrollOnBeat=willScrollOnBeat;
    }
}
