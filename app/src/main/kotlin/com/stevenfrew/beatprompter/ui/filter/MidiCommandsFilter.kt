package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class MidiCommandsFilter() :
	Filter(BeatPrompter.appResources.getString(R.string.midi_commands), false) {
	override fun equals(other: Any?): Boolean = other != null && other is MidiCommandsFilter
	override fun hashCode(): Int = javaClass.hashCode()
}