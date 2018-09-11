package com.stevenfrew.beatprompter.comm

interface Sender {
    val name:String
    suspend fun send(messages:List<OutgoingMessage>)
    fun close()
}