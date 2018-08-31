package com.stevenfrew.beatprompter.midi

class SongTrigger constructor(bankSelectMSB: Value, bankSelectLSB: Value, triggerIndex: Value, channel: Value, type: TriggerType) {

    private val mBankSelectMSB=bankSelectMSB
    private val mBankSelectLSB=bankSelectLSB
    private val mTriggerIndex=triggerIndex
    private val mChannel=channel
    private val mType=type

    constructor(msb: Byte, lsb: Byte, triggerIndex: Byte, channel: Byte, type: TriggerType): this(CommandValue(msb), CommandValue(lsb), CommandValue(triggerIndex), CommandValue(channel), type)

    companion object {
        val DEAD_TRIGGER = SongTrigger(NoValue(), NoValue(), NoValue(), NoValue(), TriggerType.SongSelect)
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

    fun isSendable(): Boolean {
        return (mTriggerIndex is CommandValue
                && mBankSelectLSB is CommandValue
                && mBankSelectMSB is CommandValue)
    }

    @Throws(ResolutionException::class)
    fun getMIDIMessages(defaultOutputChannel: Byte): List<OutgoingMessage> {
        val outputMessages = mutableListOf<OutgoingMessage>()
        if (mType==TriggerType.SongSelect)
            outputMessages.add(SongSelectMessage(mTriggerIndex.resolve().toInt()))
        else {
            val channel = if (mChannel is WildcardValue)
                defaultOutputChannel
            else
                mChannel.resolve()

            outputMessages.add(ControlChangeMessage(Message.MIDI_MSB_BANK_SELECT_CONTROLLER, mBankSelectMSB.resolve(), channel))
            outputMessages.add(ControlChangeMessage(Message.MIDI_LSB_BANK_SELECT_CONTROLLER, mBankSelectLSB.resolve(), channel))
            outputMessages.add(ProgramChangeMessage(mTriggerIndex.resolve().toInt(), channel.toInt()))
        }
        return outputMessages
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