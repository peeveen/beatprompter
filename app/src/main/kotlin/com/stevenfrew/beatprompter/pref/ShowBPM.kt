package com.stevenfrew.beatprompter.pref

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

/**
 * Enumeration for the ShowBPM preference.
 */
enum class ShowBPM {
    Yes,Rounded,No;

    companion object {
        internal fun getShowBPMPreference(sharedPrefs: SharedPreferences): ShowBPM
        {
            return try {
                ShowBPM.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_defaultValue))!!)
            } catch (e: Exception) {
                // backward compatibility with old shite values.
                No
            }
        }
    }

}