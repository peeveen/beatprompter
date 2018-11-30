package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.R

class SongListAdapter(private val values: List<PlaylistNode>) : ArrayAdapter<PlaylistNode>(BeatPrompterApplication.context, -1, values) {
    private val mLargePrint: Boolean
    private val sharedPref: SharedPreferences = BeatPrompterApplication.preferences

    init {
        mLargePrint = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_key), BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_defaultValue).toBoolean())
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompterApplication.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView
                ?: inflater.inflate(if (mLargePrint) R.layout.song_list_item_large else R.layout.song_list_item, parent, false)
        val artistView = rowView.findViewById<TextView>(R.id.songartist)
        val titleView = rowView.findViewById<TextView>(R.id.songtitle)
        val beatIcon = rowView.findViewById<ImageView>(R.id.beaticon)
        val docIcon = rowView.findViewById<ImageView>(R.id.smoothicon)
        val notesIcon = rowView.findViewById<ImageView>(R.id.musicicon)
        val song = values[position].mSongFile
        val showBeatIcons = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_key), BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_defaultValue).toBoolean())
        val showKey = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_key), BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_defaultValue).toBoolean())
        val showMusicIcon = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_key), BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_defaultValue).toBoolean())
        notesIcon.visibility = if (song.mAudioFiles.isEmpty() || !showMusicIcon) View.GONE else View.VISIBLE
        docIcon.visibility = if (!song.isSmoothScrollable || !showBeatIcons) View.GONE else View.VISIBLE
        beatIcon.visibility = if (!song.isBeatScrollable || !showBeatIcons) View.GONE else View.VISIBLE
        titleView.text = song.mTitle
        val key = song.mKey
        val keyString = if (showKey && key.isNotBlank()) " - $key" else ""
        val artist = song.mArtist + keyString
        artistView.text = artist
        return rowView
    }
}