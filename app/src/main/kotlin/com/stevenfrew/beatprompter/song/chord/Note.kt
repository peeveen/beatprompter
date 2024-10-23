package com.stevenfrew.beatprompter.song.chord

enum class Note {
	A,
	AFlat,
	ASharp,
	B,
	BFlat,
	BSharp,
	C,
	CFlat,
	CSharp,
	D,
	DFlat,
	DSharp,
	E,
	EFlat,
	ESharp,
	F,
	FFlat,
	FSharp,
	G,
	GFlat,
	GSharp;

	fun makeSharp(): Note =
		when (this) {
			A -> A
			AFlat -> GSharp
			ASharp -> ASharp
			B -> B
			BFlat -> ASharp
			BSharp -> BSharp
			C -> C
			CFlat -> B
			CSharp -> CSharp
			D -> D
			DFlat -> CSharp
			DSharp -> DSharp
			E -> E
			EFlat -> DSharp
			ESharp -> ESharp
			F -> F
			FFlat -> E
			FSharp -> FSharp
			G -> G
			GFlat -> FSharp
			GSharp -> GSharp
		}

	fun toDisplayString(
		alwaysUseSharps: Boolean,
		useUnicodeAccidentals: Boolean,
		majorOrMinorRootOnly: Boolean,
		suffix: String?
	): String {
		val possiblySharpenedRoot = if (alwaysUseSharps) makeSharp() else this
		val asString = when (possiblySharpenedRoot) {
			A -> "A"
			AFlat -> "Ab"
			ASharp -> "A#"
			B -> "B"
			BFlat -> "Bb"
			BSharp -> "B#"
			C -> "C"
			CFlat -> "Cb"
			CSharp -> "C#"
			D -> "D"
			DFlat -> "Db"
			DSharp -> "D#"
			E -> "E"
			EFlat -> "Eb"
			ESharp -> "E#"
			F -> "F"
			FFlat -> "Fb"
			FSharp -> "F#"
			G -> "G"
			GFlat -> "Gb"
			GSharp -> "G#"
		}
		val asMajorOrMinorString =
			if (majorOrMinorRootOnly && Chord.isMinorSuffix(suffix)) "${asString}m" else asString
		return asMajorOrMinorString.let {
			ChordUtils.replaceAccidentals(it, useUnicodeAccidentals)
		}
	}

	companion object {
		fun parse(note: String): Note =
			when (note.replace("♭", "b").replace("♯", "#").replace("♮", "")) {
				"A" -> A
				"Ab" -> AFlat
				"A#" -> ASharp
				"B" -> B
				"Bb" -> BFlat
				"B#" -> BSharp
				"C" -> C
				"Cb" -> CFlat
				"C#" -> CSharp
				"D" -> D
				"Db" -> DFlat
				"D#" -> DSharp
				"E" -> E
				"Eb" -> EFlat
				"E#" -> ESharp
				"F" -> F
				"Fb" -> FFlat
				"F#" -> FSharp
				"G" -> G
				"Gb" -> GFlat
				"G#" -> GSharp
				else -> throw IllegalStateException("Unknown note: $note")
			}
	}
}
