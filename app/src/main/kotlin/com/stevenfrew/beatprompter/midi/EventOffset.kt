package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import kotlin.math.abs

data class EventOffset(val mAmount: Int,
                       val mOffsetType: EventOffsetType,
                       val mSourceFileLineNumber: Int) {
    constructor(lineNumber: Int) : this(0, EventOffsetType.Milliseconds, lineNumber)

    init {
        require(!(abs(mAmount) > 16 && mOffsetType == EventOffsetType.Beats)) { BeatPrompter.getResourceString(R.string.max_midi_offset_exceeded) }
        require(!(abs(mAmount) > 10000 && mOffsetType == EventOffsetType.Milliseconds)) { BeatPrompter.getResourceString(R.string.max_midi_offset_exceeded) }
    }
}