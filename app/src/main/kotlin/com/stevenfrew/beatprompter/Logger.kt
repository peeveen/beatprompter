package com.stevenfrew.beatprompter

import android.util.Log

object Logger {
	private const val TAG = "beatprompter"
	private const val TAG_LOAD = "beatprompter_load"
	private const val TAG_COMMS = "beatprompter_comms"

	private fun log(tag: String, message: () -> String, warn: Boolean = false, t: Throwable? = null) {
		if (BuildConfig.DEBUG)
			log(tag, message(), warn, t)
	}

	private fun log(tag: String, message: String, warn: Boolean = false, t: Throwable? = null) {
		if (BuildConfig.DEBUG)
			if (t == null)
				if (warn)
					Log.w(tag, message)
				else
					Log.d(tag, message)
			else
				Log.e(tag, message, t)
	}

	fun log(message: () -> String, warn: Boolean = false, t: Throwable? = null) =
		log(TAG, message, warn, t)

	fun log(message: () -> String, warn: Boolean = false) = log(message, warn, null)
	fun log(message: String, warn: Boolean = false, t: Throwable? = null) = log(TAG, message, warn, t)
	fun log(message: String, t: Throwable) = log(TAG, message, false, t)
	fun log(message: () -> String, t: Throwable) = log(TAG, message, false, t)
	fun log(t: Throwable) = log(TAG, { t.message ?: "" }, false, t)

	private fun logLoader(
		message: () -> String,
		warn: Boolean = false,
		@Suppress("SameParameterValue") t: Throwable? = null
	) = log(TAG_LOAD, message, warn, t)

	fun logLoader(message: () -> String, warn: Boolean = false) = logLoader(message, warn, null)
	fun logLoader(message: String, warn: Boolean = false, t: Throwable? = null) =
		log(TAG_LOAD, message, warn, t)

	fun logComms(message: () -> String, warn: Boolean = false, t: Throwable? = null) =
		log(TAG_COMMS, message, warn, t)

	fun logComms(message: () -> String, warn: Boolean = false) = logComms(message, warn, null)
	fun logComms(message: String, warn: Boolean = false, t: Throwable? = null) =
		log(TAG_COMMS, message, warn, t)

	fun logComms(message: String, t: Throwable) =
		log(TAG_COMMS, message, false, t)

	fun logComms(message: () -> String, t: Throwable) =
		log(TAG_COMMS, message, false, t)
}