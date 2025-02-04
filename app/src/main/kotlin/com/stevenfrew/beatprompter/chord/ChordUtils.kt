package com.stevenfrew.beatprompter.chord

object ChordUtils {
	fun replaceAccidentals(str: String, unicode: Boolean): String =
		if (unicode) str.replace('b', '♭').replace('#', '♯') else str.replace('♭', 'b')
			.replace('♯', '#')
}