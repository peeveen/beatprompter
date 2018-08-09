package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage

class SongDisplaySettings internal constructor(@JvmField var mOrientation: Int, @JvmField var mMinFontSize: Int, @JvmField var mMaxFontSize: Int, @JvmField var mScreenWidth: Int, @JvmField var mScreenHeight: Int) {

    internal constructor(csm: ChooseSongMessage) : this(csm.mOrientation, csm.mMinFontSize, csm.mMaxFontSize, csm.mScreenWidth, csm.mScreenHeight) {}
}