package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.midi.message.ClockMessage

class MidiMessageQueue(capacity: Int) : MessageQueue(capacity) {
	internal fun addBeatClockMessages(amount: Int) =
		synchronized(blockingQueue)
		{
			repeat(amount) {
				blockingQueue.put(ClockMessage)
			}
		}
}
