package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger

class ReceiverTasks {
	private val receiverThreads = mutableMapOf<String, Thread>()
	private val receiverTasks = mutableMapOf<String, ReceiverTask>()
	private val receiverThreadsLock = Any()

	fun addReceiver(id: String, name: String, receiver: Receiver) =
		synchronized(receiverThreadsLock) {
			Logger.logComms { "Starting new receiver task '$id:' ($name)" }
			receiverTasks[id] = ReceiverTask(name, receiver).also {
				receiverThreads[id] = Thread(it).also { th ->
					th.start()
				}
			}
			Logger.logComms { "Started new receiver task '$id:' ($name)" }
		}

	fun stopAndRemoveReceiver(id: String) {
		val (receiverTask, receiverThread) = synchronized(receiverThreadsLock) {
			Logger.logComms { "Removing receiver task '$id'" }
			(receiverTasks.remove(id) to receiverThreads.remove(id)).also {
				Logger.logComms { "Removed receiver task '$id'" }
			}
		}
		Logger.logComms { "Stopping receiver task '$id'" }
		receiverTask?.apply {
			setUnregistered()
			stop()
		}
		receiverThread?.apply {
			interrupt()
			join()
		}
		Logger.logComms { "Stopped receiver task '$id'" }
	}

	fun stopAndRemoveAll(type: CommunicationType? = null) =
		synchronized(receiverThreadsLock) {
			Logger.logComms("Stopping ALL receiver tasks of type '${type}'")
			receiverTasks.filter { type == null || it.value.type == type }.keys.forEach {
				stopAndRemoveReceiver(it)
			}
			Logger.logComms("Stopped ALL receiver tasks of type '${type}")
		}

	val taskCount: Int
		get() = synchronized(receiverThreadsLock) { receiverTasks.size }
}