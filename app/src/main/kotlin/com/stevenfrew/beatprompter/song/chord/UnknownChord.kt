package com.stevenfrew.beatprompter.song.chord

class UnknownChord(private val chord: String) : IChord {
	override fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean
	): String = ChordUtils.replaceAccidentals(chord, useUnicodeAccidentals)

	override fun transpose(transpositionMap: Map<Note, Note>): IChord = this
}