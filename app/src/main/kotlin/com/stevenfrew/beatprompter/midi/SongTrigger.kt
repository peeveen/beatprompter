package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.comm.midi.message.ControlChangeMessage
import com.stevenfrew.beatprompter.comm.midi.message.Message
import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
import com.stevenfrew.beatprompter.comm.midi.message.ProgramChangeMessage
import com.stevenfrew.beatprompter.comm.midi.message.SongSelectMessage
import com.stevenfrew.beatprompter.midi.alias.CommandValue
import com.stevenfrew.beatprompter.midi.alias.NoValue
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.midi.alias.WildcardValue
import org.w3c.dom.Element

class SongTrigger(
	private val mBankSelectMSB: Value,
	private val mBankSelectLSB: Value,
	private val mTriggerIndex: Value,
	private val mChannel: Value,
	private val mType: TriggerType
) {
	constructor(msb: Byte, lsb: Byte, triggerIndex: Byte, channel: Byte, type: TriggerType) : this(
		CommandValue(msb),
		CommandValue(lsb),
		CommandValue(triggerIndex),
		CommandValue(channel),
		type
	)

	val isDeadTrigger: Boolean
		get() = mType == TriggerType.SongSelect && mBankSelectMSB is NoValue && mBankSelectLSB is NoValue && mTriggerIndex is NoValue && mChannel is NoValue

	fun writeToXML(element: Element) {
		element.setAttribute(MSB_ATTRIBUTE, "${mBankSelectMSB.resolve()}")
		element.setAttribute(LSB_ATTRIBUTE, "${mBankSelectLSB.resolve()}")
		element.setAttribute(TRIGGER_INDEX_ATTRIBUTE, "${mTriggerIndex.resolve()}")
		element.setAttribute(CHANNEL_ATTRIBUTE, "${mChannel.resolve()}")
	}

	override fun equals(other: Any?): Boolean {
		if (other is SongTrigger) {
			val mst = other as SongTrigger?
			if (mst!!.mBankSelectMSB.matches(mBankSelectMSB))
				if (mst.mBankSelectLSB.matches(mBankSelectLSB))
					if (mst.mType == mType)
						if (mst.mTriggerIndex.matches(mTriggerIndex))
							return mst.mChannel.matches(mChannel)
		}
		return false
	}

	private fun canSend(): Boolean {
		return (mTriggerIndex is CommandValue
			&& mBankSelectLSB is CommandValue
			&& mBankSelectMSB is CommandValue)
	}

	fun getMIDIMessages(defaultOutputChannel: Byte): List<OutgoingMessage> {
		return mutableListOf<OutgoingMessage>().apply {
			if (canSend())
				if (mType == TriggerType.SongSelect)
					add(SongSelectMessage(mTriggerIndex.resolve().toInt()))
				else {
					val channel = if (mChannel is WildcardValue)
						defaultOutputChannel
					else
						mChannel.resolve()

					add(
						ControlChangeMessage(
							Message.MIDI_MSB_BANK_SELECT_CONTROLLER,
							mBankSelectMSB.resolve(),
							channel
						)
					)
					add(
						ControlChangeMessage(
							Message.MIDI_LSB_BANK_SELECT_CONTROLLER,
							mBankSelectLSB.resolve(),
							channel
						)
					)
					add(ProgramChangeMessage(mTriggerIndex.resolve().toInt(), channel.toInt()))
				}
		}
	}

	override fun hashCode(): Int {
		var result = mBankSelectMSB.hashCode()
		result = 31 * result + mBankSelectLSB.hashCode()
		result = 31 * result + mTriggerIndex.hashCode()
		result = 31 * result + mChannel.hashCode()
		result = 31 * result + mType.hashCode()
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
				} catch (numberFormatException: NumberFormatException) {
					// Can't be parsed. Oh well.
				}
			}
			return null
		}
	}
}