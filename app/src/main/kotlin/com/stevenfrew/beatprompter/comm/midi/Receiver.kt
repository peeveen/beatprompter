package com.stevenfrew.beatprompter.comm.midi

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverBase
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import kotlin.experimental.and

abstract class Receiver(name: String, type: CommunicationType) : ReceiverBase(name, type) {
	private var inSysEx: Boolean = false

	private var midiBankMSBs = ByteArray(16)
	private var midiBankLSBs = ByteArray(16)

	override fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int {
		var dataStart = 0
		while (dataStart < dataEnd) {
			val messageByte = buffer[dataStart]
			// All interesting MIDI signals have the top bit set.
			if (messageByte and EIGHT_ZERO_HEX != MidiMessage.ZERO_BYTE) {
				if (inSysEx) {
					if (messageByte == MidiMessage.MIDI_SYSEX_END_BYTE)
						inSysEx = false
				} else {
					// These are single byte messages.
					when (messageByte) {
						MidiMessage.MIDI_START_BYTE -> EventRouter.sendEventToSongDisplay(Events.MIDI_START_SONG)
						MidiMessage.MIDI_CONTINUE_BYTE -> EventRouter.sendEventToSongDisplay(Events.MIDI_CONTINUE_SONG)
						MidiMessage.MIDI_STOP_BYTE -> EventRouter.sendEventToSongDisplay(Events.MIDI_STOP_SONG)
						MidiMessage.MIDI_SONG_POSITION_POINTER_BYTE ->
							// This message requires two additional bytes.
							if (dataStart < dataEnd - 2)
								EventRouter.sendEventToSongDisplay(
									Events.MIDI_SET_SONG_POSITION,
									calculateMidiBeat(buffer[++dataStart], buffer[++dataStart]),
									0
								)
							else
							// Not enough data left.
								break

						MidiMessage.MIDI_SONG_SELECT_BYTE ->
							if (dataStart < dataEnd - 1)
								EventRouter.sendEventToSongList(
									Events.MIDI_SONG_SELECT,
									buffer[++dataStart].toInt(),
									0
								)
							else
							// Not enough data left.
								break

						MidiMessage.MIDI_SYSEX_START_BYTE -> inSysEx = true
						else -> {
							val channelsToListenTo = getIncomingChannels()
							val messageByteWithoutChannel = (messageByte and F_ZERO_HEX)
							// System messages start with 0xF0, we're past caring about those.
							if (messageByteWithoutChannel != F_ZERO_HEX) {
								val channel = (messageByte and 0x0F)
								if (channelsToListenTo and (1 shl channel.toInt()) != 0) {
									when (messageByteWithoutChannel) {
										MidiMessage.MIDI_PROGRAM_CHANGE_BYTE ->
											// This message requires one additional byte.
											if (dataStart < dataEnd - 1) {
												val pcValues = byteArrayOf(
													midiBankMSBs[channel.toInt()],
													midiBankLSBs[channel.toInt()],
													buffer[++dataStart],
													channel
												)
												EventRouter.sendEventToSongList(Events.MIDI_PROGRAM_CHANGE, pcValues)
											} else
												break

										MidiMessage.MIDI_CONTROL_CHANGE_BYTE ->
											// The only control change value we care about are bank selects.
											// Control change messages have two additional bytes.
											if (dataStart < dataEnd - 2) {
												val controller = buffer[++dataStart]
												val bankValue = buffer[++dataStart]
												if (controller == MidiMessage.MIDI_MSB_BANK_SELECT_CONTROLLER)
													midiBankMSBs[channel.toInt()] = bankValue
												else if (controller == MidiMessage.MIDI_LSB_BANK_SELECT_CONTROLLER)
													midiBankLSBs[channel.toInt()] = bankValue
											} else
												break
									}
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
			BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(this)
		}

		override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
			if (key == BeatPrompter.appResources.getString(R.string.pref_midiIncomingChannels_key))
				setIncomingChannels()
		}

		private const val EIGHT_ZERO_HEX = 0x80.toByte()
		private const val F_ZERO_HEX = 0xF0.toByte()

		private var incomingChannels = getIncomingChannelsPrefValue()
		private val incomingChannelsLock = Any()

		private fun getIncomingChannelsPrefValue(): Int = BeatPrompter.preferences.incomingMIDIChannels

		private fun getIncomingChannels(): Int =
			synchronized(incomingChannelsLock) {
				return incomingChannels
			}

		private fun setIncomingChannels() =
			synchronized(incomingChannelsLock) {
				incomingChannels = getIncomingChannelsPrefValue()
			}

		private fun calculateMidiBeat(byte1: Byte, byte2: Byte): Int {
			var firstHalf = byte2.toInt()
			firstHalf = firstHalf shl 7
			val secondHalf = byte1.toInt()
			return firstHalf or secondHalf
		}
	}
}