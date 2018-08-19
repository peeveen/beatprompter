package com.stevenfrew.beatprompter

class MeasuredLine constructor(val mLine:Line,val mMeasurements:LineMeasurements,val mSongPixelPosition:Int)
{
    internal fun getTimeFromPixel(pixelPosition: Int): Long {
        if (pixelPosition == 0)
            return 0
        if (pixelPosition >= mSongPixelPosition && pixelPosition < mSongPixelPosition + mMeasurements.mPixelsToTimes.size)
            return mMeasurements.mPixelsToTimes[pixelPosition - mSongPixelPosition]
        else if (pixelPosition < mSongPixelPosition && mPrevLine != null)
            return mPrevLine!!.getTimeFromPixel(pixelPosition)
        else if (pixelPosition >= mSongPixelPosition + mMeasurements.mPixelsToTimes.size && mNextLine != null)
            return mNextLine!!.getTimeFromPixel(pixelPosition)
        return mMeasurements.mPixelsToTimes[mMeasurements.mPixelsToTimes.size - 1]
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        var lineEndTime = Long.MAX_VALUE
        if (mNextLine != null)
            lineEndTime = mNextLine!!.mLineEvent.mEventTime

        if (time >= mLineEvent.mEventTime && time < lineEndTime)
            return calculatePixelFromTime(time)
        else if (time < mLineEvent.mEventTime && mPrevLine != null)
            return mPrevLine!!.getPixelFromTime(time)
        else if (time >= lineEndTime && mNextLine != null)
            return mNextLine!!.getPixelFromTime(time)
        return mSongPixelPosition + mMeasurements.mPixelsToTimes.size
    }

    private fun calculatePixelFromTime(time: Long): Int {
        var last = mSongPixelPosition
        for (n in mMeasurements.mPixelsToTimes) {
            if (n > time)
                return last
            last++
        }
        return last
    }
}