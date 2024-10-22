package com.stevenfrew.beatprompter.graphics

import android.graphics.Paint
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.graphics.fonts.FontManager
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.max

class ScreenString private constructor(
	internal val text: String,
	internal val fontSize: Float,
	internal val color: Int,
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
			val (fontSize, bestFontSizeRect) = BeatPrompter.fontManager.getBestFontSize(
				text,
				paint,
				Utils.MINIMUM_FONT_SIZE.toFloat(),
				Utils.MAXIMUM_FONT_SIZE.toFloat(),
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