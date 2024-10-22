package com.stevenfrew.beatprompter.graphics.bitmaps

class AndroidBitmap(internal val androidBitmap: android.graphics.Bitmap) : Bitmap {
	override val isRecycled: Boolean
		get() = androidBitmap.isRecycled

	override fun recycle() = androidBitmap.recycle()
	override fun toCanvas(): BitmapCanvas = AndroidBitmapCanvas(androidBitmap)
	override val width: Int = androidBitmap.width
	override val height: Int = androidBitmap.height
}