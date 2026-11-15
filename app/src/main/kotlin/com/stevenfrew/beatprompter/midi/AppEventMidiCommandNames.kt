package com.stevenfrew.beatprompter.midi

/**
 * Some app events will trigger specially-named MIDI commands.
 */
enum class AppEventMidiCommandNames {
	/**
	 * Triggered when the song list is first shown, or returned to after a song ends (or is quit).
	 */
	BeatPrompterSongList,

	/**
	 * Triggered when the title screen of any song is shown.
	 */
	BeatPrompterTitleScreen
}