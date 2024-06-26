package com.stevenfrew.beatprompter.graphics

import android.graphics.Rect
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo

class DisplaySettings internal constructor(
	val mOrientation: Int,
	val mMinFontSize: Float,
	val mMaxFontSize: Float,
	val mScreenSize: Rect,
	val mShowBeatCounter: Boolean
) {
	internal constructor(choiceInfo: SongChoiceInfo)
		: this(
		choiceInfo.mOrientation,
		choiceInfo.mMinFontSize,
		choiceInfo.mMaxFontSize,
		choiceInfo.mScreenSize,
		choiceInfo.mBeatScroll || choiceInfo.mSmoothScroll
	)

	// Top 5% of screen is used for beat counter
	private val mBeatCounterHeight =
		if (mShowBeatCounter)
			(mScreenSize.height() / 20.0).toInt()
		else
			0
	val mBeatCounterRect = Rect(0, 0, mScreenSize.width(), mBeatCounterHeight)
	val mUsableScreenHeight = mScreenSize.height() - mBeatCounterRect.height()
}