package com.stevenfrew.beatprompter.comm.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.and

class UsbReceiver(
	private val connection: UsbDeviceConnection,
	private val endpoint: UsbEndpoint,
	name: String,
	type: CommunicationType
) : Receiver(name, type), CoroutineScope {
	private var innerBuffer = ByteArray(INITIAL_INNER_BUFFER_SIZE)

	override val coroutineContext: CoroutineContext
		get() = Dispatchers.IO
	private var closed = false

	init {
		// Read all incoming data until there is none left. Basically, clear anything that
		// has been buffered before we accepted the connection.
		// On the off-chance that there is a never-ending barrage of data coming in at a ridiculous
		// speed, let's say 1K of data, max, to prevent lockup.
		launch {
			var bufferClear = 0
			var dataRead: Int
			val wasteBuffer = ByteArray(endpoint.maxPacketSize)
			do {
				dataRead = connection.bulkTransfer(
					endpoint,
					wasteBuffer, wasteBuffer.size, 1000
				)
				if (dataRead > 0)
					bufferClear += dataRead
			} while (dataRead > 0 && bufferClear < 1024)
		}
	}

	override fun close() =
		try {
			connection.close()
		} finally {
			closed = true
		}

	override fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int {
		while (innerBuffer.size < dataEnd)
			innerBuffer = ByteArray(innerBuffer.size + INNER_BUFFER_GROW_SIZE)
		var positions = 0 to 0 // first=read position, second=write position
		while (positions.first < dataEnd)
			positions =
				parseUsbMidiMessages(buffer, innerBuffer, positions.first, positions.second, dataEnd)
		super.parseMessageData(innerBuffer, positions.second)
		return positions.first
	}

	override fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int {
		val maxRead = maximumAmount - offset
		val newArray =
			if (offset == 0)
				buffer
			else
				ByteArray(maxRead)
		return connection.bulkTransfer(
			endpoint,
			newArray,
			maxRead,
			500
		).also {
			if (it > 0) {
				if (offset != 0)
					System.arraycopy(newArray, 0, buffer, offset, it)
			} else if (it == -1)
				if (closed)
					throw Exception("Cannot read data, USB connection is closed.")
		}
	}

	override fun unregister(task: ReceiverTask) = Midi.removeReceiver(task)

	companion object {
		private const val INITIAL_INNER_BUFFER_SIZE = 4096
		private const val INNER_BUFFER_GROW_SIZE = 2048

		// Using zero to indicate "unknown/future expansion" message lengths.
		private val CODE_INDEX_MESSAGE_LENGTH_LOOKUP =
			arrayOf(0, 0, 2, 3, 3, 1, 2, 3, 3, 3, 3, 3, 2, 2, 3, 1)

		private fun parseUsbMidiMessages(
			readBuffer: ByteArray,
			writeBuffer: ByteArray,
			readStart: Int,
			writeStart: Int,
			readLimit: Int
		): Pair<Int, Int> {
			var currentReadPosition = readStart
			var currentWritePosition = writeStart
			while (currentReadPosition < readLimit) {
				val codeIndex = readBuffer[currentReadPosition] and 0x0F
				val messageLength = CODE_INDEX_MESSAGE_LENGTH_LOOKUP[codeIndex.toInt()]
				if (messageLength != 0) {
					System.arraycopy(
						readBuffer,
						currentReadPosition + 1,
						writeBuffer,
						currentWritePosition,
						messageLength
					)
					currentWritePosition += messageLength
				}
				currentReadPosition += 4
			}
			return currentReadPosition to currentWritePosition
		}
	}
}