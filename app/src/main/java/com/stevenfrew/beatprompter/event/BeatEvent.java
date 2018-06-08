package com.stevenfrew.beatprompter.event;

public class BeatEvent extends BaseEvent
{
    public double mBPM;
    public int mBPB;
    public int mBPL;
    public int mBeat;
    public boolean mClick;
    public int mWillScrollOnBeat;

    public BeatEvent(long eventTime,double bpm,int bpb,int bpl, int beat,boolean click,int willScrollOnBeat)
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
