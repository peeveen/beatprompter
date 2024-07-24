package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiInputPort
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.SenderBase

class NativeSender(
	// Annoyingly, if we don't keep a hold of the MIDI device reference for Bluetooth MIDI, then
	// it automatically closes. So I'm storing it here.
	private val mDevice: MidiDevice,
	private val mPort: MidiInputPort,
	name: String,
	type: CommunicationType
) : SenderBase(name, type) {
	override fun sendMessageData(bytes: ByteArray, length: Int) = mPort.send(bytes, 0, length)
	override fun close() = mPort.close()
}