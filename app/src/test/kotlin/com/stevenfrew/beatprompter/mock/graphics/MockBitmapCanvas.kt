package com.stevenfrew.beatprompter.mock.graphics

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapCanvas

class MockBitmapCanvas : BitmapCanvas {
	override fun drawBitmap(bitmap: Bitmap, x: Float, y: Float, paint: Paint) {
		// Do nothing
	}

	override fun drawBitmap(bitmap: Bitmap, srcRect: Rect, destRect: Rect, paint: Paint) {
		// Do nothing
	}

	override fun drawColor(color: Int, mode: PorterDuff.Mode) {
		// Do nothing
	}

	override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
		// Do nothing
	}

	override fun clipRect(left: Int, top: Int, right: Int, bottom: Int) {
		// Do nothing
	}

	override fun drawRect(rect: Rect, paint: Paint) {
		// Do nothing
	}

	override fun save() {
		// Do nothing
	}

	override fun restore() {
		// Do nothing
	}
}