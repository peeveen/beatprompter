package com.stevenfrew.beatprompter.ui

import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.song.SongInfoProvider
import com.stevenfrew.ultimateguitar.TabInfo

class UltimateGuitarListItem : SongInfoProvider, SongInfo {
	val searchStatus: UltimateGuitarSearchStatus
	val tabInfo: TabInfo?

	constructor(searchStatus: UltimateGuitarSearchStatus) {
		this.searchStatus = searchStatus
		this.tabInfo = null
	}

	constructor(tabInfo: TabInfo) {
		this.tabInfo = tabInfo
		this.searchStatus = UltimateGuitarSearchStatus.Complete
	}

	override val songInfo: SongInfo = this
	override val title get() = tabInfo?.songName ?: getSearchStatusText(searchStatus)
	override val artist get() = tabInfo?.artistName ?: getSearchStatusSubtext(searchStatus)
	override val votes get() = tabInfo?.votes ?: 0
	override val icon: String? get() = null
	override val isBeatScrollable = false
	override val isSmoothScrollable = false
	override val rating: Int get() = Math.round(tabInfo?.rating ?: 0.0).toInt()
	override val year: Int? = null
	override val keySignature: String? get() = tabInfo?.key
	override val audioFiles: Map<String, List<String>> = mapOf()

	companion object {
		private fun getSearchStatusText(searchStatus: UltimateGuitarSearchStatus) =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> "No results."
				UltimateGuitarSearchStatus.Searching -> "Searching ..."
				UltimateGuitarSearchStatus.NotEnoughSearchText -> "Not enough search text."
				else -> ""
			}

		private fun getSearchStatusSubtext(searchStatus: UltimateGuitarSearchStatus) =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> ""
				UltimateGuitarSearchStatus.Searching -> ""
				UltimateGuitarSearchStatus.NotEnoughSearchText -> "At least ${UltimateGuitarListAdapter.MINIMUM_SEARCH_TEXT_LENGTH} consecutive characters required."
				else -> ""
			}
	}
}
