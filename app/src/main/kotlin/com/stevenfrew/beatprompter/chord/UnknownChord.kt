package com.stevenfrew.beatprompter.chord

class UnknownChord(private val chord: String) : IChord {
	override fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean
	): String = ChordUtils.replaceAccidentals(chord, false)

	override fun transpose(transpositionMap: Map<Note, Note>): IChord = this
}