package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.set.PlaylistNode

class SongListAdapter(private val values: List<PlaylistNode>, context: Context) :
	ArrayAdapter<PlaylistNode>(context, -1, values) {
	private val layoutId =
		if (Preferences.largePrint)
			R.layout.song_list_item_large
		else
			R.layout.song_list_item
	private val showBeatIcons = Preferences.showBeatStyleIcons
	private val showKey = Preferences.showKeyInSongList
	private val showRating = Preferences.showRatingInSongList
	private val showMusicIcon = Preferences.showMusicIcon
	private val inflater = context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
		(convertView ?: inflater.inflate(layoutId, parent, false)).also {
			val artistView = it.findViewById<TextView>(R.id.songartist)
			val titleView = it.findViewById<TextView>(R.id.songtitle)
			val beatIcon = it.findViewById<ImageView>(R.id.beaticon)
			val docIcon = it.findViewById<ImageView>(R.id.smoothicon)
			val notesIcon = it.findViewById<ImageView>(R.id.musicicon)
			val song = values[position].songFile
			notesIcon.visibility =
				if (song.audioFiles.isEmpty() || !showMusicIcon) View.GONE else View.VISIBLE
			docIcon.visibility =
				if (!song.isSmoothScrollable || !showBeatIcons) View.GONE else View.VISIBLE
			beatIcon.visibility =
				if (!song.isBeatScrollable || !showBeatIcons) View.GONE else View.VISIBLE
			titleView.text = song.title
			val key = song.key
			val rating = song.rating
			val keyString = if (showKey && key.isNotBlank()) " - $key" else ""
			val ratingString = if (showRating && rating != 0) " - ${STARS[rating]}" else ""
			val artist = song.artist + keyString + ratingString
			artistView.text = artist
		}

	companion object {
		val STARS = arrayOf("", "★", "★★", "★★★", "★★★★", "⭐⭐⭐⭐⭐")
	}
}