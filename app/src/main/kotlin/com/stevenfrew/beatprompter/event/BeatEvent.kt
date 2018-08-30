package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.LineBeatInfo

class BeatEvent(eventTime: Long, beatInfo: LineBeatInfo, var mBeat: Int, var mClick: Boolean, var mWillScrollOnBeat: Int) : BaseEvent(eventTime) {
    val mBPM=beatInfo.mBPM
    var mBPB=beatInfo.mBPB
    init {
        mPrevBeatEvent = this
    }
}
