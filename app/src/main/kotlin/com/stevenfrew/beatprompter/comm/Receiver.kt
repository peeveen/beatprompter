package com.stevenfrew.beatprompter.comm

interface Receiver {
    val name:String
    fun receive()
    fun close()
}