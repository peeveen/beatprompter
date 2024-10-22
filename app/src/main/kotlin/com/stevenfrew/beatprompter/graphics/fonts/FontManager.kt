package com.stevenfrew.beatprompter.graphics.fonts

import android.graphics.Paint
import com.stevenfrew.beatprompter.graphics.Rect

interface FontManager {
	fun getStringWidth(
		paint: Paint,
		strIn: String,
		fontSize: Float,
		bold: Boolean = false
	): Pair<Int, Rect>

	fun getBestFontSize(
		text: String,
		paint: Paint,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean = false
	): Pair<Int, Rect>

	fun getBestFontSize(
		text: String,
		paint: Paint,
		minimumFontSize: Float,
		maximumFontSize: Float,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean = false
	): Pair<Int, Rect>

	fun measure(
		text: String,
		paint: Paint,
		fontSize: Float,
		bold: Boolean = false
	): TextMeasurement

	fun setTypeface(
		paint: Paint,
		bold: Boolean = false
	)

	fun setTextSize(
		paint: Paint,
		size: Float
	)

	val maximumFontSize: Float
	val minimumFontSize: Float
	val fontScaling: Float

	companion object {
		const val MARGIN_PIXELS = 10
	}
}