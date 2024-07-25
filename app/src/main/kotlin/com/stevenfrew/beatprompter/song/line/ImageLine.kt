package com.stevenfrew.beatprompter.song.line

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.SongParserException
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.ImageScalingMode
import com.stevenfrew.beatprompter.song.ScrollingMode

class ImageLine internal constructor(
	mImageFile: ImageFile,
	scalingMode: ImageScalingMode,
	lineTime: Long,
	lineDuration: Long,
	scrollMode: ScrollingMode,
	displaySettings: DisplaySettings,
	pixelPosition: Int,
	inChorusSection: Boolean,
	scrollTimes: Pair<Long, Long>
) : Line(
	lineTime,
	lineDuration,
	scrollMode,
	pixelPosition,
	inChorusSection,
	scrollTimes.first,
	scrollTimes.second,
	displaySettings
) {
	private val bitmap =
		BitmapFactory.decodeFile(mImageFile.file.absolutePath, BitmapFactory.Options())
	private val sourceRect = Rect(0, 0, mImageFile.size.width, mImageFile.size.height)
	private val destinationRect = getDestinationRect(
		bitmap,
		displaySettings.screenSize,
		scalingMode
	)
	override val measurements = LineMeasurements(
		1,
		destinationRect.width(),
		destinationRect.height(),
		intArrayOf(destinationRect.height()),
		lineTime,
		lineDuration,
		scrollTimes.first,
		scrollMode
	)

	override fun renderGraphics(paint: Paint) =
		repeat(measurements.lines) {
			val graphic = graphics[it]
			val canvas = canvasses[it]
			if (graphic.lastDrawnLine !== this) {
				canvas.drawBitmap(bitmap, sourceRect, destinationRect, paint)
				graphic.lastDrawnLine = this
			}
		}

	override fun recycleGraphics() {
		bitmap.recycle()
		super.recycleGraphics()
	}

	companion object {
		private fun getDestinationRect(
			bitmap: Bitmap,
			screenSize: Rect,
			scalingMode: ImageScalingMode
		): Rect {
			val imageHeight = bitmap.height
			val imageWidth = bitmap.width

			val needsStretched =
				imageWidth > screenSize.width() || scalingMode === ImageScalingMode.Stretch
			val scaledImageHeight =
				if (needsStretched)
					(imageHeight * (screenSize.width().toDouble() / imageWidth.toDouble())).toInt()
				else
					imageHeight
			val scaledImageWidth =
				if (needsStretched)
					screenSize.width()
				else
					imageWidth

			if (scaledImageHeight > 8192 || scaledImageWidth > 8192)
				throw SongParserException(BeatPrompter.appResources.getString(R.string.image_too_large))

			return Rect(0, 0, scaledImageWidth, scaledImageHeight)
		}
	}
}