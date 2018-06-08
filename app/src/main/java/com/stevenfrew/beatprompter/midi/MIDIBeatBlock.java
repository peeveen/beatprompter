package com.stevenfrew.beatprompter.midi;

public class MIDIBeatBlock
{
    public int mMIDIBeatCount;
    public double mNanoPerBeat;
    public long mBlockStartTime;
    public MIDIBeatBlock(long blockStartTime,int midiBeatCount,double nanoPerBeat)
    {
        mBlockStartTime=blockStartTime;
        mNanoPerBeat=nanoPerBeat;
        mMIDIBeatCount=midiBeatCount;
    }
}
