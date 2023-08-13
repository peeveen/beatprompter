package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task

class SenderTask(private val mMessageQueue: MessageQueue) : Task(false) {
	private val mSenders = mutableListOf<Sender>()
	private val mSendersLock = Any()

	override fun doWork() {
		try {
			// This take() will block if the queue is empty
			val messages = mMessageQueue.getMessages()

			synchronized(mSendersLock)
			{
				for (f in mSenders.size - 1 downTo 0) {
					// Sanity check in case a dead sender was removed.
					if (f < mSenders.size) {
						try {
							mSenders[f].send(messages)
						} catch (commException: Exception) {
							// Problem with the I/O? This sender is now dead to us.
							Logger.logComms("Sender threw an exception. Assuming it to be dead.")
							removeSender(mSenders[f].name)
						}
					}
				}
			}
		} catch (interruptedException: InterruptedException) {
			// Must have been signalled to stop ... main Task loop will cater for this.
		}
	}

	fun addSender(id: String, sender: Sender) {
		synchronized(mSendersLock)
		{
			Logger.logComms { "Adding new sender '$id' (${sender.name}) to the collection" }
			mSenders.add(sender)
		}
	}

	fun removeSender(id: String) {
		getSender(id)?.also { sender ->
			Logger.logComms { "Removing sender '$id' from the collection" }
			closeSender(sender)
			Logger.logComms { "Sender '$id' has been closed." }
			synchronized(mSendersLock)
			{
				mSenders.removeAll { it.name == id }
			}
			Logger.logComms { "Sender '$id' is now dead ... notifying main activity for UI." }
			EventRouter.sendEventToSongList(Events.CONNECTION_LOST, sender.name)
		}
	}

	fun removeAll() {
		Logger.logComms("Removing ALL senders from the collection.")
		synchronized(mSendersLock)
		{
			// Avoid concurrent modification exception by converting to array.
			val senderArray = mSenders.toTypedArray()
			senderArray.forEach { removeSender(it.name) }
		}
	}

	private fun getSender(id: String): Sender? {
		synchronized(mSendersLock)
		{
			return mSenders.firstOrNull { it.name == id }
		}
	}

	private fun closeSender(sender: Sender?) {
		try {
			sender?.close()
		} catch (closeException: Exception) {
			// Couldn't close it? Who cares ...
		}
	}

	val senderCount: Int
		get() = synchronized(mSendersLock)
		{
			mSenders.size
		}
}