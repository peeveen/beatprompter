package com.stevenfrew.beatprompter.song.chord

import java.util.regex.Pattern

class KeySignature(
	val majorKey: String,
	val relativeMinor: String,
	val keyType: KeyType,
	val rank: Int,
	val chromaticScale: List<String>
) {
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

		private val KEY_SIGNATURE_REGEX =
			Pattern.compile("${Chord.ROOT_PATTERN}(${MINOR_PATTERN})?")

		private val cKeySignature = KeySignature("C", "Am", KeyType.SHARP, 0, SHARP_SCALE)
		private val dFlatKeySignature = KeySignature("Db", "Bbm", KeyType.FLAT, 1, FLAT_SCALE)
		private val dKeySignature = KeySignature("D", "Bm", KeyType.SHARP, 2, SHARP_SCALE)
		private val eFlatKeySignature = KeySignature("Eb", "Cm", KeyType.FLAT, 3, FLAT_SCALE)
		private val eKeySignature = KeySignature("E", "C#m", KeyType.SHARP, 4, SHARP_SCALE)
		private val fKeySignature = KeySignature("F", "Dm", KeyType.FLAT, 5, FLAT_SCALE)
		private val gFlatKeySignature = KeySignature("Gb", "Ebm", KeyType.FLAT, 6, G_FLAT_SCALE)
		private val fSharpKeySignature = KeySignature("F#", "D#m", KeyType.SHARP, 6, F_SHARP_SCALE)
		private val gKeySignature = KeySignature("G", "Em", KeyType.SHARP, 7, SHARP_SCALE)
		private val aFlatKeySignature = KeySignature("Ab", "Fm", KeyType.FLAT, 8, FLAT_SCALE)
		private val aKeySignature = KeySignature("A", "F#m", KeyType.SHARP, 9, SHARP_SCALE)
		private val bFlatKeySignature = KeySignature("Bb", "Gm", KeyType.FLAT, 10, FLAT_SCALE)
		private val bKeySignature = KeySignature("B", "G#m", KeyType.SHARP, 11, SHARP_SCALE)
		private val cSharpKeySignature = KeySignature("C#", "A#m", KeyType.SHARP, 1, C_SHARP_SCALE)
		private val cFlatKeySignature = KeySignature("Cb", "Abm", KeyType.FLAT, 11, C_FLAT_SCALE)
		private val dSharpKeySignature = KeySignature("D#", "", KeyType.SHARP, 3, SHARP_SCALE)
		private val gSharpKeySignature = KeySignature("G#", "", KeyType.SHARP, 8, SHARP_SCALE)

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
		fun valueOf(name: String): KeySignature? {
			if (KEY_SIGNATURE_REGEX.matcher(name).matches()) {
				val chord = Chord.parse(name) ?: return null
				val signatureName = if (chord.isMinor) "${chord.root}m" else chord.root
				val foundSignature = this.keySignatureMap[signatureName]
				if (foundSignature != null)
					return foundSignature

				// If all else fails, try to find any key with this chord in it.
				for (signatureKvp in keySignatures) {
					if (signatureKvp.value.chromaticScale.contains(chord.root))
						return signatureKvp.value
				}
			}
			return null
		}

		fun forRank(rank: Int): KeySignature? = rankMap[rank]

		/**
		 * Transforms the given chord into a key signature.
		 */
		fun guessKeySignature(chord: Chord): KeySignature? {
			var signature = chord.root
			if (chord.isMinor) signature += 'm'
			return valueOf(signature)
		}
	}
}