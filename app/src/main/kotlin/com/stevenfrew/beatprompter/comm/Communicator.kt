package com.stevenfrew.beatprompter.comm

interface Communicator {
    val name:String
    fun close()
}