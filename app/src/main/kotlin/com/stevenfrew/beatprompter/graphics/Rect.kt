package com.stevenfrew.beatprompter.graphics

data class Rect(
	val left: Int,
	val top: Int,
	val right: Int,
	val bottom: Int
) {
	constructor(rect: android.graphics.Rect) : this(rect.left, rect.top, rect.right, rect.bottom)
	constructor(rect: Rect) : this(rect.left, rect.top, rect.right, rect.bottom)
	constructor() : this(0, 0, 0, 0)

	val height = bottom - top
	val width = right - left
}
