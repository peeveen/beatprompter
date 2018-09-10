package com.stevenfrew.beatprompter.comm

interface Sender {
    suspend fun send(messages:List<OutgoingMessage>)
    fun close()
}