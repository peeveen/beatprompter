package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.song.ScrollingMode
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSongParsing {
	init {
		TestUtils.setMocks()
	}

	@Test
	fun testNoLines() {
		val songFile = TestUtils.getTestFile("songs", "001-NoLines.txt")
		val exception = assertThrows<InvalidBeatPrompterFileException> { TestUtils.parseSong(songFile) }
		assertEquals("2131886489", exception.message)
	}

	@Test
	fun testBarsAndCommasOnSameLine() {
		TestUtils.testSongFileEvents("002-BarsAndCommasSameLine.txt")
	}

	@Test
	fun testTimingTrickery() {
		TestUtils.testSongFileEvents("003-TimingTrickery.txt")
	}

	@Test
	fun testScrollbeatWithBPBChange() {
		TestUtils.testSongFileEvents("004-Scrollbeat&BPBChange.txt")
	}

	@Test
	fun testBPBChange1() {
		TestUtils.testSongFileEvents("005-BPBChange1.txt")
	}

	@Test
	fun testBPBChange2() {
		TestUtils.testSongFileEvents("006-BPBChange2.txt")
	}

	@Test
	fun testScrollbeat() {
		TestUtils.testSongFileEvents("007-Scrollbeat.txt")
	}

	@Test
	fun testBPBReductionWithScrollbeatAdjustments() {
		TestUtils.testSongFileEvents("008-BPBReductionWithScrollbeatAdjustments.txt")
	}

	@Test
	fun testNoTitle() {
		val songFile = TestUtils.getTestFile("songs", "009-NoTitle.txt")
		val exception = assertThrows<InvalidBeatPrompterFileException> { TestUtils.parseSong(songFile) }
		assertEquals("2131886485 009-NoTitle.txt", exception.message)
	}

	@Test
	fun testSmoothScrolling() {
		TestUtils.testSongFileEvents("010-SmoothScrolling.txt", ScrollingMode.Smooth) {
			assertTrue(it.isSmoothScrollable)
		}
	}

	@Test
	fun testTimeTrack() {
		TestUtils.testSongFileEvents("011-TimeTrack.txt", ScrollingMode.Smooth) {
			assertTrue(it.isSmoothScrollable)
		}
	}
}