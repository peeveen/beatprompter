package com.stevenfrew.beatprompter.comm

open class OutgoingMessage(val bytes: ByteArray) {
	val length: Int
		get() = bytes.size

	override fun toString(): String =
		StringBuilder().apply {
			bytes.forEach { append(String.format("%02X ", it)) }
		}.toString().trim()
}