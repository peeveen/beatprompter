package com.stevenfrew.beatprompter.comm

interface Receiver : Communicator {
	fun receive()
	fun unregister(task: ReceiverTask)
}