package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Line

class LineEvent(eventTime: Long, @JvmField var mDuration: Long) : BaseEvent(eventTime) {
    @JvmField var mLine: Line? = null

    init {
        mPrevLineEvent = this
    }

    override fun offset(amount: Long) {
        super.offset(amount)
        mLine!!.mYStartScrollTime += amount
        mLine!!.mYStopScrollTime += amount
    }
}
