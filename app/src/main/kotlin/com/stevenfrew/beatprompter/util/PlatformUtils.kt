package com.stevenfrew.beatprompter.util

import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapFactory
import com.stevenfrew.beatprompter.graphics.fonts.FontManager

interface PlatformUtils {
	val fontManager: FontManager
	val bitmapFactory: BitmapFactory
	fun parseColor(colorString: String): Int
}