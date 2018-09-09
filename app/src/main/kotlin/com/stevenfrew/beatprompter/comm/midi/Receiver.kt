package com.stevenfrew.beatprompter.comm.midi

import android.content.SharedPreferences
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.midi.message.Message
import com.stevenfrew.beatprompter.comm.midi.message.incoming.*
import kotlin.experimental.and

abstract class Receiver: ReceiverBase<IncomingMessage>() {
    private var mInSysEx: Boolean = false

    override fun parseMessageData(buffer: ByteArray, dataStart: Int, dataEnd: Int): Pair<List<IncomingMessage>, Int> {
        var f = dataStart
        val parsedMessages= mutableListOf<IncomingMessage>()
        while (f < dataEnd) {
            val messageByte = buffer[f]
            // All interesting MIDI signals have the top bit set.
            if (messageByte and EIGHT_ZERO_HEX != ZERO_AS_BYTE) {
                if (mInSysEx) {
                    if (messageByte == Message.MIDI_SYSEX_END_BYTE) {
                        Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx end message.")
                        mInSysEx = false
                    }
                } else {
                    // These are single byte messages.
                    if (messageByte == Message.MIDI_START_BYTE)
                        parsedMessages.add(StartMessage())
                    else if (messageByte == Message.MIDI_CONTINUE_BYTE)
                        parsedMessages.add(ContinueMessage())
                    else if (messageByte == Message.MIDI_STOP_BYTE)
                        parsedMessages.add(StopMessage())
                    else if (messageByte == Message.MIDI_SONG_POSITION_POINTER_BYTE)
                    // This message requires two additional bytes.
                        if(f < dataEnd - 2)
                            parsedMessages.add(SongPositionPointerMessage(byteArrayOf(buffer[++f], buffer[++f])))
                        else
                        // Not enough data left.
                            break
                    else if (messageByte == Message.MIDI_SONG_SELECT_BYTE)
                        if (f < dataEnd - 1)
                            parsedMessages.add(SongSelectMessage(buffer[++f]))
                        else
                        // Not enough data left.
                            break
                    else if (messageByte == Message.MIDI_SYSEX_START_BYTE) {
                        Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx start message.")
                        mInSysEx = true
                    } else {
                        val channelsToListenTo = getIncomingChannels()
                        val messageByteWithoutChannel = (messageByte and F_ZERO_HEX)
                        // System messages start with 0xF0, we're past caring about those.
                        if (messageByteWithoutChannel != F_ZERO_HEX) {
                            val channel = (messageByte and 0x0F)
                            if (channelsToListenTo and (1 shl channel.toInt()) != 0) {
                                if (messageByteWithoutChannel == Message.MIDI_PROGRAM_CHANGE_BYTE)
                                    if (f < dataEnd - 1)
                                        parsedMessages.add(ProgramChangeMessage(buffer[++f],channel))
                                    else
                                        break
                                else if (messageByteWithoutChannel == Message.MIDI_CONTROL_CHANGE_BYTE)
                                // This message requires two additional bytes.
                                    if(f<dataEnd-2)
                                        parsedMessages.add(ControlChangeMessage(buffer[++f], buffer[++f],channel))
                                    else
                                        break
                            }
                        }
                    }
                }
            }
            ++f
        }
        // If we broke out of this loop before the end of the actual loop, then f will be ON the last read byte
        // We want to move back one so that that byte becomes part of the next parsing operation.
        if(f!=dataEnd)
            --f
        return Pair(parsedMessages,f-dataStart)
    }

    companion object:SharedPreferences.OnSharedPreferenceChangeListener {
        init {
            BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key))
                setIncomingChannels()
        }

        private const val ZERO_AS_BYTE=0.toByte()
        private const val EIGHT_ZERO_HEX=0x80.toByte()
        private const val F_ZERO_HEX=0xF0.toByte()

        private var mIncomingChannels=getIncomingChannelsPrefValue()
        private val mIncomingChannelsLock = Any()

        private fun getIncomingChannelsPrefValue():Int {
            return BeatPrompterApplication.preferences.getInt(BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key), 65535)
        }

        private fun getIncomingChannels(): Int {
            synchronized(mIncomingChannelsLock) {
                return mIncomingChannels
            }
        }

        fun setIncomingChannels() {
            synchronized(mIncomingChannelsLock) {
                mIncomingChannels = getIncomingChannelsPrefValue()
            }
        }
    }
}