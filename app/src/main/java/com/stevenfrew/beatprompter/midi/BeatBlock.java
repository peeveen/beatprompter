package com.stevenfrew.beatprompter.midi;

public class BeatBlock
{
    public int mMIDIBeatCount;
    public double mNanoPerBeat;
    public long mBlockStartTime;
    public BeatBlock(long blockStartTime, int midiBeatCount, double nanoPerBeat)
    {
        mBlockStartTime=blockStartTime;
        mNanoPerBeat=nanoPerBeat;
        mMIDIBeatCount=midiBeatCount;
    }
}
