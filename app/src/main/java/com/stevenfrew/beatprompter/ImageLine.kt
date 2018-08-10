package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.FileParseError
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.Tag
import com.stevenfrew.beatprompter.event.CancelEvent
import com.stevenfrew.beatprompter.event.ColorEvent
import java.util.ArrayList

class ImageLine internal constructor(private val mImageFile: ImageFile, private val mScalingMode: ImageScalingMode, lineTags: Collection<Tag>, bars: Int, lastColor: ColorEvent, bpb: Int, scrollbeat: Int, scrollbeatOffset: Int, scrollingMode: ScrollingMode, parseErrors: ArrayList<FileParseError>) : Line(lineTags, bars, lastColor, bpb, scrollbeat, scrollbeatOffset, scrollingMode, parseErrors) {
    private var mSourceRect: Rect? = null
    private var mDestRect: Rect? = null
    private var mBitmap: Bitmap? = null

    override fun doMeasurements(paint: Paint, minimumFontSize: Float, maximumFontSize: Float, screenWidth: Int, screenHeight: Int, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: ArrayList<FileParseError>, scrollMode: ScrollingMode, cancelEvent: CancelEvent): LineMeasurements? {
        val path = mImageFile.mFile.absolutePath
        val options = BitmapFactory.Options()
        try {
            mBitmap = BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            errors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mImageFile.mName))
            return null
        }

        val imageHeight = mBitmap!!.height
        val imageWidth = mBitmap!!.width
        var scaledImageHeight = imageHeight
        var scaledImageWidth = imageWidth

        if (imageWidth > screenWidth || mScalingMode === ImageScalingMode.Stretch) {
            scaledImageHeight = (imageHeight * (screenWidth.toDouble() / imageWidth.toDouble())).toInt()
            scaledImageWidth = screenWidth
        }

        val graphicHeights = ArrayList<Int>()
        graphicHeights.add(scaledImageHeight)
        mSourceRect = Rect(0, 0, imageWidth, imageHeight)
        mDestRect = Rect(0, 0, scaledImageWidth, scaledImageHeight)
        return LineMeasurements(1, mDestRect!!.width(), mDestRect!!.height(), graphicHeights, highlightColour, mLineEvent, mNextLine, mYStartScrollTime, scrollMode)
    }

    internal override fun getGraphics(allocate: Boolean): Collection<LineGraphic> {
        for (f in 0 until mLineMeasurements!!.mLines) {
            val graphic = mGraphics[f]
            if (graphic.mLastDrawnLine !== this && allocate) {
                val paint = Paint()
                val c = Canvas(graphic.mBitmap!!)
                c.drawBitmap(mBitmap!!, mSourceRect, mDestRect!!, paint)
                graphic.mLastDrawnLine = this
            }
        }
        return mGraphics
    }

    override fun recycleGraphics() {
        super.recycleGraphics()
        mBitmap!!.recycle()
    }
}