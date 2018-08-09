package com.stevenfrew.beatprompter.event

class ColorEvent(eventTime: Long, @JvmField var mBackgroundColor: Int, @JvmField var mPulseColor: Int, @JvmField var mLyricColor: Int, @JvmField var mChordColor: Int, @JvmField var mAnnotationColor: Int, @JvmField var mBeatCounterColor: Int, @JvmField var mScrollMarkerColor: Int) : BaseEvent(eventTime) {
    init {
        mPrevColorEvent = this
    }
}
