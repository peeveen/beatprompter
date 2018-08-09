package com.stevenfrew.beatprompter

import android.graphics.Bitmap
import android.graphics.Rect

internal class LineGraphic(size: Rect) {
    @JvmField var mLastDrawnLine: Line? = null
    @JvmField var mBitmap: Bitmap? = null
    @JvmField var mNextGraphic: LineGraphic? = null
    @JvmField var mPrevGraphic: LineGraphic? = null

    init {
        mBitmap = Bitmap.createBitmap(size.width(), size.height(), Bitmap.Config.ARGB_8888)
        mLastDrawnLine = null
    }

    fun recycle() {
        if (mBitmap != null)
            mBitmap!!.recycle()
    }
}