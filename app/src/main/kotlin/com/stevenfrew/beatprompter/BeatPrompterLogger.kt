package com.stevenfrew.beatprompter

import android.util.Log

object BeatPrompterLogger {
    private const val LOGGING = true
    private const val TAG = "beatprompter"
    private const val TAG_LOAD = "beatprompter_load"
    private const val TAG_COMMS = "beatprompter_comms"

    private fun log(tag: String, message: String, t: Throwable? = null) {
        if (LOGGING)
            if (t == null)
                Log.d(tag, message)
            else
                Log.e(tag, message, t)
    }

    fun log(message: String, t: Throwable? = null) {
        log(TAG, message, t)
    }

    fun log(t: Throwable) {
        log(TAG, t.message ?: "", t)
    }

    fun logLoader(message: String, t: Throwable? = null) {
        log(TAG_LOAD, message, t)
    }

    fun logComms(message: String, t: Throwable? = null) {
        log(TAG_COMMS, message, t)
    }
}