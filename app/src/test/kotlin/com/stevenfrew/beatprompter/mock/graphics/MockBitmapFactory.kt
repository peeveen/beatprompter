package com.stevenfrew.beatprompter.mock.graphics

import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapFactory

class MockBitmapFactory : BitmapFactory {
	override fun createBitmap(width: Int, height: Int): Bitmap = MockBitmap()
	override fun createBitmap(path: String): Bitmap = MockBitmap()
}