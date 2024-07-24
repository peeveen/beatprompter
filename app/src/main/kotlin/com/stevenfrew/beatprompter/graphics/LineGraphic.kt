package com.stevenfrew.beatprompter.graphics

import android.graphics.Bitmap
import android.graphics.Rect
import com.stevenfrew.beatprompter.song.line.Line

class LineGraphic(private val size: Rect) {
	var lastDrawnLine: Line? = null
	var nextGraphic = this

	private var _bitmap = createBitmap()

	val bitmap: Bitmap
		get() {
			if (_bitmap.isRecycled)
				_bitmap = createBitmap()
			return _bitmap
		}

	private fun createBitmap(): Bitmap =
		Bitmap.createBitmap(
			size.width(),
			size.height(),
			Bitmap.Config.ARGB_8888
		)

	fun recycle() {
		if (!_bitmap.isRecycled)
			_bitmap.recycle()
	}
}