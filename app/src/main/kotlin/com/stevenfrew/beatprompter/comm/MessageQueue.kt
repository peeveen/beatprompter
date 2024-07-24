package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

open class MessageQueue(capacity: Int) {
	protected val blockingQueue = ArrayBlockingQueue<OutgoingMessage>(capacity)

	// This prevents over-allocation of objects, which creates very slow garbage collection, which
	// disrupts timing-critical operations.
	private val outBuffer = mutableListOf<OutgoingMessage>()

	internal fun getMessages(): List<OutgoingMessage> {
		outBuffer.clear()
		// This take() will cause a block if empty
		outBuffer.add(blockingQueue.take())
		synchronized(blockingQueue) {
			while (blockingQueue.isNotEmpty())
				outBuffer.add(blockingQueue.remove())
			return outBuffer
		}
	}

	internal fun putMessage(message: OutgoingMessage) =
		synchronized(blockingQueue) {
			blockingQueue.put(message)
		}

	internal fun putMessages(messages: List<OutgoingMessage>) =
		synchronized(blockingQueue) {
			for (f in messages.indices)
				blockingQueue.put(messages[f])
		}
}