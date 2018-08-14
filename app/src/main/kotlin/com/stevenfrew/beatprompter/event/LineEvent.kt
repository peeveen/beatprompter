package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Line

class LineEvent(eventTime: Long, line:Line, var mDuration: Long) : BaseEvent(eventTime) {
    val mLine=line

    init {
        line.mLineEvent = this
        mPrevLineEvent = this
    }

    override fun offset(amount: Long) {
        super.offset(amount)
        mLine.mYStartScrollTime += amount
        mLine.mYStopScrollTime += amount
    }
}
