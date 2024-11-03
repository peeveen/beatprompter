package com.stevenfrew.beatprompter.mock

import com.stevenfrew.beatprompter.graphics.bitmaps.BitmapFactory
import com.stevenfrew.beatprompter.graphics.fonts.FontManager
import com.stevenfrew.beatprompter.mock.graphics.MockBitmapFactory
import com.stevenfrew.beatprompter.mock.graphics.MockFontManager
import com.stevenfrew.beatprompter.util.PlatformUtils

class MockPlatformUtils : PlatformUtils {
	override val bitmapFactory: BitmapFactory = MockBitmapFactory()
	override val fontManager: FontManager = MockFontManager()
	override fun parseColor(colorString: String): Int = 0
}