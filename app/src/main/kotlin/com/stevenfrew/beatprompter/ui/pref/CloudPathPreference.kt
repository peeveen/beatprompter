package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

class CloudPathPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    public override fun onBindView(view: View) {
        super.onBindView(view)
        val imageView = view.findViewById<ImageView>(R.id.iconImageView)
        imageView.setImageResource(R.drawable.blank_icon)
        val textView = view.findViewById<TextView>(android.R.id.summary)
        val prefs = BeatPrompterApplication.preferences
        val path = prefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_cloudPath_key), null)
        var displayPath = prefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_cloudDisplayPath_key), null)
        if (path == null)
            displayPath = BeatPrompterApplication.getResourceString(R.string.no_cloud_folder_currently_set)
        textView.text = displayPath
    }
}