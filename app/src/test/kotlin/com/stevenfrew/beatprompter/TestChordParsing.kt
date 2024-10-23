package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.song.chord.Chord
import com.stevenfrew.beatprompter.song.chord.InvalidChordException
import com.stevenfrew.beatprompter.song.chord.Note
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestChordParsing {
	init {
		TestUtils.setMocks()
	}

	@Test
	fun testBasicChords() {
		with(parseChord("A")) {
			assertEquals(Note.A, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A", toDisplayString(true, true))
		}
		with(parseChord("B")) {
			assertEquals(Note.B, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("C")) {
			assertEquals(Note.C, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D")) {
			assertEquals(Note.D, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("E")) {
			assertEquals(Note.E, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F")) {
			assertEquals(Note.F, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("G")) {
			assertEquals(Note.G, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testBasicSharpChords() {
		with(parseChord("A♯")) {
			assertEquals(Note.ASharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A#", toDisplayString())
		}
		with(parseChord("B#")) {
			assertEquals(Note.BSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("B♯", toDisplayString(true, true))
		}
		with(parseChord("C♯")) {
			assertEquals(Note.CSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D#")) {
			assertEquals(Note.DSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("E#")) {
			assertEquals(Note.ESharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F♯")) {
			assertEquals(Note.FSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("G#")) {
			assertEquals(Note.GSharp, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testBasicFlatChords() {
		with(parseChord("Ab")) {
			assertEquals(Note.AFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("A♭", toDisplayString(false, true))
		}
		with(parseChord("B♭")) {
			assertEquals(Note.BFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
			assertEquals("Bb", toDisplayString())
		}
		with(parseChord("Cb")) {
			assertEquals(Note.CFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("D♭")) {
			assertEquals(Note.DFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("Eb")) {
			assertEquals(Note.EFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F♭")) {
			assertEquals(Note.FFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
		with(parseChord("Gb")) {
			assertEquals(Note.GFlat, root)
			assertEquals(null, suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testComplexChords() {
		with(parseChord("G♮/D")) {
			assertEquals(Note.G, root)
			assertEquals(null, suffix)
			assertEquals(Note.D, bass)
		}
		with(parseChord("Dbm7b5")) {
			assertEquals(Note.DFlat, root)
			assertEquals("m7b5", suffix)
			assertEquals(null, bass)
			assertEquals("C♯m7♭5", toDisplayString(true, true))
		}
		with(parseChord("Dm#5")) {
			assertEquals(Note.D, root)
			assertEquals("m#5", suffix)
			assertEquals(null, bass)
		}
		with(parseChord("F#m(M7)")) {
			assertEquals(Note.FSharp, root)
			assertEquals("m(M7)", suffix)
			assertEquals(null, bass)
		}
	}

	@Test
	fun testInvalidChords() {
		assertThrows<InvalidChordException> { parseChord("H") }
		assertThrows<InvalidChordException> { parseChord("a") }
		assertThrows<InvalidChordException> { parseChord("hello") }
		assertThrows<InvalidChordException> { parseChord("D##m") }
		assertThrows<InvalidChordException> { parseChord("0x3430") }
		assertThrows<InvalidChordException> { parseChord("ABACAB") }
	}

	private fun parseChord(chord: String): Chord {
		var parsedChord = Chord.parse(chord)
		assertNotNull(parsedChord)
		return parsedChord
	}
}