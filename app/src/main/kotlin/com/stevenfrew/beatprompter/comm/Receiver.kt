package com.stevenfrew.beatprompter.comm

interface Receiver<TMessageType> {
    fun receive():List<TMessageType>
    fun close()
}