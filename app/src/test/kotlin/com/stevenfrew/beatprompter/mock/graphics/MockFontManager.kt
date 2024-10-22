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
	): Pair<Int, Rect> = DEFAULT_WIDTH to DEFAULT_RECT

	override fun getBestFontSize(
		text: String,
		paint: Paint,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean,
		minimumFontSize: Float?,
		maximumFontSize: Float?,
	): Pair<Int, Rect> = DEFAULT_FONT_SIZE to DEFAULT_RECT

	override fun measure(
		text: String,
		paint: Paint,
		fontSize: Float,
		bold: Boolean
	): TextMeasurement =
		TextMeasurement(DEFAULT_RECT, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_DESCENDER_OFFSET)

	override fun setTypeface(paint: Paint, bold: Boolean) {
		// Do nothing.
	}

	override fun setTextSize(paint: Paint, size: Float) {
		// Do nothing.
	}

	override val maximumFontSize: Float = 8.0f
	override val minimumFontSize: Float = 150.0f
	override val fontScaling: Float = 1.0f

	companion object {
		private const val DEFAULT_FONT_SIZE = 24
		private const val DEFAULT_WIDTH = 600
		private const val DEFAULT_HEIGHT = 100
		private const val DEFAULT_DESCENDER_OFFSET = 20
		private val DEFAULT_RECT = Rect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT)
	}
}