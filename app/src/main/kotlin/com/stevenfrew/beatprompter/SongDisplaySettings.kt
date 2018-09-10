package com.stevenfrew.beatprompter

import android.graphics.Rect
import com.stevenfrew.beatprompter.comm.bluetooth.message.SongChoiceInfo

class SongDisplaySettings internal constructor(var mOrientation: Int, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect, val mShowBeatCounter:Boolean) {
    internal constructor(choiceInfo: SongChoiceInfo) : this(choiceInfo.mOrientation, choiceInfo.mMinFontSize, choiceInfo.mMaxFontSize, choiceInfo.mScreenSize,choiceInfo.mBeatScroll||choiceInfo.mSmoothScroll)

    private val mBeatCounterHeight=
        // Top 5% of screen is used for beat counter
        if (mShowBeatCounter)
            (mScreenSize.height() / 20.0).toInt()
        else
            0
    val mBeatCounterRect = Rect(0, 0, mScreenSize.width(), mBeatCounterHeight)
    val mUsableScreenHeight=mScreenSize.height()-mBeatCounterRect.height()
}