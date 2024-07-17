package com.stevenfrew.beatprompter.comm

open class OutgoingMessage(val mBytes: ByteArray) {
	val length: Int
		get() = mBytes.size

	override fun toString(): String =
		StringBuilder().apply {
			mBytes.forEach { append(String.format("%02X ", it)) }
		}.toString().trim()
}