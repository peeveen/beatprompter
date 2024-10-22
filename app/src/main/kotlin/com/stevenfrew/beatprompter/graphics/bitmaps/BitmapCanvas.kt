package com.stevenfrew.beatprompter.graphics.bitmaps

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect

interface BitmapCanvas {
	fun drawBitmap(bitmap: Bitmap, x: Float, y: Float, paint: Paint)
	fun drawBitmap(bitmap: Bitmap, srcRect: Rect, destRect: Rect, paint: Paint)
	fun drawColor(color: Int, mode: PorterDuff.Mode)
	fun drawText(text: String, x: Float, y: Float, paint: Paint)
	fun clipRect(left: Int, top: Int, right: Int, bottom: Int)
	fun drawRect(rect: Rect, paint: Paint)
	fun save()
	fun restore()
}