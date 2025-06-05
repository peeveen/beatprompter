package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class MidiAliasFilesFilter :
	Filter(BeatPrompter.appResources.getString(R.string.midi_alias_files), false) {
	override fun equals(other: Any?): Boolean = other != null && other is MidiAliasFilesFilter
	override fun hashCode(): Int = javaClass.hashCode()
}