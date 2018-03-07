package com.stevenfrew.beatprompter;

class MIDIBeatBlock
{
    int mMIDIBeatCount;
    double mNanoPerBeat;
    long mBlockStartTime;
    MIDIBeatBlock(long blockStartTime,int midiBeatCount,double nanoPerBeat)
    {
        mBlockStartTime=blockStartTime;
        mNanoPerBeat=nanoPerBeat;
        mMIDIBeatCount=midiBeatCount;
    }
}
