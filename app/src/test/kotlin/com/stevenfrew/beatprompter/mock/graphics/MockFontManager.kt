package com.stevenfrew.beatprompter.mock.graphics

import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.graphics.fonts.FontManager
import com.stevenfrew.beatprompter.graphics.fonts.TextMeasurement

class MockFontManager : FontManager {
	override fun getStringWidth(
		paint: Paint,
		strIn: String,
		fontSize: Float,
		bold: Boolean
	): Pair<Int, Rect> = 0 to Rect()

	override fun getBestFontSize(
		text: String,
		paint: Paint,
		minimumFontSize: Float,
		maximumFontSize: Float,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean
	): Pair<Int, Rect> = 0 to Rect()

	override fun measure(
		text: String,
		paint: Paint,
		fontSize: Float,
		bold: Boolean
	): TextMeasurement = TextMeasurement()

	override fun setTypeface(paint: Paint, bold: Boolean) {
		// Do nothing.
	}
}