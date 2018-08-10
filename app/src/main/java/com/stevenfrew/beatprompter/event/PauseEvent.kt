package com.stevenfrew.beatprompter.event

class PauseEvent(eventTime: Long, var mBeats: Int, var mBeat: Int) : BaseEvent(eventTime)