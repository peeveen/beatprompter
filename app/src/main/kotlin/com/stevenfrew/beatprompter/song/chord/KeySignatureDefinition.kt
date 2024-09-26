package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.song.chord.Chord.Companion.CHORD_RANKS_AND_SHARPS
import com.stevenfrew.beatprompter.song.chord.ChordMap.Companion.NUMBER_OF_KEYS

open class KeySignatureDefinition(
	val majorKey: String,
	val relativeMinor: String,
	private val keyType: KeyType,
	val rank: Int,
	val chromaticScale: List<String>
) {
	constructor(copy: KeySignatureDefinition) : this(
		copy.majorKey,
		copy.relativeMinor,
		copy.keyType,
		copy.rank,
		copy.chromaticScale
	)

	companion object {
		private val MINOR_PATTERN = "(${Chord.MINOR_SUFFIXES})+"

		// Chromatic scale starting from C using flats only.
		private val FLAT_SCALE =
			listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "Cb")

		// Chromatic scale starting from C using sharps only.
		private val SHARP_SCALE =
			listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

		// Chromatic scale for F# major which includes E#.
		private val F_SHARP_SCALE = SHARP_SCALE.map { if (it == "F") "E#" else it }
		private val C_SHARP_SCALE = F_SHARP_SCALE.map { if (it == "C") "B#" else it }
		private val G_FLAT_SCALE = FLAT_SCALE.map { if (it == "B") "Cb" else it }
		private val C_FLAT_SCALE = G_FLAT_SCALE.map { if (it == "E") "Fb" else it }

		private val cKeySignature = KeySignatureDefinition("C", "Am", KeyType.SHARP, 0, SHARP_SCALE)
		private val dFlatKeySignature = KeySignatureDefinition("Db", "Bbm", KeyType.FLAT, 1, FLAT_SCALE)
		private val dKeySignature = KeySignatureDefinition("D", "Bm", KeyType.SHARP, 2, SHARP_SCALE)
		private val eFlatKeySignature = KeySignatureDefinition("Eb", "Cm", KeyType.FLAT, 3, FLAT_SCALE)
		private val eKeySignature = KeySignatureDefinition("E", "C#m", KeyType.SHARP, 4, SHARP_SCALE)
		private val fKeySignature = KeySignatureDefinition("F", "Dm", KeyType.FLAT, 5, FLAT_SCALE)
		private val gFlatKeySignature =
			KeySignatureDefinition("Gb", "Ebm", KeyType.FLAT, 6, G_FLAT_SCALE)
		private val fSharpKeySignature =
			KeySignatureDefinition("F#", "D#m", KeyType.SHARP, 6, F_SHARP_SCALE)
		private val gKeySignature = KeySignatureDefinition("G", "Em", KeyType.SHARP, 7, SHARP_SCALE)
		private val aFlatKeySignature = KeySignatureDefinition("Ab", "Fm", KeyType.FLAT, 8, FLAT_SCALE)
		private val aKeySignature = KeySignatureDefinition("A", "F#m", KeyType.SHARP, 9, SHARP_SCALE)
		private val bFlatKeySignature = KeySignatureDefinition("Bb", "Gm", KeyType.FLAT, 10, FLAT_SCALE)
		private val bKeySignature = KeySignatureDefinition("B", "G#m", KeyType.SHARP, 11, SHARP_SCALE)
		private val cSharpKeySignature =
			KeySignatureDefinition("C#", "A#m", KeyType.SHARP, 1, C_SHARP_SCALE)
		private val cFlatKeySignature =
			KeySignatureDefinition("Cb", "Abm", KeyType.FLAT, 11, C_FLAT_SCALE)
		private val dSharpKeySignature = KeySignatureDefinition("D#", "", KeyType.SHARP, 3, SHARP_SCALE)
		private val gSharpKeySignature = KeySignatureDefinition("G#", "", KeyType.SHARP, 8, SHARP_SCALE)

		/** Enum for each key signature. */
		private val keySignatures = mapOf(
			"C" to cKeySignature,
			"Db" to dFlatKeySignature,
			"D♭" to dFlatKeySignature,
			"D" to dKeySignature,
			"Eb" to eFlatKeySignature,
			"E♭" to eFlatKeySignature,
			"E" to eKeySignature,
			"F" to fKeySignature,
			"Gb" to gFlatKeySignature,
			"G♭" to gFlatKeySignature,
			"F#" to fSharpKeySignature,
			"F♯" to fSharpKeySignature,
			"G" to gKeySignature,
			"Ab" to aFlatKeySignature,
			"A♭" to aFlatKeySignature,
			"A" to aKeySignature,
			"Bb" to bFlatKeySignature,
			"B♭" to bFlatKeySignature,
			"B" to bKeySignature,
			"C#" to cSharpKeySignature,
			"C♯" to cSharpKeySignature,
			"Cb" to cFlatKeySignature,
			"C♭" to cFlatKeySignature,
			"D#" to dSharpKeySignature,
			"D♯" to dSharpKeySignature,
			"G#" to gSharpKeySignature,
			"G♯" to gSharpKeySignature
		)

		private val keySignatureMap = keySignatures.flatMap {
			listOf(it.value.majorKey to it.value, it.value.relativeMinor to it.value)
		}.toMap()

		private val rankMap = keySignatures.map {
			it.value.rank to it.value
		}.reversed().toMap()

		/**
		 * Returns the enum constant with the specific name or returns null if the
		 * key signature is not valid.
		 */
		fun valueOf(chord: Chord): KeySignature? {
			val foundSignature = this.keySignatureMap[chord.majorOrMinorRoot]
			if (foundSignature != null)
				return KeySignature(chord, foundSignature)

			// If all else fails, try to find any key with this chord in it.
			for (signatureKvp in keySignatures) {
				if (signatureKvp.value.chromaticScale.contains(chord.root))
					return KeySignature(chord, signatureKvp.value)
			}
			return null
		}

		internal fun forRank(rank: Int): KeySignatureDefinition? = rankMap[rank]

		private fun getKeySignature(chordName: String?): KeySignature? =
			chordName?.let { Chord.parse(it)?.let { parsedChord -> valueOf(parsedChord) } }

		fun getKeySignature(key: String?, firstChord: String?): KeySignature? =
			getKeySignature(key) ?: getKeySignature(firstChord)
	}

	/**
	 * Given the current key and the number of semitones to transpose, returns a
	 * mapping from each note to a transposed note.
	 */
	internal fun createTranspositionMap(
		newKey: KeySignatureDefinition
	): Map<String, String> {
		val semitones = semitonesTo(newKey)
		val scale: List<String> = newKey.chromaticScale
		return CHORD_RANKS_AND_SHARPS.map { it.key to scale[(it.value.first + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS] }
			.toMap()
	}

	/** Finds the number of semitones between the given keys. */
	private fun semitonesTo(other: KeySignatureDefinition): Int =
		other.rank - rank
}