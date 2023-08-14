package com.stevenfrew.beatprompter.comm

open class OutgoingMessage(val mBytes: ByteArray) {
	val length: Int
		get() = mBytes.size

	override fun toString(): String {
		return StringBuilder().apply {
			for (mMessageByte in mBytes)
				append(String.format("%02X ", mMessageByte))
		}.toString().trim()
	}
}