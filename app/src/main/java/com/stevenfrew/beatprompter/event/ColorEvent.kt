package com.stevenfrew.beatprompter.event

class ColorEvent(eventTime: Long, var mBackgroundColor: Int, var mPulseColor: Int, var mLyricColor: Int, var mChordColor: Int, var mAnnotationColor: Int, var mBeatCounterColor: Int, var mScrollMarkerColor: Int) : BaseEvent(eventTime) {
    init {
        mPrevColorEvent = this
    }
}
