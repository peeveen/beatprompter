package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

class ReceiverTask(
	val mName: String,
	private val mReceiver: Receiver
) : Task(true) {
	private var mUnregistered = false

	val type: String
		get() = mReceiver.type

	override fun doWork() =
		try {
			mReceiver.receive()
		} catch (t: Throwable) {
			// Any I/O error means this receiver is dead to us.
			Logger.logComms("Unexpected IO exception from receiver.", t)
			Logger.logComms { "Receiver '$mName' threw an exception. Assuming it to be dead." }
			if (!mUnregistered)
				mReceiver.unregister(this)
			super.stop()
			Logger.logComms("Receiver is now stopped.")
			EventRouter.sendEventToSongList(Events.CONNECTION_LOST, mName)
		}

	// Receivers often block when trying to receive data, so closing the socket or whatever behind
	// the scenes will usually kickstart it into action.
	override fun stop() =
		try {
			mReceiver.close()
		} catch (exception: Exception) {
			// At least we tried ...
		}

	fun setUnregistered() {
		mUnregistered = true
	}
}