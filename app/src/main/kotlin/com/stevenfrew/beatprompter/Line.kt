package com.stevenfrew.beatprompter

abstract class Line internal constructor(val mLineTime:Long, val mLineDuration:Long, val mBeatInfo:LineBeatInfo, val mSongPixelPosition:Int, val mYStartScrollTime:Long, val mYStopScrollTime:Long) {
    internal var mPrevLine: Line? = null
    internal var mNextLine: Line? = null
    abstract val mMeasurements:LineMeasurements
    internal var mManualScrollPositions:ManualScrollPositions?=null
    protected var mGraphics = mutableListOf<LineGraphic>() // pointer to the allocated graphic, if one exists

    internal abstract fun renderGraphics()

    internal fun getTimeFromPixel(pixelPosition: Int): Long {
        if (pixelPosition == 0)
            return 0
        if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mMeasurements.mPixelsToTimes.size)
            return mMeasurements.mPixelsToTimes[pixelPosition - mSongPixelPosition]
        if (pixelPosition < mSongPixelPosition && mPrevLine != null)
            return mPrevLine!!.getTimeFromPixel(pixelPosition)
        if (pixelPosition >= mSongPixelPosition + mMeasurements.mPixelsToTimes.size && mNextLine != null)
            return mNextLine!!.getTimeFromPixel(pixelPosition)
        return mMeasurements.mPixelsToTimes[mMeasurements.mPixelsToTimes.size - 1]
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        val lineEndTime = if (mNextLine == null) Long.MAX_VALUE else mNextLine!!.mLineTime
        if (time in mLineTime..(lineEndTime - 1))
            return calculatePixelFromTime(time)
        if (time < mLineTime && mPrevLine != null)
            return mPrevLine!!.getPixelFromTime(time)
        if (time >= lineEndTime && mNextLine != null)
            return mNextLine!!.getPixelFromTime(time)
        return mSongPixelPosition + mMeasurements.mPixelsToTimes.size
    }

    private fun calculatePixelFromTime(time: Long): Int {
        return mSongPixelPosition+mMeasurements.findClosestEarliestPixel(time)
    }

    internal fun allocateGraphic(graphic: LineGraphic) {
        mGraphics.add(graphic)
    }

    internal fun getGraphics():List<LineGraphic> {
        renderGraphics()
        return mGraphics
    }

    internal open fun recycleGraphics() {
        mGraphics.forEach{it.recycle()}
    }
}
