package com.stevenfrew.beatprompter.graphics

import android.graphics.Paint
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.graphics.fonts.FontManager
import kotlin.math.max

class ScreenString private constructor(
	val text: String,
	val fontSize: Float,
	val color: Int,
	width: Int,
	height: Int,
	val descenderOffset: Int,
	val bold: Boolean
) {
	val width = max(0, width)
	val height = max(0, height)

	companion object {
		fun create(
			text: String,
			paint: Paint,
			maxWidth: Int,
			maxHeight: Int,
			color: Int,
			bold: Boolean = false
		): ScreenString {
			val (fontSize, bestFontSizeRect) = BeatPrompter.platformUtils.fontManager.getBestFontSize(
				text,
				paint,
				maxWidth,
				maxHeight,
				bold
			)
			return ScreenString(
				text,
				fontSize.toFloat(),
				color,
				bestFontSizeRect.width,
				bestFontSizeRect.height + FontManager.MARGIN_PIXELS,
				bestFontSizeRect.bottom,
				bold
			)
		}
	}
}