package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

data class EventOffset(val mAmount: Int,
                       val mOffsetType: EventOffsetType,
                       val mSourceFileLineNumber: Int) {
    constructor(lineNumber: Int) : this(0, EventOffsetType.Milliseconds, lineNumber)

    init {
        if (Math.abs(mAmount) > 16 && mOffsetType == EventOffsetType.Beats)
            throw IllegalArgumentException(BeatPrompter.getResourceString(R.string.max_midi_offset_exceeded))
        else if (Math.abs(mAmount) > 10000 && mOffsetType == EventOffsetType.Milliseconds)
            throw IllegalArgumentException(BeatPrompter.getResourceString(R.string.max_midi_offset_exceeded))

    }
}