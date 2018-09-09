package com.stevenfrew.beatprompter.midi

data class EventOffset constructor(val mAmount:Int, val mOffsetType: EventOffsetType, val mSourceFileLineNumber:Int)