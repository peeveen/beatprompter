package com.stevenfrew.beatprompter.graphics.bitmaps

object AndroidBitmapFactory : BitmapFactory {
	override fun createBitmap(width: Int, height: Int): Bitmap =
		AndroidBitmap(
			android.graphics.Bitmap.createBitmap(
				width,
				height,
				android.graphics.Bitmap.Config.ARGB_8888
			)
		)

	override fun createBitmap(path: String): Bitmap =
		AndroidBitmap(
			android.graphics.BitmapFactory.decodeFile(
				path,
				DEFAULT_OPTIONS
			)
		)

	private val DEFAULT_OPTIONS = android.graphics.BitmapFactory.Options()
}