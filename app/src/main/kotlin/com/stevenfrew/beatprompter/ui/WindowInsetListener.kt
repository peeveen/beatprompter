package com.stevenfrew.beatprompter.ui

import android.os.Build
import android.view.View
import android.view.WindowInsets

class WindowInsetListener : View.OnApplyWindowInsetsListener {
	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsets
	): WindowInsets {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// get the height of the status bar
			val top = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
			val bottom = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
			v.setPadding(0, top, 0, bottom)
		}
		return insets
	}
}