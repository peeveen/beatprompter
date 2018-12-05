package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

data class EventOffset constructor(val mAmount: Int,
                                   val mOffsetType: EventOffsetType,
                                   val mSourceFileLineNumber: Int) {
    constructor(lineNumber: Int) : this(0, EventOffsetType.Milliseconds, lineNumber)

    init {
        if (Math.abs(mAmount) > 16 && mOffsetType == EventOffsetType.Beats)
            throw IllegalArgumentException(BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))
        else if (Math.abs(mAmount) > 10000 && mOffsetType == EventOffsetType.Milliseconds)
            throw IllegalArgumentException(BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded))

    }
}