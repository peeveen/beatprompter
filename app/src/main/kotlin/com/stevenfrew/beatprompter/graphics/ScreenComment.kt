package com.stevenfrew.beatprompter.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.stevenfrew.beatprompter.util.Utils

class ScreenComment(
	private val text: String,
	screenSize: Rect,
	paint: Paint,
	font: Typeface
) {
	private val screenString: ScreenString
	private val textDrawLocation: PointF
	private val popupRect: RectF

	init {
		val maxCommentBoxHeight = (screenSize.height() / 4.0).toInt()
		val maxTextWidth = (screenSize.width() * 0.9).toInt()
		val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
		screenString =
			ScreenString.create(text, paint, maxTextWidth, maxTextHeight, Color.BLACK, font, false)
		val rectWidth = (screenString.width * 1.1).toFloat()
		val rectHeight = (screenString.height * 1.1).toFloat()
		val heightDiff = ((rectHeight - screenString.height) / 2.0).toFloat()
		val rectX = ((screenSize.width() - rectWidth) / 2.0).toFloat()
		val rectY = screenSize.height() - rectHeight - 10
		val textWidth = screenString.width
		val textX = ((screenSize.width() - textWidth) / 2.0).toFloat()
		val textY = rectY + rectHeight - (screenString.descenderOffset + heightDiff)
		popupRect = RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight)
		textDrawLocation = PointF(textX, textY)
	}

	fun draw(canvas: Canvas, paint: Paint, textColor: Int) {
		paint.apply {
			textSize = screenString.fontSize * Utils.FONT_SCALING
			flags = Paint.ANTI_ALIAS_FLAG
			color = Color.BLACK
		}
		canvas.drawRect(popupRect, paint)
		paint.color = Color.WHITE
		canvas.drawRect(
			popupRect.left + 1,
			popupRect.top + 1,
			popupRect.right - 1,
			popupRect.bottom - 1,
			paint
		)
		paint.color = textColor
		paint.alpha = 255
		canvas.drawText(text, textDrawLocation.x, textDrawLocation.y, paint)
	}
}