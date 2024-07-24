package com.stevenfrew.beatprompter.comm

abstract class ReceiverBase(
	override val name: String,
	override val type: String,
	private val mBufferSize: Int = IN_BUFFER_SIZE
) : Receiver {
	private val mInBuffer = ByteArray(mBufferSize)
	private var mAmountOfDataInBuffer = 0

	override fun receive() {
		while (true) {
			val amountReceived =
				receiveMessageData(mInBuffer, mAmountOfDataInBuffer, mBufferSize - mAmountOfDataInBuffer)
			if (amountReceived <= 0)
				break
			mAmountOfDataInBuffer += amountReceived
			val amountOfDataParsed = parseMessageData(mInBuffer, mAmountOfDataInBuffer)
			mAmountOfDataInBuffer -= amountOfDataParsed
			System.arraycopy(mInBuffer, amountOfDataParsed, mInBuffer, 0, mAmountOfDataInBuffer)
		}
	}

	protected abstract fun receiveMessageData(buffer: ByteArray, offset: Int, maximumAmount: Int): Int

	protected abstract fun parseMessageData(buffer: ByteArray, dataEnd: Int): Int

	companion object {
		// 4K buffer by default.
		private const val IN_BUFFER_SIZE = 4096
	}
}