package com.stevenfrew.beatprompter.util

import android.content.SharedPreferences
import android.content.res.AssetManager

interface GlobalAppResources {
	fun getString(resID: Int): String
	fun getString(resID: Int, vararg args: Any): String
	val preferences: SharedPreferences
	val privatePreferences: SharedPreferences
	val assetManager: AssetManager
}

