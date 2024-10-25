package com.stevenfrew.beatprompter.chord

import com.stevenfrew.beatprompter.chord.Chord.Companion.CHORD_RANKS
import com.stevenfrew.beatprompter.chord.ChordMap.Companion.NUMBER_OF_KEYS

open class KeySignatureDefinition(
	val majorKey: Note,
	val relativeMinor: Note?,
	private val keyType: KeyType,
	val rank: Int,
	val chromaticScale: List<Note>
) {
	constructor(copy: KeySignatureDefinition) : this(
		copy.majorKey,
		copy.relativeMinor,
		copy.keyType,
		copy.rank,
		copy.chromaticScale
	)

	companion object {
		// Chromatic scale starting from C using flats only.
		private val FLAT_SCALE =
			listOf(
				Note.C,
				Note.DFlat,
				Note.D,
				Note.EFlat,
				Note.E,
				Note.F,
				Note.GFlat,
				Note.G,
				Note.AFlat,
				Note.A,
				Note.BFlat,
				Note.CFlat
			)

		// Chromatic scale starting from C using sharps only.
		private val SHARP_SCALE =
			listOf(
				Note.C,
				Note.CSharp,
				Note.D,
				Note.DSharp,
				Note.E,
				Note.F,
				Note.FSharp,
				Note.G,
				Note.GSharp,
				Note.A,
				Note.ASharp,
				Note.B
			)

		// Chromatic scale for F# major which includes E#.
		private val F_SHARP_SCALE = SHARP_SCALE.map { if (it == Note.F) Note.ESharp else it }
		private val C_SHARP_SCALE =
			F_SHARP_SCALE.map { if (it == Note.C) Note.BSharp else it }
		private val G_FLAT_SCALE = FLAT_SCALE.map { if (it == Note.B) Note.CFlat else it }
		private val C_FLAT_SCALE = G_FLAT_SCALE.map { if (it == Note.E) Note.FFlat else it }

		private val cKeySignature =
			KeySignatureDefinition(Note.C, Note.A, KeyType.SHARP, 0, SHARP_SCALE)
		private val dFlatKeySignature =
			KeySignatureDefinition(Note.DFlat, Note.BFlat, KeyType.FLAT, 1, FLAT_SCALE)
		private val dKeySignature =
			KeySignatureDefinition(Note.D, Note.B, KeyType.SHARP, 2, SHARP_SCALE)
		private val eFlatKeySignature =
			KeySignatureDefinition(Note.EFlat, Note.C, KeyType.FLAT, 3, FLAT_SCALE)
		private val eKeySignature =
			KeySignatureDefinition(Note.E, Note.CSharp, KeyType.SHARP, 4, SHARP_SCALE)
		private val fKeySignature = KeySignatureDefinition(Note.F, Note.D, KeyType.FLAT, 5, FLAT_SCALE)
		private val gFlatKeySignature =
			KeySignatureDefinition(Note.GFlat, Note.EFlat, KeyType.FLAT, 6, G_FLAT_SCALE)
		private val fSharpKeySignature =
			KeySignatureDefinition(Note.FSharp, Note.DSharp, KeyType.SHARP, 6, F_SHARP_SCALE)
		private val gKeySignature =
			KeySignatureDefinition(Note.G, Note.E, KeyType.SHARP, 7, SHARP_SCALE)
		private val aFlatKeySignature =
			KeySignatureDefinition(Note.AFlat, Note.F, KeyType.FLAT, 8, FLAT_SCALE)
		private val aKeySignature =
			KeySignatureDefinition(Note.A, Note.FSharp, KeyType.SHARP, 9, SHARP_SCALE)
		private val bFlatKeySignature =
			KeySignatureDefinition(Note.BFlat, Note.G, KeyType.FLAT, 10, FLAT_SCALE)
		private val bKeySignature =
			KeySignatureDefinition(Note.B, Note.GSharp, KeyType.SHARP, 11, SHARP_SCALE)
		private val cSharpKeySignature =
			KeySignatureDefinition(Note.CSharp, Note.ASharp, KeyType.SHARP, 1, C_SHARP_SCALE)
		private val cFlatKeySignature =
			KeySignatureDefinition(Note.CFlat, Note.AFlat, KeyType.FLAT, 11, C_FLAT_SCALE)
		private val dSharpKeySignature =
			KeySignatureDefinition(Note.DSharp, null, KeyType.SHARP, 3, SHARP_SCALE)
		private val gSharpKeySignature =
			KeySignatureDefinition(Note.GSharp, null, KeyType.SHARP, 8, SHARP_SCALE)

		/** Enum for each key signature. */
		private val keySignatures = mapOf(
			Note.C to cKeySignature,
			Note.DFlat to dFlatKeySignature,
			Note.D to dKeySignature,
			Note.EFlat to eFlatKeySignature,
			Note.E to eKeySignature,
			Note.F to fKeySignature,
			Note.GFlat to gFlatKeySignature,
			Note.FSharp to fSharpKeySignature,
			Note.G to gKeySignature,
			Note.AFlat to aFlatKeySignature,
			Note.A to aKeySignature,
			Note.BFlat to bFlatKeySignature,
			Note.B to bKeySignature,
			Note.CSharp to cSharpKeySignature,
			Note.CFlat to cFlatKeySignature,
			Note.DSharp to dSharpKeySignature,
			Note.GSharp to gSharpKeySignature,
		)

		private val majorKeySignatureMap = keySignatures.map {
			it.value.majorKey to it.value
		}.toMap()

		private val minorKeySignatureMap = keySignatures.map {
			it.value.relativeMinor to it.value
		}.toMap()

		private val rankMap = keySignatures.map {
			it.value.rank to it.value
		}.reversed().toMap()

		/**
		 * Returns the enum constant with the specific name or returns null if the
		 * key signature is not valid.
		 */
		fun valueOf(chord: Chord): KeySignature? {
			val isMinor = chord.isMinor
			val lookupMap = if (isMinor) minorKeySignatureMap else majorKeySignatureMap
			val foundSignature = lookupMap[chord.root]
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
			chordName?.let {
				try {
					valueOf(Chord.parse(it))
				} catch (_: InvalidChordException) {
					null
				}
			}

		fun getKeySignature(key: String?, firstChord: String?): KeySignature? =
			getKeySignature(key) ?: getKeySignature(firstChord)
	}

	/**
	 * Given the current key and the number of semitones to transpose, returns a
	 * mapping from each note to a transposed note.
	 */
	internal fun createTranspositionMap(
		newKey: KeySignatureDefinition
	): Map<Note, Note> {
		val semitones = semitonesTo(newKey)
		val scale: List<Note> = newKey.chromaticScale
		return CHORD_RANKS.map { it.key to scale[(it.value + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS] }
			.toMap()
	}

	/** Finds the number of semitones between the given keys. */
	private fun semitonesTo(other: KeySignatureDefinition): Int =
		other.rank - rank
}