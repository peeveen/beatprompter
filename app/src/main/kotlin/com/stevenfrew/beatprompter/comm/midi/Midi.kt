package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

object Midi {
	private const val MIDI_QUEUE_SIZE = 4096

	private var initialised = false
	private val midiOutQueue = MidiMessageQueue(MIDI_QUEUE_SIZE)
	private val senderTask = SenderTask(midiOutQueue)
	private val receiverTasks = ReceiverTasks()
	private val senderTaskThread = Thread(senderTask).also { it.priority = Thread.MAX_PRIORITY }

	fun initialize(context: Context) {
		senderTaskThread.start()
		Task.resumeTask(senderTask)

		NativeMidiController.initialize(context, senderTask, receiverTasks)
		UsbMidiController.initialize(context, senderTask, receiverTasks)
		BluetoothMidiController.initialize(context, senderTask, receiverTasks)

		initialised = true
	}

	fun removeReceiver(task: ReceiverTask) = receiverTasks.stopAndRemoveReceiver(task.name)

	internal fun addBeatClockMessages(amount: Int) {
		if (initialised) midiOutQueue.addBeatClockMessages(amount)
	}

	internal fun putMessage(message: OutgoingMessage) {
		if (initialised) midiOutQueue.putMessage(message)
	}

	internal fun putMessages(messages: List<OutgoingMessage>) {
		if (initialised) midiOutQueue.putMessages(messages)
	}
}
