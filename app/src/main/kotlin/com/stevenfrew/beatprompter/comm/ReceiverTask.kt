package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task

class ReceiverTask(
	val name: String,
	private val receiver: Receiver
) : Task(true) {
	private var unregistered = false

	val type: CommunicationType
		get() = receiver.type

	override fun doWork() =
		try {
			receiver.receive()
		} catch (t: Throwable) {
			// Any I/O error means this receiver is dead to us.
			Logger.logComms("Unexpected IO exception from receiver.", t)
			Logger.logComms({ "Receiver '$name' threw an exception. Assuming it to be dead." }, true)
			if (!unregistered)
				receiver.unregister(this)
			super.stop()
			Logger.logComms("Receiver is now stopped.")
			ConnectionNotificationTask.addDisconnection(ConnectionDescriptor(name, receiver.type))
			Unit
		}

	// Receivers often block when trying to receive data, so closing the socket or whatever behind
	// the scenes will usually kickstart it into action.
	override fun stop() =
		try {
			receiver.close()
			true
		} catch (_: Exception) {
			// At least we tried ...
			false
		}

	fun setUnregistered() {
		unregistered = true
	}
}