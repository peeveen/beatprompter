package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.comm.Message
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.midi.message.ContinueMessage
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.comm.midi.message.StartMessage
import com.stevenfrew.beatprompter.comm.midi.message.StopMessage
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.midi.alias.AliasSet

object Midi {
	private const val MIDI_QUEUE_SIZE = 4096

	private var initialised = false
	private val midiOutQueue = MidiMessageQueue(MIDI_QUEUE_SIZE)
	private val senderTask = SenderTask(midiOutQueue)
	private val receiverTasks = ReceiverTasks()
	private val senderTaskThread = Thread(senderTask).also { it.priority = Thread.MAX_PRIORITY }

	fun initialize(context: Context) {
		senderTaskThread.start()
		Task.resumeTask(senderTask, senderTaskThread)

		NativeMidiController.initialize(context, senderTask, receiverTasks)
		UsbMidiController.initialize(context, senderTask, receiverTasks)
		BluetoothMidiController.initialize(context, senderTask, receiverTasks)

		initialised = true
	}

	fun removeReceiver(task: ReceiverTask) = receiverTasks.stopAndRemoveReceiver(task.name)

	internal fun addBeatClockMessages(amount: Int) {
		if (initialised) midiOutQueue.addBeatClockMessages(amount)
	}

	internal fun putMessage(message: Message) {
		if (initialised) midiOutQueue.putMessage(message)
	}

	private fun tryPutMessage(message: Message, messageName: String) =
		try {
			putMessage(message)
		} catch (e: Exception) {
			Logger.logComms({ "Failed to add MIDI $messageName signal to output queue." }, e)
		}

	private fun tryPutWithMidiMessages(aliasSets: Set<AliasSet>, predicate: (Alias) -> Boolean) =
		aliasSets.forEach { aliasSet ->
			aliasSet.aliases.filter(predicate).forEach {
				val (messages, resolutionSets) = it.resolve(
					aliasSet,
					Cache.cachedCloudItems.midiAliasSets,
					byteArrayOf(),
					MidiMessage.getChannelFromBitmask(BeatPrompter.preferences.defaultMIDIOutputChannel)
				)
				if (aliasSets.containsAll(resolutionSets))
					messages.forEach { msg -> tryPutMessage(msg, it.name) }
			}
		}

	fun putStartMessage(aliasSets: Set<AliasSet>) {
		tryPutMessage(StartMessage, "Start")
		tryPutWithMidiMessages(aliasSets) { a -> a.withMidiStart }
	}

	fun putContinueMessage(aliasSets: Set<AliasSet>) {
		tryPutMessage(ContinueMessage, "Continue")
		tryPutWithMidiMessages(aliasSets) { a -> a.withMidiContinue }
	}

	fun putStopMessage(aliasSets: Set<AliasSet>) {
		tryPutMessage(StopMessage, "Stop")
		tryPutWithMidiMessages(aliasSets) { a -> a.withMidiStop }
	}

	internal fun putMessages(messages: List<Message>) {
		if (initialised) midiOutQueue.putMessages(messages)
	}
}
