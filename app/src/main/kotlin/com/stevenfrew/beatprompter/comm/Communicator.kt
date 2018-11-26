package com.stevenfrew.beatprompter.comm

interface Communicator {
    fun close()
    val name: String
}