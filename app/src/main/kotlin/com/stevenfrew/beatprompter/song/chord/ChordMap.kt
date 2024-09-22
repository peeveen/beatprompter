package com.stevenfrew.beatprompter.song.chord

class ChordMap(private val chordMap: Map<String, Chord>, private val key: KeySignature) :
	Map<String, Chord> {

	constructor(chordStrings: Set<String>, firstChord: String, key: String? = null) : this(
		chordStrings.mapNotNull { Chord.parse(it)?.let { parsedChord -> it to parsedChord } }.toMap(),
		key?.let { KeySignature.valueOf(it) } ?: Chord.parse(
			firstChord
		)?.let { KeySignature.guessKeySignature(it) }
		?: throw Exception("Could not determine key signature")
	)

	fun fromKey(key: KeySignature): ChordMap = ChordMap(chordMap, key)

	fun shift(semitones: Int): ChordMap {
		val newKey =
			transposeKey(key, semitones)
				?: throw Exception("Failed to calculate new key (shifting $key by $semitones semitones)")
		val newChords = transposeChords(key, newKey)
		return ChordMap(newChords, newKey)
	}

	fun toKey(toKey: String): ChordMap {
		val newKey =
			KeySignature.valueOf(toKey) ?: throw Exception("$toKey could not be parsed as a key")
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
	): Map<String, Chord> {
		val transpositionMap = createTranspositionMap(fromKey, toKey)
		return chordMap.map {
			it.key to
				Chord(
					transpositionMap[it.value.root] ?: it.value.root,
					it.value.suffix,
					transpositionMap[it.value.bass]
				)
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
		return CHORD_RANKS.map { it.key to scale[(it.value + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS] }
			.toMap()
	}

	/** Finds the number of semitones between the given keys. */
	private fun semitonesBetween(a: KeySignature, b: KeySignature): Int = b.rank - a.rank

	override val entries: Set<Map.Entry<String, Chord>>
		get() = chordMap.entries
	override val keys: Set<String>
		get() = chordMap.keys
	override val size: Int
		get() = chordMap.size
	override val values: Collection<Chord>
		get() = chordMap.values

	override fun isEmpty(): Boolean = chordMap.isEmpty()
	override fun get(key: String): Chord? = chordMap[key]
	override fun containsValue(value: Chord): Boolean = chordMap.containsValue(value)
	override fun containsKey(key: String): Boolean = chordMap.containsKey(key)

	companion object {
		const val NUMBER_OF_KEYS = 12

		/**
		 * The rank for each possible chord. Rank is the distance in semitones from C.
		 */
		val CHORD_RANKS: Map<String, Int> = mapOf(
			"B#" to 0,
			"B♯" to 0,
			"C" to 0,
			"C#" to 1,
			"C♯" to 1,
			"Db" to 1,
			"D♭" to 1,
			"D" to 2,
			"D#" to 3,
			"D♯" to 3,
			"Eb" to 3,
			"E♭" to 3,
			"E" to 4,
			"Fb" to 4,
			"F♭" to 4,
			"E#" to 5,
			"E♯" to 5,
			"F" to 5,
			"F#" to 6,
			"F♯" to 6,
			"Gb" to 6,
			"G♭" to 6,
			"G" to 7,
			"G#" to 8,
			"G♯" to 8,
			"Ab" to 8,
			"A♭" to 8,
			"A" to 9,
			"A#" to 10,
			"A♯" to 10,
			"Bb" to 10,
			"B♭" to 10,
			"Cb" to 11,
			"C♭" to 11,
			"B" to 11
		)
	}
}