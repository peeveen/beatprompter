package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.song.chord.Chord.Companion.CHORD_RANKS_AND_SHARPS

class ChordMap private constructor(
	private val chordMap: Map<String, IChord>,
	private val key: KeySignature,
	private val alwaysUseSharps: Boolean = false,
	private val useUnicodeAccidentals: Boolean = false
) : Map<String, IChord> {
	constructor(chordStrings: Set<String>, firstChord: String, key: String? = null) : this(
		chordStrings.associateWith {
			(Chord.parse(it) ?: UnknownChord(it))
		},
		key?.let { KeySignature.valueOf(it) } ?: Chord.parse(
			firstChord
		)?.let { KeySignature.guessKeySignature(it) }
		?: throw Exception("Could not determine key signature"),
		Preferences.alwaysDisplaySharpChords,
		Preferences.displayUnicodeAccidentals,
	)

	fun transpose(amount: String): ChordMap =
		try {
			val shiftAmount = amount.toInt()
			if (shiftAmount < NUMBER_OF_KEYS && shiftAmount > -NUMBER_OF_KEYS)
				shift(shiftAmount)
			else
				throw Exception(BeatPrompter.appResources.getString(R.string.excessiveTransposeMagnitude))
		} catch (nfe: NumberFormatException) {
			// Must be a key then!
			toKey(amount)
		}

	private fun shift(semitones: Int): ChordMap {
		val newKey =
			transposeKey(key, semitones)
				?: throw Exception(
					BeatPrompter.appResources.getString(
						R.string.couldNotShiftKey,
						key,
						semitones
					)
				)
		val newChords = transposeChords(key, newKey)
		return ChordMap(newChords, newKey)
	}

	private fun toKey(toKey: String): ChordMap {
		val newKey =
			KeySignature.valueOf(toKey)
				?: throw Exception(BeatPrompter.appResources.getString(R.string.failedToParseKey, toKey))
		val newChords = transposeChords(key, newKey)
		return ChordMap(newChords, newKey)
	}

	/**
	 * Finds the key that is a specified number of semitones above/below the current
	 * key.
	 */
	private fun transposeKey(
		currentKey: KeySignature,
		semitones: Int
	): KeySignature? {
		val newRank = (currentKey.rank + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS
		return KeySignature.forRank(newRank)
	}

	/**
	 * Transposes the given parsed text (by the parse() function) to another key.
	 */
	private fun transposeChords(
		fromKey: KeySignature,
		toKey: KeySignature
	): Map<String, IChord> {
		val transpositionMap = createTranspositionMap(fromKey, toKey)
		return chordMap.map {
			it.key to it.value.transpose(transpositionMap)
		}.toMap()
	}

	/**
	 * Given the current key and the number of semitones to transpose, returns a
	 * mapping from each note to a transposed note.
	 */
	private fun createTranspositionMap(
		currentKey: KeySignature,
		newKey: KeySignature
	): Map<String, String> {
		val semitones = semitonesBetween(currentKey, newKey)
		val scale: List<String> = newKey.chromaticScale
		return CHORD_RANKS_AND_SHARPS.map { it.key to scale[(it.value.first + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS] }
			.toMap()
	}

	/** Finds the number of semitones between the given keys. */
	private fun semitonesBetween(a: KeySignature, b: KeySignature): Int = b.rank - a.rank

	fun getChordDisplayString(chord: String): String? =
		get(chord)?.getChordDisplayString(alwaysUseSharps, useUnicodeAccidentals)

	fun addChordMapping(fromChord: String, toChord: IChord): ChordMap {
		val mutableMap = toMutableMap()
		mutableMap[fromChord] = toChord
		return ChordMap(mutableMap, key, alwaysUseSharps, useUnicodeAccidentals)
	}

	override val entries: Set<Map.Entry<String, IChord>>
		get() = chordMap.entries
	override val keys: Set<String>
		get() = chordMap.keys
	override val size: Int
		get() = chordMap.size
	override val values: Collection<IChord>
		get() = chordMap.values

	override fun isEmpty(): Boolean = chordMap.isEmpty()
	override fun get(key: String): IChord? = chordMap[key]
	override fun containsValue(value: IChord): Boolean = chordMap.containsValue(value)
	override fun containsKey(key: String): Boolean = chordMap.containsKey(key)

	companion object {
		const val NUMBER_OF_KEYS = 12
	}
}