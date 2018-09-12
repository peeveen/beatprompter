package com.stevenfrew.beatprompter.comm

interface Sender:Communicator {
    suspend fun send(messages:List<OutgoingMessage>)
    val lock:Any
}