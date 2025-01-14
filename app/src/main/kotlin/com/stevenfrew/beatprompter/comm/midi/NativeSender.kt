package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiInputPort
import com.stevenfrew.beatprompter.comm.CommunicationType

class NativeSender(
	// Annoyingly, if we don't keep a hold of the MIDI device reference for Bluetooth MIDI, then
	// it automatically closes. So I'm storing it here.
	// ⚠️⚠️⚠️⚠️ SO WHATEVER YOU DO, DO NOT REMOVE THIS SO-CALLED "UNUSED" PARAMETER!!! ⚠️⚠️⚠️⚠️
	@Suppress("unused") private val device: MidiDevice,
	private val port: MidiInputPort,
	name: String,
	type: CommunicationType
) : MidiSenderBase(name, type) {
	override fun sendMessageData(bytes: ByteArray, length: Int) = port.send(bytes, 0, length)
	override fun close() = port.close()
}