package com.stevenfrew.beatprompter.comm

abstract class SenderBase(
	override val name: String,
	override val type: CommunicationType,
	override val messageType: MessageType,
	private val bufferSize: Int = OUT_BUFFER_SIZE
) : Sender {
	private val outBuffer = ByteArray(bufferSize)

	override fun send(messages: List<Message>) {
		val convertedMessages = messages.map { convertMessage(it) }
		var currentMessageIndex = 0
		while (currentMessageIndex < convertedMessages.size) {
			var bytesCopied = 0
			// Copy messages to out-buffer
			// We might have more than 4K of data to send here, so might need to loop.
			while (currentMessageIndex < convertedMessages.size) {
				val messageSize = convertedMessages[currentMessageIndex].length
				if (bytesCopied + messageSize > bufferSize)
					break
				System.arraycopy(
					convertedMessages[currentMessageIndex].bytes,
					0, outBuffer, bytesCopied, messageSize
				)
				bytesCopied += messageSize
				++currentMessageIndex
			}
			sendMessageData(outBuffer, bytesCopied)
		}
	}

	protected open fun convertMessage(message: Message): Message = message

	protected abstract fun sendMessageData(bytes: ByteArray, length: Int)

	companion object {
		// 4K buffer by default.
		private const val OUT_BUFFER_SIZE = 4096
	}
}