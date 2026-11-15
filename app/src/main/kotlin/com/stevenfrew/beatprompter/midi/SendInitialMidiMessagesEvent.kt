package com.stevenfrew.beatprompter.midi

/**
 * When should initial MIDI events be sent?
 */
enum class SendInitialMidiMessagesEvent {
	/**
	 * At the title screen.
	 */
	AtTitleScreen,

	/**
	 * After the title screen.
	 */
	AfterTitleScreen
}