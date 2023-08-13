package com.stevenfrew.beatprompter.song.load

import android.graphics.Rect

data class SongChoiceInfo(
	val mNormalizedTitle: String,
	val mNormalizedArtist: String,
	val mTrack: String,
	val mOrientation: Int,
	val mBeatScroll: Boolean,
	val mSmoothScroll: Boolean,
	val mMinFontSize: Float,
	val mMaxFontSize: Float,
	val mScreenSize: Rect,
	val mNoAudio: Boolean
)