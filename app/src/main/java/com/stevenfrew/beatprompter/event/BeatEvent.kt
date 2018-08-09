package com.stevenfrew.beatprompter.event

class BeatEvent(eventTime: Long, @JvmField var mBPM: Double, @JvmField var mBPB: Int, @JvmField var mBPL: Int, @JvmField var mBeat: Int, @JvmField var mClick: Boolean, @JvmField var mWillScrollOnBeat: Int) : BaseEvent(eventTime) {
    init {
        mPrevBeatEvent = this
    }
}
