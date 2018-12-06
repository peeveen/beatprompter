package com.stevenfrew.beatprompter.comm.midi

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.midi.message.Message
import kotlin.experimental.and

abstract class Receiver(name: String) : ReceiverBase(name) {
    private var mInSysEx: Boolean = false

    private var mMidiBankMSBs = ByteArray(16)
    private var mMidiBankLSBs = ByteArray(16)

    override fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int {
        var dataStart = 0
        while (dataStart < dataEnd) {
            val messageByte = buffer[dataStart]
            // All interesting MIDI signals have the top bit set.
            if (messageByte and EIGHT_ZERO_HEX != ZERO_AS_BYTE) {
                if (mInSysEx) {
                    if (messageByte == Message.MIDI_SYSEX_END_BYTE) {
                        Logger.logComms("Received MIDI SysEx end message.")
                        mInSysEx = false
                    }
                } else {
                    // These are single byte messages.
                    if (messageByte == Message.MIDI_START_BYTE)
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_START_SONG)
                    else if (messageByte == Message.MIDI_CONTINUE_BYTE)
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_CONTINUE_SONG)
                    else if (messageByte == Message.MIDI_STOP_BYTE)
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_STOP_SONG)
                    else if (messageByte == Message.MIDI_SONG_POSITION_POINTER_BYTE)
                    // This message requires two additional bytes.
                        if (dataStart < dataEnd - 2)
                            EventHandler.sendEventToSongDisplay(EventHandler.MIDI_SET_SONG_POSITION, calculateMidiBeat(buffer[++dataStart], buffer[++dataStart]), 0)
                        else
                        // Not enough data left.
                            break
                    else if (messageByte == Message.MIDI_SONG_SELECT_BYTE)
                        if (dataStart < dataEnd - 1)
                            EventHandler.sendEventToSongList(EventHandler.MIDI_SONG_SELECT, buffer[++dataStart].toInt(), 0)
                        else
                        // Not enough data left.
                            break
                    else if (messageByte == Message.MIDI_SYSEX_START_BYTE) {
                        Logger.logComms("Received MIDI SysEx start message.")
                        mInSysEx = true
                    } else {
                        val channelsToListenTo = getIncomingChannels()
                        val messageByteWithoutChannel = (messageByte and F_ZERO_HEX)
                        // System messages start with 0xF0, we're past caring about those.
                        if (messageByteWithoutChannel != F_ZERO_HEX) {
                            val channel = (messageByte and 0x0F)
                            if (channelsToListenTo and (1 shl channel.toInt()) != 0) {
                                if (messageByteWithoutChannel == Message.MIDI_PROGRAM_CHANGE_BYTE)
                                // This message requires one additional byte.
                                    if (dataStart < dataEnd - 1) {
                                        val pcValues = byteArrayOf(mMidiBankMSBs[channel.toInt()], mMidiBankLSBs[channel.toInt()], buffer[++dataStart], channel)
                                        EventHandler.sendEventToSongList(EventHandler.MIDI_PROGRAM_CHANGE, pcValues)
                                    } else
                                        break
                                else if (messageByteWithoutChannel == Message.MIDI_CONTROL_CHANGE_BYTE) {
                                    // The only control change value we care about are bank selects.
                                    // Control change messages have two additional bytes.
                                    if (dataStart < dataEnd - 2) {
                                        val controller = buffer[++dataStart]
                                        val bankValue = buffer[++dataStart]
                                        if (controller == Message.MIDI_MSB_BANK_SELECT_CONTROLLER)
                                            mMidiBankMSBs[channel.toInt()] = bankValue
                                        else if (controller == Message.MIDI_LSB_BANK_SELECT_CONTROLLER)
                                            mMidiBankLSBs[channel.toInt()] = bankValue
                                    } else
                                        break
                                }
                            }
                        }
                    }
                }
            }
            ++dataStart
        }
        // If we broke out of this loop before the end of the actual loop, then f will be ON the last read byte
        // We want to move back one so that that byte becomes part of the next parsing operation.
        if (dataStart != dataEnd)
            --dataStart
        return dataStart
    }

    companion object : SharedPreferences.OnSharedPreferenceChangeListener {
        init {
            Preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == BeatPrompter.getResourceString(R.string.pref_midiIncomingChannels_key))
                setIncomingChannels()
        }

        private const val ZERO_AS_BYTE = 0.toByte()
        private const val EIGHT_ZERO_HEX = 0x80.toByte()
        private const val F_ZERO_HEX = 0xF0.toByte()

        private var mIncomingChannels = getIncomingChannelsPrefValue()
        private val mIncomingChannelsLock = Any()

        private fun getIncomingChannelsPrefValue(): Int {
            return Preferences.incomingMIDIChannels
        }

        private fun getIncomingChannels(): Int {
            synchronized(mIncomingChannelsLock) {
                return mIncomingChannels
            }
        }

        private fun setIncomingChannels() {
            synchronized(mIncomingChannelsLock) {
                mIncomingChannels = getIncomingChannelsPrefValue()
            }
        }

        private fun calculateMidiBeat(byte1: Byte, byte2: Byte): Int {
            var firstHalf = byte2.toInt()
            firstHalf = firstHalf shl 7
            val secondHalf = byte1.toInt()
            return firstHalf or secondHalf
        }
    }
}