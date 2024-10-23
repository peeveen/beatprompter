package com.stevenfrew.beatprompter.song.chord

interface IChord {
	fun toDisplayString(
		alwaysUseSharps: Boolean = false,
		useUnicodeAccidentals: Boolean = false,
		majorOrMinorRootOnly: Boolean = false
	): String

	fun transpose(transpositionMap: Map<Note, Note>): IChord
}