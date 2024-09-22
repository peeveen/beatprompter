package com.stevenfrew.beatprompter.song.chord

import java.util.regex.Pattern

/**
 * Represents a musical chord. For example, Am7/C would have:
 *
 * root: A
 * suffix: m7
 * bass: C
 */
class Chord(
	val root: String,
	val suffix: String? = null,
	val bass: String? = null
) {
	companion object {
		fun useUnicodeFlatsAndSharps(str: String): String = str.replace('b', '♭').replace('#', '♯')

		private const val REGEX_ROOT_GROUP_NAME = "root"
		private const val REGEX_SUFFIX_GROUP_NAME = "suffix"
		private const val REGEX_BASS_GROUP_NAME = "bass"

		private val ACCIDENTALS = listOf('b', '♭', '#', '♯', '♮')

		// Regex for recognizing chords
		val MINOR_SUFFIXES = listOf("m", "min", "minor")
		private val NOT_MINOR_SUFFIXES =
			listOf("M", "maj", "major", "dim", "sus", "dom", "aug", "Ø", "ø", "°", "Δ", "∆", "\\+", "-")

		private val TRIAD_PATTERN =
			"(${NOT_MINOR_SUFFIXES.joinToString("|")}|${MINOR_SUFFIXES.joinToString("|")})"
		private val ADDED_TONE_PATTERN =
			"(\\(?([\\/\\.\\+]|add)?[${ACCIDENTALS.joinToString()}]?\\d+[\\+-]?\\)?)"
		private val SUFFIX_PATTERN =
			"(?<$REGEX_SUFFIX_GROUP_NAME>\\(?${TRIAD_PATTERN}?${ADDED_TONE_PATTERN}*\\)?)"
		private val BASS_PATTERN =
			"(\\/(?<$REGEX_BASS_GROUP_NAME>[A-G](${ACCIDENTALS.joinToString("|")})?))?"

		val ROOT_PATTERN = "(?<$REGEX_ROOT_GROUP_NAME>[A-G](${ACCIDENTALS.joinToString("|")})?)"
		private val NOT_MINOR_PATTERN = "($NOT_MINOR_SUFFIXES)+"

		private val CHORD_REGEX = "^${ROOT_PATTERN}${SUFFIX_PATTERN}${BASS_PATTERN}$"
		private val NOT_MINOR_SUFFIX_REGEX = "^${NOT_MINOR_PATTERN}.*$"

		private val CHORD_REGEX_PATTERN = Pattern.compile(CHORD_REGEX)
		private val NOT_MINOR_SUFFIX_REGEX_PATTERN = Pattern.compile(NOT_MINOR_SUFFIX_REGEX)

		fun parse(chord: String): Chord? {
			try {
				val result = CHORD_REGEX_PATTERN.matcher(chord)
				if (result.find()) {
					val root = result.group(REGEX_ROOT_GROUP_NAME)
					val suffix = result.group(REGEX_SUFFIX_GROUP_NAME)
					val bass = result.group(REGEX_BASS_GROUP_NAME)
					if (root.isNullOrBlank())
						throw IllegalStateException("Failed to parse chord $chord")
					return Chord(root, suffix, bass)
				}
			} catch (e: IllegalStateException) {
				// Chord could not be parsed.
			}
			return null
		}

		fun isChord(token: String): Boolean = CHORD_REGEX_PATTERN.matcher(token).matches()
	}

	override fun toString(): String =
		useUnicodeFlatsAndSharps(
			if (this.bass != null)
				this.root + this.suffix + "/" + this.bass
			else
				this.root + this.suffix
		)

	val isMinor
		get() = if (suffix == null) false else !NOT_MINOR_SUFFIX_REGEX_PATTERN.matcher(suffix).matches()
}