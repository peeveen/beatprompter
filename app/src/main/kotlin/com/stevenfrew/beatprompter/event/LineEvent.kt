package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Line

class LineEvent constructor(eventTime:Long,val mLine:Line) : BaseEvent(eventTime)