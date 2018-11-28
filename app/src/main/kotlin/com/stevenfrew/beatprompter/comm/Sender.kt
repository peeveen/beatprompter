package com.stevenfrew.beatprompter.comm

interface Sender : Communicator {
    fun send(messages: List<OutgoingMessage>)
    val lock: Any
}