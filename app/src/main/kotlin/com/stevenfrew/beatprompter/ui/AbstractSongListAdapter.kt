package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.song.SongInfoProvider

abstract class AbstractSongListAdapter<T : SongInfoProvider>(
	private val values: List<T>,
	context: Context
) : ArrayAdapter<T>(context, -1, values) {
	private val layoutId =
		if (BeatPrompter.preferences.largePrint)
			R.layout.song_list_item_large
		else
			R.layout.song_list_item
	abstract val showBeatIcons: Boolean
	private val showKey = BeatPrompter.preferences.showKeyInSongList
	abstract val showRating: Boolean
	abstract val showYear: Boolean
	abstract val showVotes: Boolean
	abstract val songIconDisplayPosition: SongIconDisplayPosition
	abstract val showMusicIcon: Boolean
	abstract fun getIconBitmap(icon: String): Bitmap?

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
			val song = values[position].songInfo
			val iconShown = song.icon?.let { icon ->
				val image = getIconBitmap(icon)
				songIconDisplayed?.setImageBitmap(image)
				songIconDisplayed != null && it != null
			} ?: false
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
			val votes = song.votes
			val year = song.year
			val keyString = if (showKey && !key.isNullOrBlank()) " - $key" else ""
			val votesString = if (showVotes) " (${votes})" else ""
			val ratingString = if (showRating && rating != 0) " - ${STARS[rating]}${votesString}" else ""
			val yearString = if (showYear && year != null) " - $year" else ""
			val artist = song.artist + yearString + keyString + ratingString
			artistView.text = artist
		}

	companion object {
		val STARS = arrayOf("", "★", "★★", "★★★", "★★★★", "⭐⭐⭐⭐⭐")
	}
}