package com.stevenfrew.beatprompter.comm

import android.os.Looper
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import java.util.Calendar
import java.util.Date

object ConnectionNotificationTask : Task(true) {
	private val connections: MutableList<ConnectionDescriptor> = mutableListOf()
	private val disconnections: MutableList<ConnectionDescriptor> = mutableListOf()
	private val recentConnections = mutableMapOf<ConnectionDescriptor, Date>()
	private val recentDisconnections = mutableMapOf<ConnectionDescriptor, Date>()

	private fun hasHappenedInThePastSecond(
		descriptor: ConnectionDescriptor,
		recentActions: Map<ConnectionDescriptor, Date>
	): Boolean =
		synchronized(recentActions) {
			val lastNotificationDate = recentActions[descriptor]
			return lastNotificationDate?.let {
				val calendar = Calendar.getInstance()
				calendar.add(Calendar.SECOND, -1)
				val oneSecondAgo = calendar.getTime()
				it >= oneSecondAgo
			} == true
		}

	fun addConnection(connection: ConnectionDescriptor) =
		synchronized(connections) {
			val date = Date()
			if (!hasHappenedInThePastSecond(connection, recentConnections))
				connections.add(connection)
			synchronized(recentConnections) {
				recentConnections[connection] = date
			}
		}

	fun addDisconnection(disconnection: ConnectionDescriptor) =
		synchronized(disconnection) {
			val date = Date()
			if (!hasHappenedInThePastSecond(disconnection, recentDisconnections))
				disconnections.add(disconnection)
			synchronized(recentDisconnections) {
				recentDisconnections[disconnection] = date
			}
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

