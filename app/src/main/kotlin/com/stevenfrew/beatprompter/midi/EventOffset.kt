package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import kotlin.math.abs

data class EventOffset(
	val amount: Int,
	val offsetType: EventOffsetType,
	val sourceFileLineNumber: Int
) {
	constructor(lineNumber: Int) : this(0, EventOffsetType.Milliseconds, lineNumber)

	init {
		require(!(abs(amount) > 16 && offsetType == EventOffsetType.Beats)) {
			BeatPrompter.appResources.getString(
				R.string.max_midi_offset_exceeded
			)
		}
		require(!(abs(amount) > 10000 && offsetType == EventOffsetType.Milliseconds)) {
			BeatPrompter.appResources.getString(
				R.string.max_midi_offset_exceeded
			)
		}
	}
}