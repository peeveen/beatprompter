package com.stevenfrew.beatprompter

import android.graphics.Bitmap
import android.graphics.Rect

internal class LineGraphic(size: Rect) {
    var mLastDrawnLine: Line? = null
    var mBitmap: Bitmap = Bitmap.createBitmap(size.width(), size.height(), Bitmap.Config.ARGB_8888)
    var mNextGraphic: LineGraphic? = null
    var mPrevGraphic: LineGraphic? = null

    fun recycle() {
        mBitmap.recycle()
    }
}