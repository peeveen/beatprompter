package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.midi.message.ClockMessage

class MIDIMessageQueue(capacity: Int) : MessageQueue(capacity) {
	internal fun addBeatClockMessages(amount: Int) {
		synchronized(mBlockingQueue)
		{
			repeat(amount) {
				mBlockingQueue.put(ClockMessage)
			}
		}
	}
}