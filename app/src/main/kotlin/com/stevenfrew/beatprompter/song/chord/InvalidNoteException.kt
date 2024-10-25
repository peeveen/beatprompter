package com.stevenfrew.beatprompter.song.chord

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

/**
 * Note parsing exception.
 */
internal class InvalidNoteException(note: String) :
	Exception(
		BeatPrompter.appResources.getString(
			R.string.failedToParseNote,
			note
		)
	)