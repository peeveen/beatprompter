package com.stevenfrew.beatprompter.util

import android.content.res.Resources
import androidx.core.graphics.toColorInt
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.graphics.bitmaps.AndroidBitmapFactory
import com.stevenfrew.beatprompter.graphics.fonts.AndroidFontManager
import com.stevenfrew.beatprompter.graphics.fonts.FontManager

class AndroidUtils(resources: Resources) : PlatformUtils {
	override val fontManager: FontManager
	override val bitmapFactory = AndroidBitmapFactory

	override fun parseColor(colorString: String): Int = colorString.toColorInt()

	init {
		val minimumFontSize = resources.getString(R.string.fontSizeMin).toFloat()
		val maximumFontSize = resources.getString(R.string.fontSizeMax).toFloat()
		fontManager =
			AndroidFontManager(minimumFontSize, maximumFontSize, resources.displayMetrics.density)
	}
}