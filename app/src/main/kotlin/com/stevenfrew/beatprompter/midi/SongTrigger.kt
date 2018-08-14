package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.FileParseError
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.ArrayList

class SongTrigger constructor(bankSelectMSB: Value, bankSelectLSB: Value, triggerIndex: Value, channel: Value, songSelect: Boolean) {

    constructor(msb: Byte, lsb: Byte, triggerIndex: Byte, channel: Byte, isSongSelect: Boolean): this(CommandValue(msb), CommandValue(lsb), CommandValue(triggerIndex), CommandValue(channel), isSongSelect)

    companion object {
        private const val MSB_ATTRIBUTE_NAME = "bankSelectMSB"
        private const val LSB_ATTRIBUTE_NAME = "bankSelectLSB"
        private const val TRIGGER_INDEX_ATTRIBUTE_NAME = "triggerIndex"
        private const val CHANNEL_ATTRIBUTE_NAME = "channel"
        private const val IS_SONG_SELECT_ATTRIBUTE_NAME = "isSongSelect"
        val DEAD_TRIGGER = SongTrigger(NoValue(), NoValue(), NoValue(), NoValue(), true)

        fun readFromXMLElement(element: Element): SongTrigger {
            val msbString = element.getAttribute(MSB_ATTRIBUTE_NAME)
            val lsbString = element.getAttribute(LSB_ATTRIBUTE_NAME)
            val triggerIndexString = element.getAttribute(TRIGGER_INDEX_ATTRIBUTE_NAME)
            val channelString = element.getAttribute(CHANNEL_ATTRIBUTE_NAME)
            val isSongSelectString = element.getAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME)

            val msbValue = Value.parseValue(msbString)
            val lsbValue = Value.parseValue(lsbString)
            val triggerIndexValue = Value.parseValue(triggerIndexString)
            val channelValue = Value.parseChannelValue(channelString)
            val isSongSelect = java.lang.Boolean.parseBoolean(isSongSelectString)

            return SongTrigger(msbValue, lsbValue, triggerIndexValue, channelValue, isSongSelect)
        }

        fun parse(descriptor: String?, songSelect: Boolean, lineNumber: Int, errors: MutableList<FileParseError>): SongTrigger? {
            if (descriptor == null)
                return null
            val bits = descriptor.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (f in bits.indices)
                bits[f] = bits[f].trim()
            var msb: Value = WildcardValue()
            var lsb: Value = WildcardValue()
            var channel: Value = WildcardValue()
            if (bits.size > 1)
                if (songSelect)
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_value)))
            if (bits.size > 4 || bits.isEmpty())
                if (songSelect)
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_value)))
                else
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.song_index_must_have_one_two_or_three_values)))

            if (bits.size > 3) {
                val value = Value.parseValue(bits[3], lineNumber, 3, bits.size, errors)
                if (value is ChannelValue)
                    channel = value
            }

            if (bits.size > 2)
                lsb = Value.parseValue(bits[2], lineNumber, 2, bits.size, errors)

            if (bits.size > 1)
                msb = Value.parseValue(bits[1], lineNumber, 1, bits.size, errors)

            val index = Value.parseValue(bits[0], lineNumber, 0, bits.size, errors)

            return SongTrigger(msb, lsb, index, channel, songSelect)
        }
    }

    private val mBankSelectMSB=bankSelectMSB
    private val mBankSelectLSB=bankSelectLSB
    private val mTriggerIndex=triggerIndex
    private val mChannel=channel
    private val mSongSelect=songSelect

    override fun equals(other: Any?): Boolean {
        if (other is SongTrigger) {
            val mst = other as SongTrigger?
            if (mst!!.mBankSelectMSB.matches(mBankSelectMSB))
                if (mst.mBankSelectLSB.matches(mBankSelectLSB))
                    if (mst.mSongSelect == mSongSelect)
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
        triggerElement.setAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME, "" + mSongSelect)
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
        if (mSongSelect)
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
        result = 31 * result + mSongSelect.hashCode()
        return result
    }

}