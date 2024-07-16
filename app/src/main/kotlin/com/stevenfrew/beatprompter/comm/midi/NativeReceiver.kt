package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import com.stevenfrew.beatprompter.comm.ReceiverTask
import kotlin.math.max
import kotlin.math.min

class NativeReceiver(
	private val mPort: MidiOutputPort,
	name: String
) : Receiver(name) {
	private val mInnerReceiver = NativeReceiverReceiver()
	private val mInnerBufferLock = Any()
	private var mInnerBuffer = ByteArray(INITIAL_INNER_BUFFER_SIZE)
	private var mInnerBufferPosition = 0
	private var mClosed: Boolean = false

	init {
		mPort.connect(mInnerReceiver)
	}

	override fun close() {
		try {
			mPort.disconnect(mInnerReceiver)
			mPort.close()
		} finally {
			mClosed = true
		}
	}

	override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
		synchronized(mInnerBufferLock) {
			return min(maximumAmount, mInnerBufferPosition).also {
				if (it != 0) {
					System.arraycopy(mInnerBuffer, 0, buffer, offset, it)
					mInnerBufferPosition -= it
				} else {
					if (mClosed)
						throw Exception("Cannot read data, MIDI connection is closed.")
				}
			}
		}
	}

	override fun unregister(task: ReceiverTask) {
		Midi.removeReceiver(task)
	}

	inner class NativeReceiverReceiver : MidiReceiver() {
		override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
			if (msg != null) {
				synchronized(mInnerBufferLock) {
					// If we exceed the available space, we have to increase space.
					// There is no second-chance to get this data.
					if (mInnerBufferPosition + count > mInnerBuffer.size) {
						val biggerBufferSize =
							max(mInnerBuffer.size + INNER_BUFFER_GROW_SIZE, mInnerBufferPosition + count)
						val biggerBuffer = ByteArray(biggerBufferSize)
						System.arraycopy(mInnerBuffer, 0, biggerBuffer, 0, mInnerBuffer.size)
						mInnerBuffer = biggerBuffer
					}
					System.arraycopy(msg, offset, mInnerBuffer, mInnerBufferPosition, count)
					mInnerBufferPosition += count
				}
			}
		}
	}

	companion object {
		private const val INITIAL_INNER_BUFFER_SIZE = 4096
		private const val INNER_BUFFER_GROW_SIZE = 2048
	}
}