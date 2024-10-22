package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class TestSongParsing {
	@Test
	fun testOnlyATitle() {
		val songFile = TestUtils.getTestFile("songs", "001-OnlyATitle.txt")
		assertThrows<InvalidBeatPrompterFileException> { TestUtils.parseSong(songFile) }
	}

	@Test
	fun testBarsAndCommasOnSameLine() = TestUtils.testSongFileEvents("002-BarsAndCommasSameLine.txt")

	@Test
	fun testTimingTrickery() = TestUtils.testSongFileEvents("003-TimingTrickery.txt")

	@Test
	fun testScrollbeatWithBPBChange() = TestUtils.testSongFileEvents("004-Scrollbeat&BPBChange.txt")

	@Test
	fun testBPBChange1() = TestUtils.testSongFileEvents("005-BPBChange1.txt")

	@Test
	fun testBPBChange2() = TestUtils.testSongFileEvents("006-BPBChange2.txt")

	@Test
	fun testScrollbeat() = TestUtils.testSongFileEvents("007-Scrollbeat.txt")

	@Test
	fun testBPBReductionWithScrollbeatAdjustments() =
		TestUtils.testSongFileEvents("008-BPBReductionWithScrollbeatAdjustments.txt")
}