package com.stevenfrew.beatprompter

import android.util.Log

object Logger {
	private const val LOGGING = false
	private const val TAG = "beatprompter"
	private const val TAG_LOAD = "beatprompter_load"
	private const val TAG_COMMS = "beatprompter_comms"

	@Suppress("unused")
	fun logAlways(message: String) = Log.d(TAG, message)

	private fun log(tag: String, message: () -> String, t: Throwable? = null) {
		if (LOGGING)
			log(tag, message(), t)
	}

	private fun log(tag: String, message: String, t: Throwable? = null) {
		if (LOGGING)
			if (t == null)
				Log.d(tag, message)
			else
				Log.e(tag, message, t)
	}

	fun log(message: () -> String, t: Throwable? = null) = log(TAG, message, t)
	fun log(message: () -> String) = log(message, null)
	fun log(message: String, t: Throwable? = null) = log(TAG, message, t)
	fun log(t: Throwable) = log(TAG, { t.message ?: "" }, t)

	private fun logLoader(
		message: () -> String,
		@Suppress("SameParameterValue") t: Throwable? = null
	) = log(TAG_LOAD, message, t)

	fun logLoader(message: () -> String) = logLoader(message, null)
	fun logLoader(message: String, t: Throwable? = null) = log(TAG_LOAD, message, t)

	fun logComms(message: () -> String, t: Throwable? = null) = log(TAG_COMMS, message, t)
	fun logComms(message: () -> String) = logComms(message, null)
	fun logComms(message: String, t: Throwable? = null) = log(TAG_COMMS, message, t)
}