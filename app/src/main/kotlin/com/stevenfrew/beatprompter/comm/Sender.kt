package com.stevenfrew.beatprompter.comm

interface Sender<T> : Communicator where T: Message {
	fun send(messages: List<T>)
	val messageType: MessageType
}