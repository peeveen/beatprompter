package com.stevenfrew.beatprompter.ui.pref

import com.stevenfrew.beatprompter.BeatPrompterPreferences

enum class MetronomeContext {
    On, OnWhenNoTrack, Off, DuringCountIn;

    companion object {
        internal fun getMetronomeContextPreference(): MetronomeContext {
            return try {
                MetronomeContext.valueOf(BeatPrompterPreferences.metronomeContext)
            } catch (e: Exception) {
                // backward compatibility with old shite values.
                Off
            }
        }
    }
}
