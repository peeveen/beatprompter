package com.stevenfrew.beatprompter.event

import android.graphics.*
import com.stevenfrew.beatprompter.Comment
import com.stevenfrew.beatprompter.ScreenString

class CommentEvent(eventTime: Long, var mComment: Comment) : BaseEvent(eventTime) {
    var mScreenString: ScreenString?=null
    var mTextDrawLocation: PointF?=null
    var mPopupRect: RectF?=null

    fun doMeasurements(screenSize:Rect, paint: Paint, face: Typeface) {
        val maxCommentBoxHeight = (screenSize.height() / 4.0).toInt()
        val maxTextWidth = (screenSize.width() * 0.9).toInt()
        val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
        mScreenString = ScreenString.create(mComment.mText, paint, maxTextWidth, maxTextHeight, Color.BLACK, face, false)
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
}
