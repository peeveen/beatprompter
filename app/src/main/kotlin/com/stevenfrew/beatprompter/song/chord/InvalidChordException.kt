package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import kotlin.Exception

/**
 * Special exception for song parsing errors.
 */
internal class InvalidChordException(val chord: String) :
	Exception(
		BeatPrompter.appResources.getString(
			R.string.failedToParseChord,
			chord
		)
	)