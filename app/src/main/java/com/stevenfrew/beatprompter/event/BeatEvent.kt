package com.stevenfrew.beatprompter.event

class BeatEvent(eventTime: Long, var mBPM: Double, var mBPB: Int, var mBPL: Int, var mBeat: Int, var mClick: Boolean, var mWillScrollOnBeat: Int) : BaseEvent(eventTime) {
    init {
        mPrevBeatEvent = this
    }
}
