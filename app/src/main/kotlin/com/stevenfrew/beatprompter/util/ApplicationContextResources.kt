package com.stevenfrew.beatprompter.util

import android.content.Context
import android.content.res.AssetManager

class ApplicationContextResources(context: Context) : GlobalAppResources {
	private val resources = context.resources

	override fun getString(resID: Int): String = resources.getString(resID)
	override fun getString(resID: Int, vararg args: Any): String =
		resources.getString(resID, *args)

	override fun getStringSet(resID: Int): Set<String> = resources.getStringArray(resID).toSet()
	override val assetManager: AssetManager = resources.assets
}