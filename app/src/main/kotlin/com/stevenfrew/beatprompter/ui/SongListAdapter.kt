package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.graphics.bitmaps.AndroidBitmap
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.set.PlaylistNode

class SongListAdapter(
	private val values: List<PlaylistNode>,
	private val imageDictionary: Map<String, Bitmap>,
	private val missingIconBitmap: android.graphics.Bitmap,
	context: Context
) : ArrayAdapter<PlaylistNode>(context, -1, values) {
	private val layoutId =
		if (BeatPrompter.preferences.largePrint)
			R.layout.song_list_item_large
		else
			R.layout.song_list_item
	private val showBeatIcons = BeatPrompter.preferences.showBeatStyleIcons
	private val showKey = BeatPrompter.preferences.showKeyInSongList
	private val showRating = BeatPrompter.preferences.showRatingInSongList
	private val showYear = BeatPrompter.preferences.showYearInSongList
	private val songIconDisplayPosition = BeatPrompter.preferences.songIconDisplayPosition
	private val showMusicIcon = BeatPrompter.preferences.showMusicIcon
	private val inflater = context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
		(convertView ?: inflater.inflate(layoutId, parent, false)).also {
			val artistView = it.findViewById<TextView>(R.id.songartist)
			val titleView = it.findViewById<TextView>(R.id.songtitle)
			val beatIcon = it.findViewById<ImageView>(R.id.beaticon)
			val docIcon = it.findViewById<ImageView>(R.id.smoothicon)
			val notesIcon = it.findViewById<ImageView>(R.id.musicicon)
			val songIconLeft = it.findViewById<ImageView>(R.id.songIconLeft)
			val songIconSectionLeft = it.findViewById<ImageView>(R.id.songIconSectionLeft)
			val songIconSectionRight = it.findViewById<ImageView>(R.id.songIconSectionRight)
			val songIconDisplayed = when (songIconDisplayPosition) {
				SongIconDisplayPosition.Left -> songIconLeft
				SongIconDisplayPosition.IconSectionLeft -> songIconSectionLeft
				SongIconDisplayPosition.IconSectionRight -> songIconSectionRight
				else -> null
			}
			val song = values[position].songFile
			val iconShown = song.icon.let {
				val image = (imageDictionary[it] as AndroidBitmap?)?.androidBitmap
					?: missingIconBitmap
				songIconDisplayed?.setImageBitmap(image)
				songIconDisplayed != null && it != null
			}
			notesIcon.visibility =
				if (song.audioFiles.values.flatten()
						.isEmpty() || !showMusicIcon
				) View.GONE else View.VISIBLE
			docIcon.visibility =
				if (!song.isSmoothScrollable || !showBeatIcons) View.GONE else View.VISIBLE
			beatIcon.visibility =
				if (!song.isBeatScrollable || !showBeatIcons) View.GONE else View.VISIBLE
			songIconLeft.visibility =
				if (songIconDisplayPosition == SongIconDisplayPosition.Left && iconShown) View.VISIBLE else View.GONE
			songIconSectionLeft.visibility =
				if (songIconDisplayPosition == SongIconDisplayPosition.IconSectionLeft && iconShown) View.VISIBLE else View.GONE
			songIconSectionRight.visibility =
				if (songIconDisplayPosition == SongIconDisplayPosition.IconSectionRight && iconShown) View.VISIBLE else View.GONE
			titleView.text = song.title
			val key = song.keySignature
			val rating = song.rating
			val year = song.year
			val keyString = if (showKey && !key.isNullOrBlank()) " - $key" else ""
			val ratingString = if (showRating && rating != 0) " - ${STARS[rating]}" else ""
			val yearString = if (showYear && year != null) " - $year" else ""
			val artist = song.artist + yearString + keyString + ratingString
			artistView.text = artist
		}

	companion object {
		val STARS = arrayOf("", "★", "★★", "★★★", "★★★★", "⭐⭐⭐⭐⭐")
	}
}