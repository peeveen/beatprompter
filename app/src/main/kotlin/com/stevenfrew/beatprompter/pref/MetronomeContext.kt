package com.stevenfrew.beatprompter.pref

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

enum class MetronomeContext {
    On, OnWhenNoTrack, Off, DuringCountIn;

    companion object {
        internal fun getMetronomeContextPreference(sharedPrefs:SharedPreferences): MetronomeContext
        {
            return try {
                MetronomeContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_metronome_key), BeatPrompterApplication.getResourceString(R.string.pref_metronome_defaultValue))!!)
            } catch (e: Exception) {
                // backward compatibility with old shite values.
                Off
            }
        }
    }
}