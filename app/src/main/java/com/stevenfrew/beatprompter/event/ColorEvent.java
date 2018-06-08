package com.stevenfrew.beatprompter.event;

public class ColorEvent extends BaseEvent
{
    public int mBackgroundColor;
    public int mPulseColor;
    public int mLyricColor;
    public int mChordColor;
    public int mAnnotationColor;
    public int mBeatCounterColor;
    public int mScrollMarkerColor;

    public ColorEvent(long eventTime, int backgroundColor,int pulseColor,int lyricColor,int chordColour,int annotationColour,int beatCounterColor,int scrollMarkerColor)
    {
        super(eventTime);
        mPrevColorEvent=this;
        mBackgroundColor=backgroundColor;
        mPulseColor=pulseColor;
        mLyricColor=lyricColor;
        mAnnotationColor=annotationColour;
        mChordColor=chordColour;
        mBeatCounterColor=beatCounterColor;
        mScrollMarkerColor=scrollMarkerColor;
    }
}
