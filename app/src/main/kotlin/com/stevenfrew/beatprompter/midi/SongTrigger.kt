package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.comm.midi.message.ControlChangeMessage
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.comm.midi.message.ProgramChangeMessage
import com.stevenfrew.beatprompter.comm.midi.message.SongSelectMessage
import com.stevenfrew.beatprompter.midi.alias.CommandValue
import com.stevenfrew.beatprompter.midi.alias.NoValue
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.midi.alias.WildcardValue
import org.w3c.dom.Element

class SongTrigger(
	private val bankSelectMSB: Value,
	private val bankSelectLSB: Value,
	private val triggerIndex: Value,
	private val channel: Value,
	private val type: TriggerType
) {
	constructor(msb: Byte, lsb: Byte, triggerIndex: Byte, channel: Byte, type: TriggerType) : this(
		CommandValue(msb),
		CommandValue(lsb),
		CommandValue(triggerIndex),
		CommandValue(channel),
		type
	)

	val isDeadTrigger: Boolean
		get() = type == TriggerType.SongSelect && bankSelectMSB is NoValue && bankSelectLSB is NoValue && triggerIndex is NoValue && channel is NoValue

	fun writeToXML(element: Element) {
		element.setAttribute(MSB_ATTRIBUTE, "${bankSelectMSB.resolve()}")
		element.setAttribute(LSB_ATTRIBUTE, "${bankSelectLSB.resolve()}")
		element.setAttribute(TRIGGER_INDEX_ATTRIBUTE, "${triggerIndex.resolve()}")
		element.setAttribute(CHANNEL_ATTRIBUTE, "${channel.resolve()}")
	}

	override fun equals(other: Any?): Boolean =
		if (other is SongTrigger) {
			val mst = other as SongTrigger?
			mst!!.bankSelectMSB.matches(bankSelectMSB) &&
				mst.bankSelectLSB.matches(bankSelectLSB) &&
				mst.type == type &&
				mst.triggerIndex.matches(triggerIndex) &&
				mst.channel.matches(channel)
		} else false

	private fun canSend(): Boolean =
		triggerIndex is CommandValue
			&& bankSelectLSB is CommandValue
			&& bankSelectMSB is CommandValue

	fun getMIDIMessages(defaultOutputChannel: Byte): List<MidiMessage> =
		mutableListOf<MidiMessage>().apply {
			if (canSend())
				if (type == TriggerType.SongSelect)
					add(SongSelectMessage(triggerIndex.resolve().toInt()))
				else {
					val channel = if (channel is WildcardValue)
						defaultOutputChannel
					else
						channel.resolve()

					add(
						ControlChangeMessage(
							MidiMessage.MIDI_MSB_BANK_SELECT_CONTROLLER,
							bankSelectMSB.resolve(),
							channel
						)
					)
					add(
						ControlChangeMessage(
							MidiMessage.MIDI_LSB_BANK_SELECT_CONTROLLER,
							bankSelectLSB.resolve(),
							channel
						)
					)
					add(ProgramChangeMessage(triggerIndex.resolve().toInt(), channel.toInt()))
				}
		}

	override fun hashCode(): Int {
		var result = bankSelectMSB.hashCode()
		result = 31 * result + bankSelectLSB.hashCode()
		result = 31 * result + triggerIndex.hashCode()
		result = 31 * result + channel.hashCode()
		result = 31 * result + type.hashCode()
		return result
	}

	companion object {
		val DEAD_TRIGGER =
			SongTrigger(NoValue(), NoValue(), NoValue(), NoValue(), TriggerType.SongSelect)

		private const val MSB_ATTRIBUTE = "msb"
		private const val LSB_ATTRIBUTE = "lsb"
		private const val TRIGGER_INDEX_ATTRIBUTE = "triggerIndex"
		private const val CHANNEL_ATTRIBUTE = "channel"

		fun readFromXml(element: Element, type: TriggerType): SongTrigger? {
			if (element.hasAttribute(MSB_ATTRIBUTE) &&
				element.hasAttribute(LSB_ATTRIBUTE) &&
				element.hasAttribute(TRIGGER_INDEX_ATTRIBUTE)
				&& element.hasAttribute(CHANNEL_ATTRIBUTE)
			) {
				val msbString = element.getAttribute(MSB_ATTRIBUTE)
				val lsbString = element.getAttribute(LSB_ATTRIBUTE)
				val triggerIndexString = element.getAttribute(TRIGGER_INDEX_ATTRIBUTE)
				val channelString = element.getAttribute(CHANNEL_ATTRIBUTE)
				try {
					val msb = msbString.toByte()
					val lsb = lsbString.toByte()
					val triggerIndex = triggerIndexString.toByte()
					val channel = channelString.toByte()
					return SongTrigger(msb, lsb, triggerIndex, channel, type)
				} catch (_: NumberFormatException) {
					// Can't be parsed. Oh well.
				}
			}
			return null
		}
	}
}