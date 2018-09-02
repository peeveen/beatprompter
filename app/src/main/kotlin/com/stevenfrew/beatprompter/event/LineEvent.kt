package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Line

class LineEvent constructor(eventTime:Long,line:Line) : BaseEvent(eventTime) {
    val mLine=line

    init {
        mPrevLineEvent = this
    }
}
