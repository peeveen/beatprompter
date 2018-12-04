package com.stevenfrew.beatprompter.song.event

/**
 * A BeatEvent signals the event processor to update the beat counter display, and optionally play a
 * metronome click sound.
 */
class BeatEvent(eventTime: Long, val mBPM: Double, var mBPB: Int, val mBeat: Int, val mClick: Boolean, var mWillScrollOnBeat: Int) : BaseEvent(eventTime)