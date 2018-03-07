package com.stevenfrew.beatprompter;

class ColorEvent extends BaseEvent
{
    int mBackgroundColor;
    int mPulseColor;
    int mLyricColor;
    int mChordColor;
    int mAnnotationColor;
    int mBeatCounterColor;
    int mScrollMarkerColor;

    ColorEvent(long eventTime, int backgroundColor,int pulseColor,int lyricColor,int chordColour,int annotationColour,int beatCounterColor,int scrollMarkerColor)
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
