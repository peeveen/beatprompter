package com.stevenfrew.beatprompter.graphics.fonts

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object AndroidFontManager : FontManager {
	private val DEFAULT_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
	private val BOLD_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

	private const val MASKING = true
	private const val MASKING_STRING = "X"
	private const val DOUBLE_MASKING_STRING = MASKING_STRING + MASKING_STRING

	private val boldDoubleXWidth = Array(
		Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1,
		init = { _ -> -1 to Rect() }
	)
	private val regularDoubleXWidth = Array(
		Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1,
		init = { _ -> -1 to Rect() }
	)

	private fun getTextRect(
		str: String,
		paint: Paint
	): Rect {
		val measureWidth = paint.measureText(str)
		val androidRect = android.graphics.Rect()
		paint.getTextBounds(str, 0, str.length, androidRect)
		androidRect.left = 0
		androidRect.right = ceil(measureWidth.toDouble()).toInt()
		return Rect(androidRect)
	}

	private fun getDoubleXStringLength(
		paint: Paint,
		fontSize: Float,
		bold: Boolean
	): Pair<Int, Rect> {
		val intFontSize = (fontSize.toInt() - Utils.MINIMUM_FONT_SIZE).let {
			// This should never happen, but let's check anyway.
			when {
				it < 0 -> 0
				it >= boldDoubleXWidth.size -> boldDoubleXWidth.size - 1
				else -> it
			}
		}

		return (if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize].let {
			if (it.first == -1) {
				val doubleXRect = getTextRect(DOUBLE_MASKING_STRING, paint)
				val newSize = doubleXRect.width to doubleXRect
				(if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize] = newSize
				newSize
			} else
				it
		}
	}

	override fun getStringWidth(
		paint: Paint,
		strIn: String,
		fontSize: Float,
		bold: Boolean
	): Pair<Int, Rect> {
		if (strIn.isEmpty())
			return 0 to Rect()
		paint.typeface = DEFAULT_TYPEFACE
		paint.textSize = fontSize * Utils.FONT_SCALING
		val str = if (MASKING) "$MASKING_STRING$strIn$MASKING_STRING" else strIn
		val stringWidthRect = getTextRect(str, paint)
		return (stringWidthRect.width - if (MASKING) getDoubleXStringLength(
			paint,
			fontSize,
			false
		).first else 0) to stringWidthRect
	}

	override fun measure(
		text: String,
		paint: Paint,
		fontSize: Float,
		bold: Boolean
	): TextMeasurement {
		paint.typeface = DEFAULT_TYPEFACE
		paint.textSize = fontSize * Utils.FONT_SCALING
		val measureText = if (MASKING) "$MASKING_STRING$text$MASKING_STRING" else text
		var measureRect = getTextRect(measureText, paint)
		if (MASKING)
			measureRect = Rect(
				measureRect.left,
				measureRect.top,
				measureRect.right - getDoubleXStringLength(paint, fontSize, false).first,
				measureRect.bottom
			)
		val measuredWidth = max(0, measureRect.width)
		val measuredHeight = max(0, measureRect.height + FontManager.MARGIN_PIXELS)
		val measuredDescenderOffset = measureRect.bottom
		return TextMeasurement(measureRect, measuredWidth, measuredHeight, measuredDescenderOffset)
	}

	override fun setTypeface(paint: Paint, bold: Boolean) {
		paint.typeface = if (bold) BOLD_TYPEFACE else DEFAULT_TYPEFACE
	}

	override fun getBestFontSize(
		text: String,
		paint: Paint,
		minimumFontSize: Float,
		maximumFontSize: Float,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean
	): Pair<Int, Rect> {
		if (maxWidth <= 0)
			return 0 to Rect()
		var hi = maximumFontSize
		var lo = minimumFontSize
		val threshold = 0.5f // How close we have to be

		val maskedText = if (MASKING) "$MASKING_STRING$text$MASKING_STRING" else text
		paint.typeface = if (bold) BOLD_TYPEFACE else DEFAULT_TYPEFACE
		while (hi - lo > threshold) {
			val size = ((hi + lo) / 2.0).toFloat()
			val intSize = floor(size.toDouble()).toInt()
			paint.textSize = intSize * Utils.FONT_SCALING
			val bestFontSizeRect = getTextRect(maskedText, paint)
			val widthXX =
				(if (MASKING) getDoubleXStringLength(paint, intSize.toFloat(), bold).first else 0).toFloat()
			if (bestFontSizeRect.width - widthXX >= maxWidth || maxHeight != -1 && bestFontSizeRect.height >= maxHeight - FontManager.MARGIN_PIXELS)
				hi = size // too big
			else
				lo = size // too small
		}
		// Use lo so that we undershoot rather than overshoot
		val sizeToUse = floor(lo.toDouble()).toInt()
		paint.textSize = sizeToUse * Utils.FONT_SCALING
		var bestFontSizeRect = getTextRect(maskedText, paint)
		if (MASKING) {
			val widthXX = getDoubleXStringLength(paint, sizeToUse.toFloat(), bold).first.toFloat()
			bestFontSizeRect = Rect(
				bestFontSizeRect.left,
				bestFontSizeRect.top,
				bestFontSizeRect.right - widthXX.toInt(),
				bestFontSizeRect.bottom
			)
		}
		return sizeToUse to bestFontSizeRect
	}
}