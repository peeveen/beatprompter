package com.stevenfrew.beatprompter.util

import android.content.res.AssetManager
import android.content.res.Resources

class ApplicationContextResources(private val resources: Resources) : GlobalAppResources {
	override fun getString(resID: Int): String = resources.getString(resID)
	override fun getString(resID: Int, vararg args: Any): String =
		resources.getString(resID, *args)

	override fun getStringSet(resID: Int): Set<String> = resources.getStringArray(resID).toSet()
	override val assetManager: AssetManager = resources.assets
}