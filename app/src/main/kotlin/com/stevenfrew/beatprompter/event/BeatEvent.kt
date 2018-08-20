package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.BeatInfo

class BeatEvent(eventTime: Long, beatInfo: BeatInfo, var mBeat: Int, var mClick: Boolean, var mWillScrollOnBeat: Int) : BaseEvent(eventTime) {
    val mBPM=beatInfo.mBPM
    var mBPB=beatInfo.mBPB
    val mBPL=beatInfo.mBPL
    init {
        mPrevBeatEvent = this
    }
}
