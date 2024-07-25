package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

class SenderTask(private val messageQueue: MessageQueue) : Task(false) {
	private val senders = mutableListOf<Sender>()
	private val sendersLock = Any()

	override fun doWork() =
		try {
			// This take() will block if the queue is empty
			val messages = messageQueue.getMessages()

			synchronized(sendersLock) {
				for (f in senders.size - 1 downTo 0) {
					// Sanity check in case a dead sender was removed.
					if (f < senders.size) {
						try {
							senders[f].send(messages)
						} catch (commException: Exception) {
							// Problem with the I/O? This sender is now dead to us.
							Logger.logComms("Sender threw an exception. Assuming it to be dead.")
							removeSender(senders[f].name)
						}
					}
				}
			}
		} catch (interruptedException: InterruptedException) {
			// Must have been signalled to stop ... main Task loop will cater for this.
		}

	fun addSender(id: String, sender: Sender) =
		synchronized(sendersLock) {
			Logger.logComms { "Adding new sender '$id' (${sender.name}) to the collection" }
			senders.add(sender)
		}

	fun removeSender(id: String) =
		getSender(id)?.also { sender ->
			Logger.logComms { "Removing sender '$id' from the collection" }
			closeSender(sender)
			Logger.logComms { "Sender '$id' has been closed." }
			synchronized(sendersLock) {
				senders.removeAll { it.name == id }
			}
			Logger.logComms { "Sender '$id' is now dead ... notifying main activity for UI." }
			EventRouter.sendEventToSongList(Events.CONNECTION_LOST, sender.name)
		}

	fun removeAll(type: CommunicationType? = null) {
		Logger.logComms("Removing ALL senders of type '${type}' from the collection.")
		synchronized(sendersLock) {
			// Avoid concurrent modification exception by converting to array.
			val senderArray = senders.filter { type == null || it.type == type }.toTypedArray()
			senderArray.forEach { removeSender(it.name) }
		}
	}

	private fun getSender(id: String): Sender? =
		synchronized(sendersLock) {
			return senders.firstOrNull { it.name == id }
		}

	private fun closeSender(sender: Sender?) =
		try {
			sender?.close()
		} catch (closeException: Exception) {
			// Couldn't close it? Who cares ...
		}

	val senderCount: Int
		get() = synchronized(sendersLock) {
			senders.size
		}
}