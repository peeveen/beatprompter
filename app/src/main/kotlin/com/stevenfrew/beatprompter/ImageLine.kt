package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.ImageFile

class ImageLine internal constructor(mImageFile:ImageFile, scalingMode:ImageScalingMode, lineTime:Long, lineDuration:Long, beatInfo:LineBeatInfo, displaySettings:SongDisplaySettings, pixelPosition:Int, startScrollTime:Long, stopScrollTime:Long) : Line(lineTime,lineDuration,beatInfo,pixelPosition,startScrollTime,stopScrollTime) {
    private val mBitmap:Bitmap=BitmapFactory.decodeFile(mImageFile.mFile.absolutePath, BitmapFactory.Options())
    private val mSourceRect: Rect = Rect(0,0,mImageFile.mWidth,mImageFile.mHeight)
    private val mDestinationRect=getDestinationRect(mBitmap,displaySettings.mScreenSize,scalingMode)
    override val mMeasurements:LineMeasurements

    init
    {
        mMeasurements=LineMeasurements(1, mDestinationRect.width(), mDestinationRect.height(), intArrayOf(mDestinationRect.height()), lineTime,lineDuration, startScrollTime, beatInfo.mScrollMode)
    }

    override fun renderGraphics()  {
        for (f in 0 until mMeasurements.mLines) {
            val graphic = mGraphics[f]
            if (graphic.mLastDrawnLine !== this) {
                val paint = Paint()
                val c = Canvas(graphic.mBitmap)
                c.drawBitmap(mBitmap, mSourceRect, mDestinationRect, paint)
                graphic.mLastDrawnLine = this
            }
        }
    }

    override fun recycleGraphics() {
        mBitmap.recycle()
        super.recycleGraphics()
    }

    companion object {
        private fun getDestinationRect(bitmap:Bitmap,screenSize:Rect,scalingMode:ImageScalingMode):Rect
        {
            val imageHeight = bitmap.height
            val imageWidth = bitmap.width
            var scaledImageHeight = imageHeight
            var scaledImageWidth = imageWidth

            if (imageWidth > screenSize.width() || scalingMode === ImageScalingMode.Stretch) {
                scaledImageHeight = (imageHeight * (screenSize.width().toDouble() / imageWidth.toDouble())).toInt()
                scaledImageWidth = screenSize.width()
            }

            return Rect(0, 0, scaledImageWidth, scaledImageHeight)
        }
    }
}