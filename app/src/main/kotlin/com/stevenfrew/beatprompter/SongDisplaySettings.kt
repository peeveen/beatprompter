package com.stevenfrew.beatprompter

import android.graphics.Rect
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage

class SongDisplaySettings internal constructor(var mOrientation: Int, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect) {
    internal constructor(csm: ChooseSongMessage) : this(csm.mOrientation, csm.mMinFontSize.toFloat(), csm.mMaxFontSize.toFloat(), Rect(0,0,csm.mScreenWidth,csm.mScreenHeight))
}