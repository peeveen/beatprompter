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
import com.stevenfrew.beatprompter.PlaylistNode
import com.stevenfrew.beatprompter.R

class SongListAdapter(private val values: List<PlaylistNode>) : ArrayAdapter<PlaylistNode>(BeatPrompterApplication.context, -1, values) {
    private val mLargePrint: Boolean
    private val sharedPref: SharedPreferences = BeatPrompterApplication.preferences

    init {
        mLargePrint = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_defaultValue)))
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
        val showBeatIcons = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_defaultValue)))
        val showKey = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_defaultValue)))
        val showMusicIcon = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_defaultValue)))
        if (song.mAudioFiles.size == 0 || !showMusicIcon) {
            notesIcon.visibility = View.GONE
            //            RelativeLayout.LayoutParams docIconLayoutParams=(RelativeLayout.LayoutParams)docIcon.getLayoutParams();
            //            docIconLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            //            docIcon.setLayoutParams(docIconLayoutParams);
        } else
            notesIcon.visibility = View.VISIBLE
        if (!song.isSmoothScrollable || !showBeatIcons) {
            docIcon.visibility = View.GONE
            //            RelativeLayout.LayoutParams beatIconLayoutParams=(RelativeLayout.LayoutParams)beatIcon.getLayoutParams();
            //            beatIconLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            //            beatIcon.setLayoutParams(beatIconLayoutParams);
        } else
            docIcon.visibility = View.VISIBLE
        if (!song.isBeatScrollable || !showBeatIcons) {
            beatIcon.visibility = View.GONE
        } else
            beatIcon.visibility = View.VISIBLE
        titleView.text = song.mTitle
        var artist = song.mArtist
        if (showKey) {
            val key = song.mKey
            if (key.isNotBlank())
                artist += " - $key"
        }
        artistView.text = artist
        return rowView
    }
}