package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

/**
 * Chord parsing exception.
 */
internal class InvalidChordException : Exception {
	constructor(chord: String) : super(getMessage(chord))
	constructor(chord: String, invalidNoteException: InvalidNoteException) : super(
		getMessage(chord), invalidNoteException
	)

	companion object {
		fun getMessage(chord: String): String =
			BeatPrompter.appResources.getString(
				R.string.failedToParseChord,
				chord
			)
	}
}