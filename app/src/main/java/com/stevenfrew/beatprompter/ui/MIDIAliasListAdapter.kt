package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.MIDIAliasFile

class MIDIAliasListAdapter(private val values: List<MIDIAliasFile>) : ArrayAdapter<MIDIAliasFile>(BeatPrompterApplication.context, -1, values) {
    private val mLargePrint: Boolean

    init {
        val sharedPref = BeatPrompterApplication.preferences
        mLargePrint = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_defaultValue)))
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompterApplication.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView
                ?: inflater.inflate(if (mLargePrint) R.layout.midi_alias_list_item_large else R.layout.midi_alias_list_item, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.alias_file_name)
        val errorIcon = rowView.findViewById<ImageView>(R.id.erroricon)
        val maf = values[position]
        if (maf.mErrors.size == 0)
            errorIcon.visibility = View.GONE
        titleView.text = maf.mAliasSet.name
        return rowView
    }
}