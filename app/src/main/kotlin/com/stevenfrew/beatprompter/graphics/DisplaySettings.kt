package com.stevenfrew.beatprompter.graphics

import com.stevenfrew.beatprompter.song.load.SongChoiceInfo

class DisplaySettings internal constructor(
	val orientation: Int,
	val minimumFontSize: Float,
	val maximumFontSize: Float,
	val screenSize: Rect,
	val showBeatCounter: Boolean = true
) {
	internal constructor(choiceInfo: SongChoiceInfo)
		: this(
		choiceInfo.orientation,
		choiceInfo.minFontSize,
		choiceInfo.maxFontSize,
		Rect(choiceInfo.screenSize),
		choiceInfo.isBeatScroll || choiceInfo.isSmoothScroll
	)

	// Top 5% of screen is used for beat counter
	private val beatCounterHeight =
		if (showBeatCounter)
			(screenSize.height / 20.0).toInt()
		else
			0

	val beatCounterRect = Rect(0, 0, screenSize.width, beatCounterHeight)
	val usableScreenHeight = screenSize.height - beatCounterRect.height
}