package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class UltimateGuitarFilter :
	Filter(BeatPrompter.appResources.getString(R.string.ultimate_guitar), false) {
	override fun equals(other: Any?): Boolean = other != null && other is UltimateGuitarFilter
	override fun hashCode(): Int = javaClass.hashCode()
}