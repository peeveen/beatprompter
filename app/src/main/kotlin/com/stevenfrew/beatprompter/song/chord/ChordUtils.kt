package com.stevenfrew.beatprompter.song.chord

object ChordUtils {
	fun useUnicodeAccidentals(str: String): String = str.replace('b', '♭').replace('#', '♯')
}