package com.stevenfrew.beatprompter.comm.midi

import android.content.Context
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

object Midi {
	private const val MIDI_QUEUE_SIZE = 4096

	private var mInitialised = false
	private val mMIDIOutQueue = MidiMessageQueue(MIDI_QUEUE_SIZE)
	private val mSenderTask = SenderTask(mMIDIOutQueue)
	private val mReceiverTasks = ReceiverTasks()
	private val mSenderTaskThread = Thread(mSenderTask).also { it.priority = Thread.MAX_PRIORITY }

	fun initialize(context: Context) {
		mSenderTaskThread.start()
		Task.resumeTask(mSenderTask)

		NativeMidiController(context, mSenderTask, mReceiverTasks)
		UsbMidiController(context, mSenderTask, mReceiverTasks)

		mInitialised = true
	}

	fun removeReceiver(task: ReceiverTask) = mReceiverTasks.stopAndRemoveReceiver(task.mName)

	internal fun addBeatClockMessages(amount: Int) {
		if (mInitialised) mMIDIOutQueue.addBeatClockMessages(amount)
	}

	internal fun putMessage(message: OutgoingMessage) {
		if (mInitialised) mMIDIOutQueue.putMessage(message)
	}

	internal fun putMessages(messages: List<OutgoingMessage>) {
		if (mInitialised) mMIDIOutQueue.putMessages(messages)
	}
}
