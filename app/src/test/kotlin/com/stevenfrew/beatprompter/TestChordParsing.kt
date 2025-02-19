package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.chord.Chord
import com.stevenfrew.beatprompter.chord.InvalidChordException
import com.stevenfrew.beatprompter.chord.UnknownChord
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestChordParsing {
	init {
		TestUtils
	}

	@Test
	fun testBasicChords() {
		with(parseChord("A")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.A, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A", toDisplayString(alwaysUseSharps = true, useUnicodeAccidentals = true))
		}
		with(parseChord("B")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.B, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("C")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.C, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.D, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("E")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.E, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.F, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("G")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.G, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testBasicSharpChords() {
		with(parseChord("A♯")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.ASharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A#", toDisplayString())
		}
		with(parseChord("B#")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.BSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("B♯", toDisplayString(alwaysUseSharps = true, useUnicodeAccidentals = true))
		}
		with(parseChord("C♯")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.CSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D#")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.DSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("E#")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.ESharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F♯")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.FSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("G#")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.GSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testBasicFlatChords() {
		with(parseChord("Ab")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.AFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A♭", toDisplayString(alwaysUseSharps = false, useUnicodeAccidentals = true))
		}
		with(parseChord("B♭")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.BFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("Bb", toDisplayString())
		}
		with(parseChord("Cb")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.CFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D♭")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.DFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("Eb")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.EFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F♭")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.FFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("Gb")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.GFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testComplexChords() {
		with(parseChord("G♮/D")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.G, root)
			assertEquals(null, suffix)
			assertEquals(com.stevenfrew.beatprompter.chord.Note.D, bass)
		}
		with(parseChord("Dbm7b5")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.DFlat, root)
			assertEquals("m7b5", suffix)
			assertEquals(null, bass)
			assertEquals("C♯m7♭5", toDisplayString(alwaysUseSharps = true, useUnicodeAccidentals = true))
		}
		with(parseChord("Dm#5")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.D, root)
			assertEquals("m#5", suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F#m(M7)")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.FSharp, root)
			assertEquals("m(M7)", suffix)
			assertEquals(null, bass)
		}
		with(parseChord("Dbmaj7sus2/F#")) {
			assertEquals(com.stevenfrew.beatprompter.chord.Note.DFlat, root)
			assertEquals("maj7sus2", suffix)
			assertEquals(com.stevenfrew.beatprompter.chord.Note.FSharp, bass)
		}
	}

	@Test
	fun testInvalidChordDisplayString() {
		val nonUnicodeChordString = "blah#"
		val nonUnicodeChord = UnknownChord(nonUnicodeChordString)
		assertEquals(
			nonUnicodeChordString,
			nonUnicodeChord.toDisplayString(alwaysUseSharps = true, useUnicodeAccidentals = true)
		)
		assertEquals(
			nonUnicodeChordString,
			nonUnicodeChord.toDisplayString(alwaysUseSharps = false, useUnicodeAccidentals = false)
		)
		val unicodeChordString = "♭lah♯"
		val unicodeChord = UnknownChord(unicodeChordString)
		assertEquals(
			unicodeChordString,
			unicodeChord.toDisplayString(alwaysUseSharps = true, useUnicodeAccidentals = true)
		)
		assertEquals(
			unicodeChordString,
			unicodeChord.toDisplayString(alwaysUseSharps = false, useUnicodeAccidentals = false)
		)
	}

	@Test
	fun testInvalidChords() {
		assertThrows<InvalidChordException> { parseChord("H") }
		assertThrows<InvalidChordException> { parseChord("a") }
		assertThrows<InvalidChordException> { parseChord("hello") }
		assertThrows<InvalidChordException> { parseChord("blah#") }
		assertThrows<InvalidChordException> { parseChord("D##m") }
		assertThrows<InvalidChordException> { parseChord("0x3430") }
		assertThrows<InvalidChordException> { parseChord("ABACAB") }
	}

	private fun parseChord(chord: String): Chord {
		val parsedChord = Chord.parse(chord)
		assertNotNull(parsedChord)
		return parsedChord
	}
}