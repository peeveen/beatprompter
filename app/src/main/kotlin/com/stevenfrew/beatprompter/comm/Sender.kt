package com.stevenfrew.beatprompter.comm

interface Sender : Communicator {
    fun send(message: OutgoingMessage)
}