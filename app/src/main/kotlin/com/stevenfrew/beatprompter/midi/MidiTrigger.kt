package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import org.w3c.dom.Element

interface MidiTrigger {
	val isDeadTrigger: Boolean
	fun writeToXML(element: Element)
	fun getMIDIMessages(defaultOutputChannel: Byte): List<MidiMessage>
}