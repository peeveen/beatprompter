package com.stevenfrew.beatprompter.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.util.inflate

class ScreenComment(
	private val text: String,
	screenSize: Rect,
	paint: Paint
) {
	private val screenString: ScreenString
	private val textDrawLocation: PointF
	private val popupRect: RectF

	init {
		val maxCommentBoxHeight = (screenSize.height / 4.0).toInt()
		val maxTextWidth = (screenSize.width * 0.9).toInt()
		val maxTextHeight = (maxCommentBoxHeight * 0.9).toInt()
		screenString =
			ScreenString.create(text, paint, maxTextWidth, maxTextHeight, Color.BLACK)
		val rectWidth = (screenString.width * 1.1).toFloat()
		val rectHeight = (screenString.height * 1.1).toFloat()
		val heightDiff = ((rectHeight - screenString.height) / 2.0).toFloat()
		val rectX = ((screenSize.width - rectWidth) / 2.0).toFloat()
		val rectY = screenSize.height - rectHeight - (screenSize.height * 0.05).toInt()
		val textWidth = screenString.width
		val textX = ((screenSize.width - textWidth) / 2.0).toFloat()
		val textY = rectY + rectHeight - (screenString.descenderOffset + heightDiff)
		popupRect = RectF(rectX, rectY, rectX + rectWidth, rectY + rectHeight)
		textDrawLocation = PointF(textX, textY)
	}

	fun draw(canvas: Canvas, paint: Paint, textColor: Int) {
		val backgroundColor = BeatPrompter.preferences.backgroundColor
		val outline =
			if ((Color.red(backgroundColor) + Color.green(backgroundColor) + Color.blue(backgroundColor)) / 3 > 127) Color.BLACK else Color.WHITE

		paint.apply {
			BeatPrompter.fontManager.setTextSize(this, screenString.fontSize)
			flags = Paint.ANTI_ALIAS_FLAG
			color = outline
		}
		canvas.drawRect(popupRect, paint)
		paint.color = backgroundColor
		canvas.drawRect(
			popupRect.inflate(-1),
			paint
		)
		paint.color = textColor
		paint.alpha = 255
		canvas.drawText(text, textDrawLocation.x, textDrawLocation.y, paint)
	}
}