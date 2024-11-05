package com.stevenfrew.beatprompter.graphics.fonts

import com.stevenfrew.beatprompter.graphics.Rect

data class TextMeasurement(
	val rect: Rect = Rect(),
	var width: Int = 0,
	var height: Int = 0,
	var descenderOffset: Int = 0
)