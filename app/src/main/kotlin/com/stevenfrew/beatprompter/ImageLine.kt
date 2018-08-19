package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.songload.CancelEvent

class ImageLine internal constructor(lineTime: Long, lineDuration:Long, beatInfo:BeatInfo,private val mImageFile: ImageFile, private val mScalingMode: ImageScalingMode) : Line(lineTime,lineDuration, beatInfo) {
    private val mSourceRect: Rect = Rect(0,0,mImageFile.mWidth,mImageFile.mHeight)
    private val mBitmap:Bitmap=BitmapFactory.decodeFile(mImageFile.mFile.absolutePath, BitmapFactory.Options())

    override fun doMeasurements(paint: Paint, songDisplaySettings: SongDisplaySettings, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, scrollMode: ScrollingMode, cancelEvent: CancelEvent): LineMeasurements {
        val destRect=getDestinationRect()
        return LineMeasurements(1, destRect.width(), destRect.height(), intArrayOf(destRect.height()), highlightColour, mLineEvent, mNextLine, mYStartScrollTime, scrollMode)
    }

    override fun getGraphics(allocate: Boolean): Collection<LineGraphic> {
        val destRect=getDestinationRect()
        for (f in 0 until mLineMeasurements.mLines) {
            val graphic = mGraphics[f]
            if (graphic.mLastDrawnLine !== this && allocate) {
                val paint = Paint()
                val c = Canvas(graphic.mBitmap)
                c.drawBitmap(mBitmap, mSourceRect, destRect, paint)
                graphic.mLastDrawnLine = this
            }
        }
        return mGraphics
    }

    override fun recycleGraphics() {
        super.recycleGraphics()
        mBitmap.recycle()
    }

    private fun getDestinationRect():Rect
    {
        val imageHeight = mBitmap.height
        val imageWidth = mBitmap.width
        var scaledImageHeight = imageHeight
        var scaledImageWidth = imageWidth

        if (imageWidth > mScreenSize.width() || mScalingMode === ImageScalingMode.Stretch) {
            scaledImageHeight = (imageHeight * (mScreenSize.width().toDouble() / imageWidth.toDouble())).toInt()
            scaledImageWidth = mScreenSize.width()
        }

        return Rect(0, 0, scaledImageWidth, scaledImageHeight)
    }
}