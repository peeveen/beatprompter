package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
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

	private fun tryPutWithMidiMessages(predicate: (Alias) -> Boolean) {
		Cache.cachedCloudItems.midiAliases.filter(predicate).forEach {
			val messages = it.resolve(
				Cache.cachedCloudItems.midiAliases,
				byteArrayOf(),
				MidiMessage.getChannelFromBitmask(Preferences.defaultMIDIOutputChannel)
			)
			messages.forEach { msg -> tryPutMessage(msg, it.name) }
		}
	}

	fun putStartMessage() {
		tryPutMessage(StartMessage, "Start")
		tryPutWithMidiMessages { a -> a.withMidiStart }
	}

	fun putContinueMessage() {
		tryPutMessage(ContinueMessage, "Continue")
		tryPutWithMidiMessages { a -> a.withMidiContinue }
	}

	fun putStopMessage() {
		tryPutMessage(StopMessage, "Stop")
		tryPutWithMidiMessages { a -> a.withMidiStop }
	}

	internal fun putMessages(messages: List<Message>) {
		if (initialised) midiOutQueue.putMessages(messages)
	}
}
