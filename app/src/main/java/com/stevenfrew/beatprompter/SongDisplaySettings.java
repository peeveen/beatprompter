package com.stevenfrew.beatprompter;

import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;

public class SongDisplaySettings {
    public int mOrientation;
    public int mMinFontSize;
    public int mMaxFontSize;
    public int mScreenWidth;
    public int mScreenHeight;

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
