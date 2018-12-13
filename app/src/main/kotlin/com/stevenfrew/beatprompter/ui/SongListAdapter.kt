package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.R

class SongListAdapter(private val values: List<PlaylistNode>)
    : ArrayAdapter<PlaylistNode>(BeatPrompter.context, -1, values) {
    private val mLayoutId =
            if (Preferences.largePrint)
                R.layout.song_list_item_large
            else
                R.layout.song_list_item
    private val mShowBeatIcons = Preferences.showBeatStyleIcons
    private val mShowKey = Preferences.showKeyInSongList
    private val mShowMusicIcon = Preferences.showMusicIcon
    private val mInflater = BeatPrompter.context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView
                ?: mInflater.inflate(mLayoutId, parent, false)).also {
            val artistView = it.findViewById<TextView>(R.id.songartist)
            val titleView = it.findViewById<TextView>(R.id.songtitle)
            val beatIcon = it.findViewById<ImageView>(R.id.beaticon)
            val docIcon = it.findViewById<ImageView>(R.id.smoothicon)
            val notesIcon = it.findViewById<ImageView>(R.id.musicicon)
            val song = values[position].mSongFile
            notesIcon.visibility = if (song.mAudioFiles.isEmpty() || !mShowMusicIcon) View.GONE else View.VISIBLE
            docIcon.visibility = if (!song.isSmoothScrollable || !mShowBeatIcons) View.GONE else View.VISIBLE
            beatIcon.visibility = if (!song.isBeatScrollable || !mShowBeatIcons) View.GONE else View.VISIBLE
            titleView.text = song.mTitle
            val key = song.mKey
            val keyString = if (mShowKey && key.isNotBlank()) " - $key" else ""
            val artist = song.mArtist + keyString
            artistView.text = artist
        }
    }
}