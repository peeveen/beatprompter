package com.stevenfrew.beatprompter;

public class SongDisplaySettings {
    int mOrientation;
    int mMinFontSize;
    int mMaxFontSize;
    int mScreenWidth;
    int mScreenHeight;

    SongDisplaySettings(int orientation,int minFontSize,int maxFontSize,int screenWidth,int screenHeight)
    {
        mMinFontSize=minFontSize;
        mMaxFontSize=maxFontSize;
        mOrientation=orientation;
        mScreenWidth=screenWidth;
        mScreenHeight=screenHeight;
    }

    SongDisplaySettings(ChooseSongMessage csm)
    {
        this(csm.mOrientation,csm.mMinFontSize,csm.mMaxFontSize,csm.mScreenWidth,csm.mScreenHeight);
    }
}
