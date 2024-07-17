package com.stevenfrew.beatprompter.graphics

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class ScreenString private constructor(
	internal val mText: String,
	internal val mFontSize: Float,
	internal val mColor: Int,
	width: Int,
	height: Int,
	internal val mFace: Typeface,
	val mDescenderOffset: Int
) {
	val mWidth = max(0, width)
	val mHeight = max(0, height)

	companion object {
		private const val MARGIN_PIXELS = 10
		private const val MASKING = true
		private const val MASKING_STRING = "X"
		private const val DOUBLE_MASKING_STRING = MASKING_STRING + MASKING_STRING

		private val boldDoubleXWidth = IntArray(Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1)
		private val regularDoubleXWidth =
			IntArray(Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1)

		init {
			for (f in Utils.MINIMUM_FONT_SIZE..Utils.MAXIMUM_FONT_SIZE) {
				regularDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE] = -1
				boldDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE] =
					regularDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE]
			}
		}

		private fun getTextRect(
			str: String,
			paint: Paint, r: Rect
		) {
			val measureWidth = paint.measureText(str)
			paint.getTextBounds(str, 0, str.length, r)
			r.left = 0
			r.right = ceil(measureWidth.toDouble()).toInt()
		}

		private val doubleXRect = Rect()
		private fun getDoubleXStringLength(
			paint: Paint,
			fontSize: Float,
			bold: Boolean
		): Int {
			val intFontSize = (fontSize.toInt() - Utils.MINIMUM_FONT_SIZE).let {
				// This should never happen, but let's check anyway.
				when {
					it < 0 -> 0
					it >= boldDoubleXWidth.size -> boldDoubleXWidth.size - 1
					else -> it
				}
			}

			return (if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize].let {
				if (it == -1) {
					getTextRect(DOUBLE_MASKING_STRING, paint, doubleXRect)
					val newSize = doubleXRect.width()
					(if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize] = newSize
					newSize
				} else
					it
			}
		}

		private val stringWidthRect = Rect()
		internal fun getStringWidth(
			paint: Paint,
			strIn: String?,
			face: Typeface,
			fontSize: Float
		): Int {
			if (strIn.isNullOrEmpty())
				return 0
			paint.typeface = face
			paint.textSize = fontSize * Utils.FONT_SCALING
			val str = if (MASKING) "$MASKING_STRING$strIn$MASKING_STRING" else strIn
			getTextRect(str, paint, stringWidthRect)
			return stringWidthRect.width() - if (MASKING) getDoubleXStringLength(
				paint,
				fontSize,
				false
			) else 0
		}

		internal fun getBestFontSize(
			text: String,
			paint: Paint,
			minimumFontSize: Float,
			maximumFontSize: Float,
			maxWidth: Int,
			maxHeight: Int,
			face: Typeface
		): Int =
			getBestFontSize(
				text,
				paint,
				minimumFontSize,
				maximumFontSize,
				maxWidth,
				maxHeight,
				face,
				false
			)

		fun create(
			text: String,
			paint: Paint,
			maxWidth: Int,
			maxHeight: Int,
			color: Int,
			face: Typeface,
			bold: Boolean
		): ScreenString {
			val fontSize = getBestFontSize(
				text,
				paint,
				Utils.MINIMUM_FONT_SIZE.toFloat(),
				Utils.MAXIMUM_FONT_SIZE.toFloat(),
				maxWidth,
				maxHeight,
				face,
				bold
			)
			return ScreenString(
				text,
				fontSize.toFloat(),
				color,
				bestFontSizeRect.width(),
				bestFontSizeRect.height() + MARGIN_PIXELS,
				face,
				bestFontSizeRect.bottom
			)
		}

		private val measureRect = Rect()
		var mMeasuredWidth = 0
		var mMeasuredHeight = 0
		var mMeasuredDescenderOffset = 0
		fun measure(
			text: String,
			paint: Paint,
			fontSize: Float,
			face: Typeface
		) {
			paint.typeface = face
			paint.textSize = fontSize * Utils.FONT_SCALING
			val measureText = if (MASKING) "$MASKING_STRING$text$MASKING_STRING" else text
			getTextRect(measureText, paint, measureRect)
			if (MASKING)
				measureRect.right -= getDoubleXStringLength(paint, fontSize, false)
			mMeasuredWidth = max(0, measureRect.width())
			mMeasuredHeight = max(0, measureRect.height() + MARGIN_PIXELS)
			mMeasuredDescenderOffset = measureRect.bottom
		}

		private val bestFontSizeRect = Rect()
		private fun getBestFontSize(
			textIn: String,
			paint: Paint,
			minimumFontSize: Float,
			maximumFontSize: Float,
			maxWidth: Int,
			maxHeight: Int,
			face: Typeface,
			bold: Boolean
		): Int {
			if (maxWidth <= 0)
				return 0
			var hi = maximumFontSize
			var lo = minimumFontSize
			val threshold = 0.5f // How close we have to be

			val text = if (MASKING) "$MASKING_STRING$textIn$MASKING_STRING" else textIn
			paint.typeface = face
			while (hi - lo > threshold) {
				val size = ((hi + lo) / 2.0).toFloat()
				val intSize = floor(size.toDouble()).toInt()
				paint.textSize = intSize * Utils.FONT_SCALING
				getTextRect(text, paint, bestFontSizeRect)
				val widthXX =
					(if (MASKING) getDoubleXStringLength(paint, intSize.toFloat(), bold) else 0).toFloat()
				if (bestFontSizeRect.width() - widthXX >= maxWidth || maxHeight != -1 && bestFontSizeRect.height() >= maxHeight - MARGIN_PIXELS)
					hi = size // too big
				else
					lo = size // too small
			}
			// Use lo so that we undershoot rather than overshoot
			val sizeToUse = floor(lo.toDouble()).toInt()
			paint.textSize = sizeToUse * Utils.FONT_SCALING
			getTextRect(text, paint, bestFontSizeRect)
			if (MASKING) {
				val widthXX = getDoubleXStringLength(paint, sizeToUse.toFloat(), bold).toFloat()
				bestFontSizeRect.right -= widthXX.toInt()
			}
			return sizeToUse
		}
	}
}
