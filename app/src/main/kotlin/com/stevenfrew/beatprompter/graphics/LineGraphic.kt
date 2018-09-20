package com.stevenfrew.beatprompter.graphics

import android.graphics.Bitmap
import android.graphics.Rect
import com.stevenfrew.beatprompter.song.line.Line

class LineGraphic(private val mSize: Rect) {
    var mLastDrawnLine: Line? = null
    private var mBitmap: Bitmap = createBitmap()
    var mNextGraphic: LineGraphic =this

    val bitmap:Bitmap
    get(){
        if(mBitmap.isRecycled)
            mBitmap=createBitmap()
        return mBitmap
    }

    private fun createBitmap():Bitmap
    {
        return Bitmap.createBitmap(mSize.width(), mSize.height(), Bitmap.Config.ARGB_8888)
    }

    fun recycle() {
        mBitmap.recycle()
    }
}