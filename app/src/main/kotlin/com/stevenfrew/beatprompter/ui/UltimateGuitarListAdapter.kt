package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.graphics.Bitmap
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.execute
import com.stevenfrew.ultimateguitar.ChordSearcher
import com.stevenfrew.ultimateguitar.TabInfo
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class UltimateGuitarListAdapter(
	searchText: String,
	private val listItems: MutableList<UltimateGuitarListItem>,
	context: Context,
) : AbstractSongListAdapter<UltimateGuitarListItem>(listItems, context) {
	override val showBeatIcons = false
	override val showRating = true
	override val showVotes = true
	override val showYear = false
	override val songIconDisplayPosition = SongIconDisplayPosition.None
	override val showMusicIcon = false

	override fun getIconBitmap(icon: String): Bitmap? = null

	init {
		if (isSearchTextSufficient(searchText))
			TabInfoFetcher().execute(searchText, Dispatchers.Main)
		else {
			listItems.clear()
			listItems.add(NotEnoughSearchTextListItem)
		}
	}

	inner class TabInfoFetcher : CoroutineTask<String, Int, List<TabInfo>> {
		override fun onPreExecute() {
			listItems.clear()
			listItems.add(SearchingListItem)
			notifyDataSetChanged()
		}

		override fun onError(t: Throwable) {
			// TODO
		}

		override fun onProgressUpdate(progress: Int) {
			// Do nothing
		}

		override fun onPostExecute(result: List<TabInfo>) {
			listItems.clear()
			if (result.any())
				listItems.addAll(result.map { UltimateGuitarListItem(it) })
			else
				listItems.add(NoResultsListItem)
			notifyDataSetChanged()
		}

		override fun doInBackground(
			params: String,
			progressUpdater: suspend (Int) -> Unit
		): List<TabInfo> =
			ChordSearcher.search(params)

		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO
	}

	companion object {
		internal const val MINIMUM_SEARCH_TEXT_LENGTH = 3

		private val SearchingListItem = UltimateGuitarListItem(UltimateGuitarSearchStatus.Searching)
		private val NoResultsListItem = UltimateGuitarListItem(UltimateGuitarSearchStatus.NoResults)
		private val NotEnoughSearchTextListItem =
			UltimateGuitarListItem(UltimateGuitarSearchStatus.NotEnoughSearchText)

		private fun isSearchTextSufficient(searchText: String): Boolean =
			searchText.split(' ').any { it.length >= MINIMUM_SEARCH_TEXT_LENGTH }
	}
}