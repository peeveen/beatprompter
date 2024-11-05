package com.stevenfrew.beatprompter.graphics

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
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
		BeatPrompter.platformUtils.bitmapFactory.createBitmap(
			size.width,
			size.height
		)

	fun recycle() {
		if (!_bitmap.isRecycled)
			_bitmap.recycle()
	}
}