package com.stevenfrew.beatprompter.graphics

import android.graphics.Bitmap
import android.graphics.Rect
import com.stevenfrew.beatprompter.song.line.Line

class LineGraphic(size: Rect) {
    var mLastDrawnLine: Line? = null
    var mBitmap: Bitmap = Bitmap.createBitmap(size.width(), size.height(), Bitmap.Config.ARGB_8888)
    var mNextGraphic: LineGraphic =this

    fun recycle() {
        mBitmap.recycle()
    }
}