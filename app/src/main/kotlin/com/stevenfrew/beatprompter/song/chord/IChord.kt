package com.stevenfrew.beatprompter.song.chord

interface IChord {
	fun getChordDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean
	): String

	fun transpose(transpositionMap: Map<String, String>): IChord

	fun useUnicodeAccidentals(str: String): String = str.replace('b', '♭').replace('#', '♯')
}