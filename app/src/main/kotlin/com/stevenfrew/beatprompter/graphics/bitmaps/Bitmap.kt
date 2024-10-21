package com.stevenfrew.beatprompter.graphics.bitmaps

interface Bitmap {
	val isRecycled: Boolean
	fun recycle()
	fun toCanvas(): BitmapCanvas
	val width: Int
	val height: Int
}