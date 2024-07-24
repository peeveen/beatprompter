package com.stevenfrew.beatprompter.comm

abstract class ReceiverBase(
	override val name: String,
	override val type: CommunicationType,
	private val bufferSize: Int = IN_BUFFER_SIZE
) : Receiver {
	private val inBuffer = ByteArray(bufferSize)
	private var amountOfDataInBuffer = 0

	override fun receive() {
		while (true) {
			val amountReceived =
				receiveMessageData(inBuffer, amountOfDataInBuffer, bufferSize - amountOfDataInBuffer)
			if (amountReceived <= 0)
				break
			amountOfDataInBuffer += amountReceived
			val amountOfDataParsed = parseMessageData(inBuffer, amountOfDataInBuffer)
			amountOfDataInBuffer -= amountOfDataParsed
			System.arraycopy(inBuffer, amountOfDataParsed, inBuffer, 0, amountOfDataInBuffer)
		}
	}

	protected abstract fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int

	protected abstract fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int

	companion object {
		// 4K buffer by default.
		private const val IN_BUFFER_SIZE = 4096
	}
}