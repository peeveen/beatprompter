package com.stevenfrew.beatprompter.mock.graphics

import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapCanvas

class MockBitmap : Bitmap {
	override val isRecycled: Boolean = false
	override fun recycle() {
		// Nothing to do.
	}

	override fun toCanvas(): BitmapCanvas = MockBitmapCanvas()

	override val width: Int
		get() = TODO("Not yet implemented")
	override val height: Int
		get() = TODO("Not yet implemented")
}