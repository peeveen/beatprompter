package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

open class MessageQueue(capacity: Int) {
	protected val blockingQueue = ArrayBlockingQueue<Message>(capacity)

	// This prevents over-allocation of objects, which creates very slow garbage collection, which
	// disrupts timing-critical operations.
	private val outBuffer = mutableListOf<Message>()

	internal fun getMessages(): List<Message> =
		outBuffer.apply {
			clear()
			// This take() will cause a block if empty
			add(blockingQueue.take())
			synchronized(blockingQueue) {
				while (blockingQueue.isNotEmpty())
					add(blockingQueue.remove())
			}
		}

	internal fun putMessage(message: Message) =
		synchronized(blockingQueue) {
			blockingQueue.put(message)
		}

	internal fun putMessages(messages: List<Message>) =
		synchronized(blockingQueue) {
			for (f in messages.indices)
				blockingQueue.put(messages[f])
		}
}