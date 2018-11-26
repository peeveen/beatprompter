package com.stevenfrew.beatprompter.graphics

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.util.Utils

class ScreenString private constructor(internal var mText: String, internal var mFontSize: Float, internal var mColor: Int, width: Int, height: Int, internal var mFace: Typeface, var mDescenderOffset: Int) {
    val mWidth = Math.max(0, width)
    val mHeight = Math.max(0, height)

    companion object {
        private const val MARGIN_PIXELS = 10
        private const val MASKING = true
        private const val MASKING_STRING = "X"
        private const val DOUBLE_MASKING_STRING = MASKING_STRING + MASKING_STRING

        private val boldDoubleXWidth = IntArray(Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1)
        private val regularDoubleXWidth = IntArray(Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE + 1)

        init {
            for (f in Utils.MINIMUM_FONT_SIZE..Utils.MAXIMUM_FONT_SIZE) {
                regularDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE] = -1
                boldDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE] = regularDoubleXWidth[f - Utils.MINIMUM_FONT_SIZE]
            }
        }

        private fun getTextRect(str: String, paint: Paint, r: Rect) {
            val measureWidth = paint.measureText(str)
            paint.getTextBounds(str, 0, str.length, r)
            r.left = 0
            r.right = Math.ceil(measureWidth.toDouble()).toInt()
        }

        //    static Rect singleXRect=new Rect();
        private val doubleXRect = Rect()

        private fun getDoubleXStringLength(paint: Paint, fontSize: Float, bold: Boolean): Int {
            var intFontSize = fontSize.toInt() - Utils.MINIMUM_FONT_SIZE

            // This should never happen, but let's check anyway.
            if (intFontSize < 0)
                intFontSize = 0
            else if (intFontSize >= boldDoubleXWidth.size)
                intFontSize = boldDoubleXWidth.size - 1

            var size = (if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize]
            if (size == -1) {
                getTextRect(DOUBLE_MASKING_STRING, paint, doubleXRect)
                size = doubleXRect.width()
                (if (bold) boldDoubleXWidth else regularDoubleXWidth)[intFontSize] = size
            }
            return size
        }

        private val stringWidthRect = Rect()
        internal fun getStringWidth(paint: Paint, strIn: String?, face: Typeface, fontSize: Float): Int {
            var str = strIn
            if (str == null || str.isEmpty())
                return 0
            paint.typeface = face
            paint.textSize = fontSize * Utils.FONT_SCALING
            if (MASKING)
                str = MASKING_STRING + str + MASKING_STRING
            getTextRect(str, paint, stringWidthRect)
            return stringWidthRect.width() - if (MASKING) getDoubleXStringLength(paint, fontSize, false) else 0
        }

        internal fun getBestFontSize(text: String, paint: Paint, minimumFontSize: Float, maximumFontSize: Float, maxWidth: Int, maxHeight: Int, face: Typeface): Int {
            return getBestFontSize(text, paint, minimumFontSize, maximumFontSize, maxWidth, maxHeight, face, false, null)
        }

        fun create(text: String, paint: Paint, maxWidth: Int, maxHeight: Int, color: Int, face: Typeface, bold: Boolean): ScreenString {
            val outRect = Rect()
            val fontSize = getBestFontSize(text, paint, Utils.MINIMUM_FONT_SIZE.toFloat(), Utils.MAXIMUM_FONT_SIZE.toFloat(), maxWidth, maxHeight, face, bold, outRect)
            return ScreenString(text, fontSize.toFloat(), color, outRect.width(), outRect.height() + MARGIN_PIXELS, face, outRect.bottom)
        }

        private val createRect = Rect()
        internal fun create(text: String, paint: Paint, fontSize: Float, face: Typeface, color: Int = Color.BLACK): ScreenString {
            paint.typeface = face
            paint.textSize = fontSize * Utils.FONT_SCALING
            var measureText = text
            if (MASKING)
                measureText = MASKING_STRING + text + MASKING_STRING
            getTextRect(measureText, paint, createRect)
            if (MASKING)
                createRect.right -= getDoubleXStringLength(paint, fontSize, false)
            return ScreenString(text, fontSize, color, createRect.width(), createRect.height() + MARGIN_PIXELS, face, createRect.bottom)
        }

        private fun getBestFontSize(textIn: String, paint: Paint, minimumFontSize: Float, maximumFontSize: Float, maxWidth: Int, maxHeight: Int, face: Typeface, bold: Boolean, outRect: Rect?): Int {
            var text = textIn
            if (maxWidth <= 0)
                return 0
            var hi = maximumFontSize
            var lo = minimumFontSize
            val threshold = 0.5f // How close we have to be

            var rect = outRect
            if (rect == null)
                rect = Rect()
            if (MASKING)
                text = MASKING_STRING + text + MASKING_STRING
            paint.typeface = face
            while (hi - lo > threshold) {
                val size = ((hi + lo) / 2.0).toFloat()
                val intSize = Math.floor(size.toDouble()).toInt()
                paint.textSize = intSize * Utils.FONT_SCALING
                getTextRect(text, paint, rect)
                val widthXX = (if (MASKING) getDoubleXStringLength(paint, intSize.toFloat(), bold) else 0).toFloat()
                if (rect.width() - widthXX >= maxWidth || maxHeight != -1 && rect.height() >= maxHeight - MARGIN_PIXELS)
                    hi = size // too big
                else
                    lo = size // too small
            }
            // Use lo so that we undershoot rather than overshoot
            val sizeToUse = Math.floor(lo.toDouble()).toInt()
            if (outRect != null) {
                paint.textSize = sizeToUse * Utils.FONT_SCALING
                getTextRect(text, paint, outRect)
                if (MASKING) {
                    val widthXX = getDoubleXStringLength(paint, sizeToUse.toFloat(), bold).toFloat()
                    outRect.right -= widthXX.toInt()
                }
            }
            return sizeToUse
        }
    }
}
