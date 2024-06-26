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

	companion object {
		val DEAD_TRIGGER =
			SongTrigger(NoValue(), NoValue(), NoValue(), NoValue(), TriggerType.SongSelect)
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
}