package com.stevenfrew.beatprompter;

public class SmoothScrollingTimings
{
    public SmoothScrollingTimings(long line,long bar,long track)
    {
        mTimePerLine=line;
        mTimePerBar=bar;
        mTrackLength=track;
    }
    public long mTimePerLine;
    public long mTimePerBar;
    public long mTrackLength;
}

