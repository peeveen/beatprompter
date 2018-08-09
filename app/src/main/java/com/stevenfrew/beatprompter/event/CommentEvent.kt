package com.stevenfrew.beatprompter.event

import android.graphics.*
import com.stevenfrew.beatprompter.Comment
import com.stevenfrew.beatprompter.ScreenString

class CommentEvent(eventTime: Long, @JvmField var mComment: Comment) : BaseEvent(eventTime) {
    @JvmField var mScreenString: ScreenString?=null
    @JvmField var mTextDrawLocation: PointF?=null
    @JvmField var mPopupRect: RectF?=null

    fun doMeasurements(screenWidth: Int, screenHeight: Int, paint: Paint, face: Typeface) {
        val maxCommentBoxHeight = (screenHeight / 4.0).toInt()
        val maxTextWidth = (screenWidth * 0.9).toInt()
        val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
        mScreenString = ScreenString.create(mComment.mText, paint, maxTextWidth, maxTextHeight, Color.BLACK, face, false)
        val rectWidth = (mScreenString!!.mWidth * 1.1).toFloat()
        val rectHeight = (mScreenString!!.mHeight * 1.1).toFloat()
        val heightDiff = ((rectHeight - mScreenString!!.mHeight) / 2.0).toFloat()
        val rectX = ((screenWidth - rectWidth) / 2.0).toFloat()
        val rectY = screenHeight - rectHeight - 10
        val textWidth = mScreenString!!.mWidth
        val textX = ((screenWidth - textWidth) / 2.0).toFloat()
        val textY = rectY + rectHeight - (mScreenString!!.mDescenderOffset + heightDiff)
        mPopupRect = RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight)
        mTextDrawLocation = PointF(textX, textY)
    }
}
