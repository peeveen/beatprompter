package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage

class SongDisplaySettings internal constructor(var mOrientation: Int, var mMinFontSize: Int, var mMaxFontSize: Int, var mScreenWidth: Int, var mScreenHeight: Int) {

    internal constructor(csm: ChooseSongMessage) : this(csm.mOrientation, csm.mMinFontSize, csm.mMaxFontSize, csm.mScreenWidth, csm.mScreenHeight)
}