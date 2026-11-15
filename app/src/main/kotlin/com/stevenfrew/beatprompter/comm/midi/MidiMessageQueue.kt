package com.stevenfrew.beatprompter.comm.midi

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.midi.message.ClockMessage
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

class MidiMessageQueue(capacity: Int) : MessageQueue<MidiMessage>(capacity) {
	internal fun addBeatClockMessages(amount: Int) =
		synchronized(blockingQueue)
		{
			repeat(amount) {
				blockingQueue.put(ClockMessage)
			}
		}

	override fun shouldPutMessage(message: MidiMessage): Boolean =
		getOutgoingChannels() and (1 shl message.channel.toInt()) != 0

	companion object : SharedPreferences.OnSharedPreferenceChangeListener {

		init {
			BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(this)
		}

		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences?,
			key: String?
		) {
			if (key == BeatPrompter.appResources.getString(R.string.pref_midiOutgoingChannels_key))
				setOutgoingChannels()
		}

		private var outgoingChannels = getOutgoingChannelsPrefValue()
		private val outgoingChannelsLock = Any()

		private fun getOutgoingChannelsPrefValue(): Int =
			BeatPrompter.preferences.outgoingMIDIChannels

		private fun getOutgoingChannels(): Int =
			synchronized(outgoingChannelsLock) {
				return outgoingChannels
			}

		private fun setOutgoingChannels() =
			synchronized(outgoingChannelsLock) {
				outgoingChannels = getOutgoingChannelsPrefValue()
			}
	}
}
