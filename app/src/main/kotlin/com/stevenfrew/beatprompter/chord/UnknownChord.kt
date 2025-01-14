package com.stevenfrew.beatprompter.chord

class UnknownChord(private val chord: String) : IChord {
	override fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean
	): String = chord

	override fun transpose(transpositionMap: Map<Note, Note>): IChord = this
}