package com.stevenfrew.beatprompter.song.chord

class UnknownChord(private val chord: String) : IChord {
	override fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean
	): String = if (useUnicodeAccidentals) ChordUtils.useUnicodeAccidentals(chord) else chord

	override fun transpose(transpositionMap: Map<Note, Note>): IChord = this
}