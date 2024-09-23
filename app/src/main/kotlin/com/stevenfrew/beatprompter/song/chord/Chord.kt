package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
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
) : IChord {
  companion object {
    /**
     * The rank for each possible chord, and also the sharp-only version.
     * Rank is the distance in semitones from C.
     */
    val CHORD_RANKS_AND_SHARPS: Map<String, Pair<Int, String>> = mapOf(
      "B#" to (0 to "B#"),
      "B♯" to (0 to "B♯"),
      "C" to (0 to "C"),
      "C#" to (1 to "C#"),
      "C♯" to (1 to "C♯"),
      "Db" to (1 to "C#"),
      "D♭" to (1 to "C♯"),
      "D" to (2 to "D"),
      "D#" to (3 to "D#"),
      "D♯" to (3 to "D♯"),
      "Eb" to (3 to "D#"),
      "E♭" to (3 to "D♯"),
      "E" to (4 to "E"),
      "Fb" to (4 to "E"),
      "F♭" to (4 to "E"),
      "E#" to (5 to "E#"),
      "E♯" to (5 to "E♯"),
      "F" to (5 to "F"),
      "F#" to (6 to "F#"),
      "F♯" to (6 to "F♯"),
      "Gb" to (6 to "F#"),
      "G♭" to (6 to "F♯"),
      "G" to (7 to "G"),
      "G#" to (8 to "G#"),
      "G♯" to (8 to "G♯"),
      "Ab" to (8 to "G#"),
      "A♭" to (8 to "G♯"),
      "A" to (9 to "A"),
      "A#" to (10 to "A#"),
      "A♯" to (10 to "A♯"),
      "Bb" to (10 to "A#"),
      "B♭" to (10 to "A♯"),
      "Cb" to (11 to "B"),
      "C♭" to (11 to "B"),
      "B" to (11 to "B")
    )

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
          // For Oreo
          /*					val root = result.group(REGEX_ROOT_GROUP_NAME)
                    val suffix = result.group(REGEX_SUFFIX_GROUP_NAME)
                    val bass = result.group(REGEX_BASS_GROUP_NAME)*/
          val root = result.group(1)
          val suffix = result.group(3)
          val bass = result.group(8)
          if (root.isNullOrBlank())
            throw IllegalStateException(
              BeatPrompter.appResources.getString(
                R.string.failedToParseChord,
                chord
              )
            )
          return Chord(root, suffix, bass)
        }
      } catch (e: IllegalStateException) {
        // Chord could not be parsed.
      }
      return null
    }

    fun isChord(token: String): Boolean = CHORD_REGEX_PATTERN.matcher(token).matches()
  }

  private fun makeSharp(chord: String?): String? =
    chord?.let { CHORD_RANKS_AND_SHARPS[it]?.second ?: chord }

  override fun getChordDisplayString(
    alwaysUseSharps: Boolean,
    useUnicodeAccidentals: Boolean,
    rootOnly: Boolean
  ): String {
    val root = if (alwaysUseSharps) makeSharp(root) else root
    val bass = if (alwaysUseSharps) makeSharp(bass) else bass
    val secondPart =
      if (rootOnly)
        ""
      else if (bass != null)
        "$suffix/$bass"
      else
        suffix
    val rawChord = root + secondPart
    return rawChord.let {
      if (useUnicodeAccidentals) ChordUtils.useUnicodeAccidentals(it) else it
    }
  }

  override fun transpose(transpositionMap: Map<String, String>): IChord =
    Chord(
      transpositionMap[root] ?: root,
      suffix,
      transpositionMap[bass]
    )

  val isMinor
    get() = if (suffix == null) false else !NOT_MINOR_SUFFIX_REGEX_PATTERN.matcher(suffix).matches()
}