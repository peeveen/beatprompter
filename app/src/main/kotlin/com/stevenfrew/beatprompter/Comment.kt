package com.stevenfrew.beatprompter

import android.graphics.*

class Comment internal constructor(var mText: String, audience: List<String>, screenSize: Rect, paint: Paint, font: Typeface) {
    private val commentAudience = audience
    var mScreenString: ScreenString?=null
    var mTextDrawLocation: PointF?=null
    var mPopupRect: RectF?=null

    init {
        val maxCommentBoxHeight = (screenSize.height() / 4.0).toInt()
        val maxTextWidth = (screenSize.width() * 0.9).toInt()
        val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
        mScreenString = ScreenString.create(mText, paint, maxTextWidth, maxTextHeight, Color.BLACK, font, false)
        val rectWidth = (mScreenString!!.mWidth * 1.1).toFloat()
        val rectHeight = (mScreenString!!.mHeight * 1.1).toFloat()
        val heightDiff = ((rectHeight - mScreenString!!.mHeight) / 2.0).toFloat()
        val rectX = ((screenSize.width() - rectWidth) / 2.0).toFloat()
        val rectY = screenSize.height() - rectHeight - 10
        val textWidth = mScreenString!!.mWidth
        val textX = ((screenSize.width() - textWidth) / 2.0).toFloat()
        val textY = rectY + rectHeight - (mScreenString!!.mDescenderOffset + heightDiff)
        mPopupRect = RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight)
        mTextDrawLocation = PointF(textX, textY)
    }

    fun isIntendedFor(audience: String): Boolean {
        return commentAudience.isEmpty() ||
                audience.isBlank() ||
                audience.toLowerCase().splitAndTrim(",").intersect(commentAudience).any()
    }
}