package com.stevenfrew.beatprompter.comm

/*
Enum used to flag sender/receivers are having a specific purpose,
so that we can stop groups of them en masse whenever device functionality
changes.
 */
enum class CommunicationType {
	Bluetooth, // Band Bluetooth
	Midi, // Native MIDI
	UsbMidi, // USB MIDI
	BluetoothMidi; // Bluetooth MIDI

	override fun toString(): String =
		when (this) {
			Bluetooth -> "Bluetooth"
			Midi -> "MIDI"
			UsbMidi -> "USB-MIDI"
			BluetoothMidi -> "Bluetooth MIDI"
		}
}
