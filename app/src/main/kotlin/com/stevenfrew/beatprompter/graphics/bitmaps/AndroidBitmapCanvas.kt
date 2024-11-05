package com.stevenfrew.beatprompter.graphics.bitmaps

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect

class AndroidBitmapCanvas(bitmap: android.graphics.Bitmap) : BitmapCanvas {
	private val canvas = Canvas(bitmap)
	override fun drawBitmap(bitmap: Bitmap, x: Float, y: Float, paint: Paint) =
		canvas.drawBitmap((bitmap as AndroidBitmap).androidBitmap, x, y, paint)

	override fun drawBitmap(bitmap: Bitmap, srcRect: Rect, destRect: Rect, paint: Paint) =
		canvas.drawBitmap((bitmap as AndroidBitmap).androidBitmap, srcRect, destRect, paint)

	override fun drawColor(color: Int, mode: PorterDuff.Mode) =
		canvas.drawColor(color, mode)

	override fun drawText(text: String, x: Float, y: Float, paint: Paint) =
		canvas.drawText(text, x, y, paint)

	override fun clipRect(left: Int, top: Int, right: Int, bottom: Int) {
		canvas.clipRect(left, top, right, bottom)
	}

	override fun drawRect(rect: Rect, paint: Paint) =
		canvas.drawRect(rect, paint)

	override fun save() {
		canvas.save()
	}

	override fun restore() {
		canvas.restore()
	}
}