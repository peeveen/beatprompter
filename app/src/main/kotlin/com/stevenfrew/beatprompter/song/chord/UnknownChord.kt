package com.stevenfrew.beatprompter.song.chord

class UnknownChord(private val chord: String) : IChord {
  override fun getChordDisplayString(
    alwaysUseSharps: Boolean,
    useUnicodeAccidentals: Boolean,
    rootOnly: Boolean
  ): String = if (useUnicodeAccidentals) ChordUtils.useUnicodeAccidentals(chord) else chord

  override fun transpose(transpositionMap: Map<String, String>): IChord = this
}