package com.stevenfrew.beatprompter.song.chord

interface IChord {
	fun getChordDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean = false
	): String

	fun transpose(transpositionMap: Map<String, String>): IChord
}