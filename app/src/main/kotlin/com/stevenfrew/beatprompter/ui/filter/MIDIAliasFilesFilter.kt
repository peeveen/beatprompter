package com.stevenfrew.beatprompter.ui.filter

class MIDIAliasFilesFilter(name: String) : Filter(name, false) {
	override fun equals(other: Any?): Boolean = other != null && other is MIDIAliasFilesFilter
	override fun hashCode(): Int = javaClass.hashCode()
}