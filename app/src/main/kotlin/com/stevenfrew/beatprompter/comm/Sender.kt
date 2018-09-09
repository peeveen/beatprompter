package com.stevenfrew.beatprompter.comm

interface Sender<TMessageType> where TMessageType:Message {
    suspend fun send(messages:List<TMessageType>)
    fun close()
}