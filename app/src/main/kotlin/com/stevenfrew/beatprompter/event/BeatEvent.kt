package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.LineBeatInfo

/**
 * A BeatEvent signals the event processor to update the beat counter display, and optionally play a
 * metronome click sound.
 */
class BeatEvent(eventTime: Long, beatInfo: LineBeatInfo, var mBeat: Int, var mClick: Boolean, var mWillScrollOnBeat: Int) : BaseEvent(eventTime) {
    val mBPM=beatInfo.mBPM
    var mBPB=beatInfo.mBPB
}
