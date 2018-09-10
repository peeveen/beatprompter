package com.stevenfrew.beatprompter.comm

interface Receiver {
    fun receive()
    fun close()
}