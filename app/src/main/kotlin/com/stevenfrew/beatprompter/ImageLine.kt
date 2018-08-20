package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.songload.CancelEvent

class ImageLine internal constructor(mImageFile:ImageFile, private val mScalingMode:ImageScalingMode,lineTime:Long,lineDuration:Long,scrollMode:ScrollingMode,displaySettings:SongDisplaySettings,currentHighlightColor:Int?,pixelPosition:Int) : Line(lineTime,lineDuration,scrollMode,displaySettings,pixelPosition) {
    private val mSourceRect: Rect = Rect(0,0,mImageFile.mWidth,mImageFile.mHeight)
    private val mBitmap:Bitmap=BitmapFactory.decodeFile(mImageFile.mFile.absolutePath, BitmapFactory.Options())
    override val mMeasurements:LineMeasurements

    init
    {
        val destRect=getDestinationRect(displaySettings.mScreenSize)
        mMeasurements=LineMeasurements(1, destRect.width(), destRect.height(), intArrayOf(destRect.height()), currentHighlightColor, lineTime,lineDuration, mNextLine, mYStartScrollTime, scrollMode,displaySettings.mScreenSize)
    }

    override fun renderGraphics(allocate: Boolean)  {
        val destRect=getDestinationRect(mMeasurements.mScreenSize)
        for (f in 0 until mMeasurements.mLines) {
            val graphic = mGraphics[f]
            if (graphic.mLastDrawnLine !== this && allocate) {
                val paint = Paint()
                val c = Canvas(graphic.mBitmap)
                c.drawBitmap(mBitmap, mSourceRect, destRect, paint)
                graphic.mLastDrawnLine = this
            }
        }
    }

    override fun recycleGraphics() {
        mBitmap.recycle()
    }

    private fun getDestinationRect(screenSize:Rect):Rect
    {
        val imageHeight = mBitmap.height
        val imageWidth = mBitmap.width
        var scaledImageHeight = imageHeight
        var scaledImageWidth = imageWidth

        if (imageWidth > screenSize.width() || mScalingMode === ImageScalingMode.Stretch) {
            scaledImageHeight = (imageHeight * (screenSize.width().toDouble() / imageWidth.toDouble())).toInt()
            scaledImageWidth = screenSize.width()
        }

        return Rect(0, 0, scaledImageWidth, scaledImageHeight)
    }
}