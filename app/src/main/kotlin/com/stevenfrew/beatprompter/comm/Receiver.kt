package com.stevenfrew.beatprompter.comm

interface Receiver:Communicator {
    suspend fun receive()
}