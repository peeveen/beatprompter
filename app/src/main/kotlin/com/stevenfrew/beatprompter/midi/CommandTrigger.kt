package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.midi.alias.CommandValue
import com.stevenfrew.beatprompter.midi.alias.NoValue
import com.stevenfrew.beatprompter.midi.alias.Value
import org.w3c.dom.Element

class CommandTrigger(
	private val controller: Value,
	private val value: Value,
	private val channel: Value
) : MidiTrigger {
	constructor(controller: Byte, value: Byte, channel: Byte) : this(
		CommandValue(controller),
		CommandValue(value),
		CommandValue(channel)
	)

	override val isDeadTrigger: Boolean
		get() = controller is NoValue && value is NoValue && channel is NoValue

	override fun writeToXML(element: Element) {
		element.setAttribute(CONTROLLER_ATTRIBUTE, "${controller.resolve()}")
		element.setAttribute(VALUE_ATTRIBUTE, "${value.resolve()}")
		element.setAttribute(CHANNEL_ATTRIBUTE, "${channel.resolve()}")
	}

	// These never get sent out, so no point putting anything together.
	override fun getMIDIMessages(defaultOutputChannel: Byte): List<MidiMessage> = listOf()

	override fun equals(other: Any?): Boolean =
		if (other is CommandTrigger) {
			val otherCommandTrigger = other as CommandTrigger?
			otherCommandTrigger!!.controller.matches(controller) &&
				otherCommandTrigger.value.matches(value) &&
				otherCommandTrigger.channel.matches(channel)
		} else false

	override fun hashCode(): Int {
		var result = controller.hashCode()
		result = 31 * result + value.hashCode()
		result = 31 * result + channel.hashCode()
		return result
	}

	companion object {
		private const val CONTROLLER_ATTRIBUTE = "controller"
		private const val VALUE_ATTRIBUTE = "value"
		private const val CHANNEL_ATTRIBUTE = "channel"
	}
}