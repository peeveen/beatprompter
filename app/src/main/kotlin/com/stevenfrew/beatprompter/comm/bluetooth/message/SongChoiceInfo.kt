package com.stevenfrew.beatprompter.comm.bluetooth.message

import android.graphics.Rect

data class SongChoiceInfo(val mNormalizedTitle: String, val mNormalizedArtist:String, val mTrack: String, val mOrientation: Int, val mBeatScroll: Boolean, val mSmoothScroll: Boolean, val mMinFontSize: Float, val mMaxFontSize: Float, val mScreenSize: Rect)