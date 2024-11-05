package com.stevenfrew.beatprompter.mock

import android.content.res.AssetManager
import com.stevenfrew.beatprompter.util.GlobalAppResources

class MockGlobalAppResources : GlobalAppResources {
	override fun getString(resID: Int): String = "$resID"
	override fun getString(resID: Int, vararg args: Any): String =
		"$resID${getResourceStringArgs(*args)}"

	private fun getResourceStringArgs(vararg args: Any): String =
		if (args.isEmpty()) "" else " ${args.joinToString(" ")}"

	override fun getStringSet(resID: Int): Set<String> = setOf("$resID")
	override val assetManager: AssetManager
		get() = TODO("Not yet implemented")
}