package com.stevenfrew.beatprompter.comm

interface Sender : Communicator {
	fun send(messages: List<Message>)
	val messageType: MessageType
}