package com.stevenfrew.beatprompter.comm

import android.os.Looper
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

object ConnectionNotificationTask : Task(true) {
	private val connections: MutableList<ConnectionDescriptor> = mutableListOf()
	private val disconnections: MutableList<ConnectionDescriptor> = mutableListOf()

	fun addConnection(connection: ConnectionDescriptor) =
		synchronized(connections) {
			connections.add(connection)
		}

	fun addDisconnection(disconnection: ConnectionDescriptor) =
		synchronized(disconnection) {
			disconnections.add(disconnection)
		}

	private fun report(
		event: Int,
		notifications: MutableList<ConnectionDescriptor>
	) = synchronized(notifications) {
		for (f in notifications.size - 1 downTo 0) {
			if (EventRouter.sendEventToSongList(event, notifications[f].toString()))
				notifications.removeAt(f)
		}
	}

	override fun doWork() {
		Looper.prepare()
		while (true) {
			Thread.sleep(1000)
			report(Events.CONNECTION_ADDED, connections)
			report(Events.CONNECTION_LOST, disconnections)
		}
	}
}

