package com.stevenfrew.beatprompter.mock.graphics

import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.line.Line
import com.stevenfrew.beatprompter.song.line.LineMeasurements

class MockLine(
	lineTime: Long,
	lineDuration: Long,
	scrollingMode: ScrollingMode,
	songPixelPosition: Int,
	isInChorusSection: Boolean,
	yStartScrollTime: Long,
	yStopScrollTime: Long,
	displaySettings: DisplaySettings
) : Line(
	lineTime,
	lineDuration,
	scrollingMode,
	songPixelPosition,
	isInChorusSection,
	yStartScrollTime,
	yStopScrollTime,
	displaySettings
) {
	override val measurements: LineMeasurements
		get() = TODO("Not yet implemented")

	override fun renderGraphics(paint: Paint) {
		TODO("Not yet implemented")
	}
}