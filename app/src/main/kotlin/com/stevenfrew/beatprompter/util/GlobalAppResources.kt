package com.stevenfrew.beatprompter.util

import android.content.res.AssetManager

interface GlobalAppResources {
	fun getString(resID: Int): String
	fun getString(resID: Int, vararg args: Any): String
	fun getStringSet(resID: Int): Set<String>
	val assetManager: AssetManager
}

