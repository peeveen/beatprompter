package com.stevenfrew.beatprompter

import android.graphics.Rect
import com.stevenfrew.beatprompter.bluetooth.message.ChooseSongMessage

class SongDisplaySettings internal constructor(var mOrientation: Int, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect, val mShowBeatCounter:Boolean) {
    internal constructor(csm: ChooseSongMessage) : this(csm.mOrientation, csm.mMinFontSize, csm.mMaxFontSize, csm.mScreenSize,csm.mBeatScroll||csm.mSmoothScroll)

    private val mBeatCounterHeight=
        // Top 5% of screen is used for beat counter
        if (mShowBeatCounter)
            (mScreenSize.height() / 20.0).toInt()
        else
            0
    val mBeatCounterRect = Rect(0, 0, mScreenSize.width(), mBeatCounterHeight)
    val mUsableScreenHeight=mScreenSize.height()-mBeatCounterRect.height()
}