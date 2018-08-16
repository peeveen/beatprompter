package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import org.w3c.dom.Document
import org.w3c.dom.Element

class SongTrigger constructor(bankSelectMSB: Value, bankSelectLSB: Value, triggerIndex: Value, channel: Value, type: TriggerType) {

    constructor(msb: Byte, lsb: Byte, triggerIndex: Byte, channel: Byte, type: TriggerType): this(CommandValue(msb), CommandValue(lsb), CommandValue(triggerIndex), CommandValue(channel), type)

    companion object {
        private const val MSB_ATTRIBUTE_NAME = "bankSelectMSB"
        private const val LSB_ATTRIBUTE_NAME = "bankSelectLSB"
        private const val TRIGGER_INDEX_ATTRIBUTE_NAME = "triggerIndex"
        private const val CHANNEL_ATTRIBUTE_NAME = "channel"
        private const val TRIGGER_TYPE_ATTRIBUTE_NAME = "triggerType"
        val DEAD_TRIGGER = SongTrigger(NoValue(), NoValue(), NoValue(), NoValue(), TriggerType.SongSelect)

        fun readFromXMLElement(element: Element): SongTrigger {
            val msbString = element.getAttribute(MSB_ATTRIBUTE_NAME)
            val lsbString = element.getAttribute(LSB_ATTRIBUTE_NAME)
            val triggerIndexString = element.getAttribute(TRIGGER_INDEX_ATTRIBUTE_NAME)
            val channelString = element.getAttribute(CHANNEL_ATTRIBUTE_NAME)
            val triggerTypeString = element.getAttribute(TRIGGER_TYPE_ATTRIBUTE_NAME)

            val msbValue = Value.parseValue(msbString)
            val lsbValue = Value.parseValue(lsbString)
            val triggerIndexValue = Value.parseValue(triggerIndexString)
            val channelValue = Value.parseChannelValue(channelString)
            val triggerType = TriggerType.valueOf(triggerTypeString)

            return SongTrigger(msbValue, lsbValue, triggerIndexValue, channelValue,triggerType)
        }
    }

    private val mBankSelectMSB=bankSelectMSB
    private val mBankSelectLSB=bankSelectLSB
    private val mTriggerIndex=triggerIndex
    private val mChannel=channel
    private val mType=type

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

    fun writeToXML(doc: Document, parent: Element, tag: String) {
        val triggerElement = doc.createElement(tag)
        triggerElement.setAttribute(MSB_ATTRIBUTE_NAME, mBankSelectMSB.toString())
        triggerElement.setAttribute(LSB_ATTRIBUTE_NAME, mBankSelectLSB.toString())
        triggerElement.setAttribute(TRIGGER_INDEX_ATTRIBUTE_NAME, mTriggerIndex.toString())
        triggerElement.setAttribute(CHANNEL_ATTRIBUTE_NAME, mChannel.toString())
        triggerElement.setAttribute(TRIGGER_TYPE_ATTRIBUTE_NAME, mType.toString())
        parent.appendChild(triggerElement)
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