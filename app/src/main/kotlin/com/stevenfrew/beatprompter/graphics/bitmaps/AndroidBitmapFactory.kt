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
				android.graphics.BitmapFactory.Options()
			)
		)
}