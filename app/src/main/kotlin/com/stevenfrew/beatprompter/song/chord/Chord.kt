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
	val root: Note,
	val suffix: String? = null,
	val bass: Note? = null
) : IChord {
	companion object {
		/**
		 * The rank for each possible chord, and also the sharp-only version.
		 * Rank is the distance in semitones from C.
		 */
		val CHORD_RANKS: Map<Note, Int> = mapOf(
			Note.BSharp to 0,
			Note.C to 0,
			Note.CSharp to 1,
			Note.DFlat to 1,
			Note.D to 2,
			Note.DSharp to 3,
			Note.EFlat to 3,
			Note.E to 4,
			Note.FFlat to 4,
			Note.ESharp to 5,
			Note.F to 5,
			Note.FSharp to 6,
			Note.GFlat to 6,
			Note.G to 7,
			Note.GSharp to 8,
			Note.AFlat to 8,
			Note.A to 9,
			Note.ASharp to 10,
			Note.BFlat to 10,
			Note.CFlat to 11,
			Note.B to 11
		)

		private const val REGEX_ROOT_GROUP_NAME = "root"
		private const val REGEX_SUFFIX_GROUP_NAME = "suffix"
		private const val REGEX_BASS_GROUP_NAME = "bass"

		private val ACCIDENTALS = listOf('b', '♭', '#', '♯', '♮')

		// Regex for recognizing chords
		val MINOR_SUFFIXES = listOf("m", "mmaj", "mM", "min", "minor")
		private val NOT_MINOR_SUFFIXES =
			listOf("M", "maj", "major", "dim", "sus", "dom", "aug", "Ø", "ø", "°", "Δ", "∆", "\\+", "-")

		private val TRIAD_PATTERN =
			"(${NOT_MINOR_SUFFIXES.joinToString("|")}|${MINOR_SUFFIXES.joinToString("|")})"
		private val ADDED_TONE_PATTERN =
			"(\\(?([\\/\\.\\+]|add)?[${ACCIDENTALS.joinToString()}]?\\d+[\\+-]?\\)?)"
		private val SUFFIX_PATTERN =
			"(?<$REGEX_SUFFIX_GROUP_NAME>${TRIAD_PATTERN}?\\d*\\(?${TRIAD_PATTERN}?${ADDED_TONE_PATTERN}*\\)?)"
		private val BASS_PATTERN =
			"(\\/(?<$REGEX_BASS_GROUP_NAME>[A-G](${ACCIDENTALS.joinToString("|")})?))?"

		private val ROOT_PATTERN = "(?<$REGEX_ROOT_GROUP_NAME>[A-G](${ACCIDENTALS.joinToString("|")})?)"

		private val CHORD_REGEX = "^${ROOT_PATTERN}${SUFFIX_PATTERN}${BASS_PATTERN}$"

		private val CHORD_REGEX_PATTERN = Pattern.compile(CHORD_REGEX)

		fun parse(chord: String): Chord? {
			try {
				val result = CHORD_REGEX_PATTERN.matcher(chord)
				if (result.find()) {
					// For Oreo
					/*					val root = result.group(REGEX_ROOT_GROUP_NAME)
										val suffix = result.group(REGEX_SUFFIX_GROUP_NAME)
										val bass = result.group(REGEX_BASS_GROUP_NAME)*/
					val root = result.group(1)
					val suffix = result.group(3)
					val bass = result.group(9)
					if (root.isNullOrBlank())
						throw InvalidChordException(chord)
					return Chord(
						Note.parse(root),
						if (suffix.isNullOrBlank()) null else suffix,
						if (bass.isNullOrBlank()) null else Note.parse(bass)
					)
				}
			} catch (_: InvalidChordException) {
				// Chord could not be parsed.
			}
			return null
		}

		fun isChord(token: String): Boolean = CHORD_REGEX_PATTERN.matcher(token).matches()

		internal fun isMinorSuffix(suffix: String?) =
			(suffix?.startsWith("m") == true || suffix?.startsWith("min") == true) && suffix.startsWith("maj") != true
	}

	override fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean
	): String {
		val replacedSuffix =
			if (useUnicodeAccidentals && suffix != null) ChordUtils.useUnicodeAccidentals(suffix) else suffix
		val secondPart =
			if (majorOrMinorRootOnly)
				""
			else if (bass != null)
				"$replacedSuffix/${
					bass.toDisplayString(alwaysUseSharps, useUnicodeAccidentals, false, replacedSuffix)
				}"
			else
				replacedSuffix
		return "${
			root.toDisplayString(
				alwaysUseSharps,
				useUnicodeAccidentals,
				majorOrMinorRootOnly,
				replacedSuffix
			)
		}${secondPart}"
	}

	override fun transpose(transpositionMap: Map<Note, Note>): IChord =
		Chord(
			transpositionMap[root] ?: root,
			suffix,
			transpositionMap[bass]
		)

	val isMinor = isMinorSuffix(suffix)
}