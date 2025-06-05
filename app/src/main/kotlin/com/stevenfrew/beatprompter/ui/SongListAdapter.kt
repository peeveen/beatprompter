package com.stevenfrew.beatprompter.ui

import android.content.Context
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.util.Utils

class SongListAdapter(
	values: List<PlaylistNode>,
	private val imageDictionary: Map<String, Bitmap>,
	private val missingIconBitmap: android.graphics.Bitmap,
	context: Context
) : AbstractSongListAdapter<PlaylistNode>(values, context) {
	override val showBeatIcons = BeatPrompter.preferences.showBeatStyleIcons
	override val showRating = BeatPrompter.preferences.showRatingInSongList
	override val showYear = BeatPrompter.preferences.showYearInSongList
	override val showVotes = false
	override val songIconDisplayPosition = BeatPrompter.preferences.songIconDisplayPosition
	override val showMusicIcon = BeatPrompter.preferences.showMusicIcon

	override fun getIconBitmap(
		icon: String?,
		tags: Set<String>,
	) = Utils.getIconBitmap(icon, tags, imageDictionary, missingIconBitmap)
}