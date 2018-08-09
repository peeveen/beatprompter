package com.stevenfrew.beatprompter.event

class PauseEvent(eventTime: Long, @JvmField var mBeats: Int, @JvmField var mBeat: Int) : BaseEvent(eventTime)