package com.stevenfrew.beatprompter.comm

enum class MessageType {
	// MIDI message (Native, USB, or BlueTooth)
	Midi,

	// Band message (always Bluetooth)
	Band
}