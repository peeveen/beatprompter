package com.stevenfrew.beatprompter.graphics

import android.graphics.*
import com.stevenfrew.beatprompter.util.Utils

class ScreenComment(private val mText: String,
                    screenSize: Rect,
                    paint: Paint,
                    font: Typeface) {
    private val mScreenString: ScreenString
    private val mTextDrawLocation: PointF
    private val mPopupRect: RectF

    init {
        val maxCommentBoxHeight = (screenSize.height() / 4.0).toInt()
        val maxTextWidth = (screenSize.width() * 0.9).toInt()
        val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
        mScreenString = ScreenString.create(mText, paint, maxTextWidth, maxTextHeight, Color.BLACK, font, false)
        val rectWidth = (mScreenString.mWidth * 1.1).toFloat()
        val rectHeight = (mScreenString.mHeight * 1.1).toFloat()
        val heightDiff = ((rectHeight - mScreenString.mHeight) / 2.0).toFloat()
        val rectX = ((screenSize.width() - rectWidth) / 2.0).toFloat()
        val rectY = screenSize.height() - rectHeight - 10
        val textWidth = mScreenString.mWidth
        val textX = ((screenSize.width() - textWidth) / 2.0).toFloat()
        val textY = rectY + rectHeight - (mScreenString.mDescenderOffset + heightDiff)
        mPopupRect = RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight)
        mTextDrawLocation = PointF(textX, textY)
    }

    fun draw(canvas: Canvas, paint: Paint, textColor: Int) {
        with(paint)
        {
            textSize = mScreenString.mFontSize * Utils.FONT_SCALING
            flags = Paint.ANTI_ALIAS_FLAG
            color = Color.BLACK
        }
        canvas.drawRect(mPopupRect, paint)
        paint.color = Color.WHITE
        canvas.drawRect(mPopupRect.left + 1, mPopupRect.top + 1, mPopupRect.right - 1, mPopupRect.bottom - 1, paint)
        paint.color = textColor
        paint.alpha = 255
        canvas.drawText(mText, mTextDrawLocation.x, mTextDrawLocation.y, paint)
    }
}