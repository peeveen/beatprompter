package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

open class MessageQueue<T>(capacity: Int) where T : Message {
	protected val blockingQueue = ArrayBlockingQueue<T>(capacity)

	// This prevents over-allocation of objects, which creates very slow garbage collection, which
	// disrupts timing-critical operations.
	private val outBuffer = mutableListOf<T>()

	internal fun getMessages(): List<T> =
		outBuffer.apply {
			clear()
			// This take() will cause a block if empty
			add(blockingQueue.take())
			synchronized(blockingQueue) {
				while (blockingQueue.isNotEmpty())
					add(blockingQueue.remove())
			}
		}

	open fun shouldPutMessage(message: T): Boolean = true

	internal fun putMessage(message: T) =
		synchronized(blockingQueue) {
			if (shouldPutMessage(message))
				blockingQueue.put(message)
		}

	internal fun putMessages(messages: List<T>) =
		synchronized(blockingQueue) {
			for (f in messages.indices) {
				val msg = messages[f]
				if (shouldPutMessage(msg))
					blockingQueue.put(msg)
			}
		}
}