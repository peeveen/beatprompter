package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R

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
    KeySignatureDefinition.getKeySignature(key, firstChord)
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

  fun transpose(amount: Int): ChordMap = shift(amount)

  private fun shift(semitones: Int): ChordMap {
    if (semitones == 0)
      return this
    val newKey =
      key.shift(semitones)
        ?: throw Exception(
          BeatPrompter.appResources.getString(
            R.string.couldNotShiftKey,
            key,
            semitones
          )
        )
    val newChords = transposeChords(key, newKey)
    return ChordMap(newChords, newKey, alwaysUseSharps, useUnicodeAccidentals)
  }

  private fun toKey(toKey: String): ChordMap {
    val newKey =
      Chord.parse(toKey)?.let { KeySignatureDefinition.valueOf(it) }
        ?: throw Exception(BeatPrompter.appResources.getString(R.string.failedToParseKey, toKey))
    val newChords = transposeChords(key, newKey)
    return ChordMap(newChords, newKey, alwaysUseSharps, useUnicodeAccidentals)
  }

  /**
   * Transposes the given parsed text (by the parse() function) to another key.
   */
  private fun transposeChords(
    fromKey: KeySignature,
    toKey: KeySignature
  ): Map<String, IChord> {
    val transpositionMap = fromKey.createTranspositionMap(toKey)
    return chordMap.map {
      it.key to it.value.transpose(transpositionMap)
    }.toMap()
  }

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