package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import com.github.peeveen.ultimateguitar.ChordSearcher
import com.github.peeveen.ultimateguitar.TabInfo
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.song.UltimateGuitarSongInfo
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.execute
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class UltimateGuitarListAdapter(
	searchText: String,
	private val handler: Handler,
	private val listItems: MutableList<PlaylistNode>,
	context: Context,
) : AbstractSongListAdapter<PlaylistNode>(listItems, context) {
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
			listItems.add(NotEnoughSearchTextNode)
		}
	}

	inner class TabInfoFetcher : CoroutineTask<String, Int, List<TabInfo>> {
		override fun onPreExecute() {
			listItems.clear()
			listItems.add(SearchingNode)
			notifyDataSetChanged()
		}

		override fun onError(t: Throwable) =
			handler.obtainMessage(Events.SEARCH_ERROR, t.message).sendToTarget()

		override fun onProgressUpdate(progress: Int) {
			// Do nothing
		}

		override fun onPostExecute(result: List<TabInfo>) {
			listItems.clear()
			if (result.any())
				listItems.addAll(result.map { PlaylistNode(UltimateGuitarSongInfo(it)) })
			else
				listItems.add(NoResultsNode)
			notifyDataSetChanged()
		}

		override fun doInBackground(
			params: String,
			progressUpdater: suspend (Int) -> Unit
		): List<TabInfo> {
			val searchResults = ChordSearcher.search(params)
			// Sort by rating, then votes
			val sortedSearchResults = searchResults.sortedWith(object : Comparator<TabInfo> {
				override fun compare(
					p0: TabInfo,
					p1: TabInfo
				): Int {
					val originalIndex0 =
						searchResults.indexOfFirst { it.songName == p0.songName && it.artistName == p0.artistName }
					val originalIndex1 =
						searchResults.indexOfFirst { it.songName == p1.songName && it.artistName == p1.artistName }
					if (originalIndex0 == originalIndex1) {
						val rating0 = p0.rating.roundToInt()
						val rating1 = p1.rating.roundToInt()
						return if (rating0 == rating1)
							p1.votes - p0.votes
						else
							rating1 - rating0
					}
					return originalIndex0 - originalIndex1
				}
			})

			return sortedSearchResults
		}

		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO
	}

	companion object {
		internal const val MINIMUM_SEARCH_TEXT_LENGTH = 3

		private val SearchingNode =
			PlaylistNode(UltimateGuitarSearchStatusNode(UltimateGuitarSearchStatus.Searching))
		private val NoResultsNode =
			PlaylistNode(UltimateGuitarSearchStatusNode(UltimateGuitarSearchStatus.NoResults))
		private val NotEnoughSearchTextNode =
			PlaylistNode(UltimateGuitarSearchStatusNode(UltimateGuitarSearchStatus.NotEnoughSearchText))

		private fun isSearchTextSufficient(searchText: String): Boolean =
			searchText.split(' ').any { it.length >= MINIMUM_SEARCH_TEXT_LENGTH }
	}
}