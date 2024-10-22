package com.stevenfrew.beatprompter.graphics.bitmaps

interface BitmapFactory {
	fun createBitmap(width: Int, height: Int): Bitmap
	fun createBitmap(path: String): Bitmap
}