package com.stevenfrew.beatprompter.comm.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverTask
import kotlin.math.max
import kotlin.math.min

class NativeReceiver(
	// Annoyingly, if we don't keep a hold of the MIDI device reference for Bluetooth MIDI, then
	// it automatically closes. So I'm storing it here.
	private val device: MidiDevice,
	private val port: MidiOutputPort,
	name: String,
	type: CommunicationType
) : Receiver(name, type) {
	private val innerReceiver = NativeReceiverReceiver()
	private val innerBufferLock = Any()
	private var innerBuffer = ByteArray(INITIAL_INNER_BUFFER_SIZE)
	private var innerBufferPosition = 0
	private var closed: Boolean = false

	init {
		port.connect(innerReceiver)
	}

	override fun close() =
		try {
			port.disconnect(innerReceiver)
			port.close()
		} finally {
			closed = true
		}

	override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int =
		synchronized(innerBufferLock) {
			return min(maximumAmount, innerBufferPosition).also {
				if (it != 0) {
					System.arraycopy(innerBuffer, 0, buffer, offset, it)
					innerBufferPosition -= it
				} else if (closed)
					throw Exception("Cannot read data, MIDI connection is closed.")
			}
		}

	override fun unregister(task: ReceiverTask) = Midi.removeReceiver(task)

	inner class NativeReceiverReceiver : MidiReceiver() {
		override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
			if (msg != null) {
				synchronized(innerBufferLock) {
					// If we exceed the available space, we have to increase space.
					// There is no second-chance to get this data.
					if (innerBufferPosition + count > innerBuffer.size) {
						val biggerBufferSize =
							max(innerBuffer.size + INNER_BUFFER_GROW_SIZE, innerBufferPosition + count)
						val biggerBuffer = ByteArray(biggerBufferSize)
						System.arraycopy(innerBuffer, 0, biggerBuffer, 0, innerBuffer.size)
						innerBuffer = biggerBuffer
					}
					System.arraycopy(msg, offset, innerBuffer, innerBufferPosition, count)
					innerBufferPosition += count
				}
			}
		}
	}

	companion object {
		private const val INITIAL_INNER_BUFFER_SIZE = 4096
		private const val INNER_BUFFER_GROW_SIZE = 2048
	}
}