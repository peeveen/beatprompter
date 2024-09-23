package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.song.chord.ChordMap.Companion.NUMBER_OF_KEYS

class KeySignature(
  val chord: IChord,
  keySignatureDefinition: KeySignatureDefinition
) : KeySignatureDefinition(keySignatureDefinition) {
  fun getDisplayString(useUnicodeAccidentals: Boolean): String =
    chord.getChordDisplayString(false, useUnicodeAccidentals, true)

  /**
   * Finds the key that is a specified number of semitones above/below the current
   * key.
   */
  fun shift(
    semitones: Int
  ): KeySignature? {
    val newRank = (rank + semitones + NUMBER_OF_KEYS) % NUMBER_OF_KEYS
    val newKeySignatureDefinition = forRank(newRank)
    val transpositionMap = newKeySignatureDefinition?.let { createTranspositionMap(it) }
    val transposedChord = transpositionMap?.let { chord.transpose(it) }
    return transposedChord?.let { KeySignature(it, newKeySignatureDefinition) }
  }
}