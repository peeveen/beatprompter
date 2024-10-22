package com.stevenfrew.beatprompter.graphics.fonts

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.graphics.Rect
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class AndroidFontManager(
	override val minimumFontSize: Float,
	override val maximumFontSize: Float,
	override val fontScaling: Float
) : FontManager {
	companion object {
		private val NORMAL_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
		private val BOLD_TYPEFACE = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

		private const val MASKING = true
		private const val MASKING_STRING = "X"
		private const val DOUBLE_MASKING_STRING = MASKING_STRING + MASKING_STRING
	}

	private val boldDoubleXWidth: Array<Pair<Int, Rect>>
	private val regularDoubleXWidth: Array<Pair<Int, Rect>>

	init {
		boldDoubleXWidth = Array(
			ceil(maximumFontSize).toInt() - floor(minimumFontSize).toInt() + 1,
			init = { _ -> -1 to Rect() }
		)
		regularDoubleXWidth = Array(
			ceil(maximumFontSize).toInt() - floor(minimumFontSize).toInt() + 1,
			init = { _ -> -1 to Rect() }
		)
	}

	private fun getTextRect(
		str: String,
		paint: Paint,
		fontSize: Float
	): Rect {
		setTextSize(paint, fontSize)
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
		val intFontSize = (fontSize - minimumFontSize).let {
			// This should never happen, but let's check anyway.
			when {
				it < 0 -> 0
				it >= boldDoubleXWidth.size -> boldDoubleXWidth.size - 1
				else -> it
			}.toInt()
		}

		return (if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize].let {
			if (it.first == -1) {
				val doubleXRect = getTextRect(DOUBLE_MASKING_STRING, paint, fontSize)
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
		setTypeface(paint, bold)
		val str = if (MASKING) "$MASKING_STRING$strIn$MASKING_STRING" else strIn
		val stringWidthRect = getTextRect(str, paint, fontSize)
		return (stringWidthRect.width - if (MASKING) getDoubleXStringLength(
			paint,
			fontSize,
			bold
		).first else 0) to stringWidthRect
	}

	override fun measure(
		text: String,
		paint: Paint,
		fontSize: Float,
		bold: Boolean
	): TextMeasurement {
		setTypeface(paint, bold)
		val measureText = if (MASKING) "$MASKING_STRING$text$MASKING_STRING" else text
		var measureRect = getTextRect(measureText, paint, fontSize)
		if (MASKING)
			measureRect = Rect(
				measureRect.left,
				measureRect.top,
				measureRect.right - getDoubleXStringLength(paint, fontSize, bold).first,
				measureRect.bottom
			)
		val measuredWidth = max(0, measureRect.width)
		val measuredHeight = max(0, measureRect.height + FontManager.MARGIN_PIXELS)
		val measuredDescenderOffset = measureRect.bottom
		return TextMeasurement(measureRect, measuredWidth, measuredHeight, measuredDescenderOffset)
	}

	override fun setTypeface(paint: Paint, bold: Boolean) {
		paint.typeface = if (bold) BOLD_TYPEFACE else NORMAL_TYPEFACE
	}

	override fun setTextSize(paint: Paint, size: Float) {
		paint.textSize = size * fontScaling
	}

	override fun getBestFontSize(
		text: String,
		paint: Paint,
		maxWidth: Int,
		maxHeight: Int,
		bold: Boolean
	): Pair<Int, Rect> =
		getBestFontSize(text, paint, minimumFontSize, maximumFontSize, maxWidth, maxHeight, bold)

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
		setTypeface(paint, bold)
		while (hi - lo > threshold) {
			val size = ((hi + lo) / 2.0).toFloat()
			val intSize = floor(size.toDouble()).toInt()
			val bestFontSizeRect = getTextRect(maskedText, paint, intSize.toFloat())
			val widthXX =
				(if (MASKING) getDoubleXStringLength(paint, intSize.toFloat(), bold).first else 0).toFloat()
			if (bestFontSizeRect.width - widthXX >= maxWidth || maxHeight != -1 && bestFontSizeRect.height >= maxHeight - FontManager.MARGIN_PIXELS)
				hi = size // too big
			else
				lo = size // too small
		}
		// Use lo so that we undershoot rather than overshoot
		val sizeToUse = floor(lo.toDouble()).toInt()
		var bestFontSizeRect = getTextRect(maskedText, paint, sizeToUse.toFloat())
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