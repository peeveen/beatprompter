package com.stevenfrew.beatprompter

import android.util.Log

abstract class Task(private var running: Boolean) : Runnable {
	private var stop = false
	private val runningSync = Any()
	private val stopSync = Any()
	private var isRunning: Boolean
		get() = synchronized(runningSync) {
			return running
		}
		set(value) = synchronized(runningSync) {
			running = value
		}
	protected val shouldStop: Boolean
		get() = synchronized(stopSync) {
			return stop
		}

	private fun setShouldStop() =
		synchronized(stopSync) {
			stop = true
		}

	override fun run() {
		Log.d(TASK_TAG, "Task initialising.")
		initialise()
		Log.d(TASK_TAG, "Task starting.")
		while (!shouldStop) {
			if (isRunning) {
				doWork()
			} else {
				try {
					Thread.sleep(500)
				} catch (ie: InterruptedException) {
					Log.d(TASK_TAG, "Thread sleep (while paused) was interrupted.", ie)
				}
			}
		}
		Log.d(TASK_TAG, "Task ended.")
	}

	private fun pause(): Boolean {
		val wasRunning = isRunning
		isRunning = false
		return wasRunning
	}

	private fun resume(): Boolean {
		val wasRunning = isRunning
		isRunning = true
		return !wasRunning
	}

	open fun stop(): Boolean {
		val wasRunningAndNotStopping = isRunning && !shouldStop
		isRunning = false
		setShouldStop()
		return wasRunningAndNotStopping
	}

	protected open fun initialise() {}
	abstract fun doWork()

	companion object {
		private const val TASK_TAG = "task"

		private fun changeTaskState(task: Task?, thread: Thread?, fn: (Task) -> Boolean): Boolean =
			if (task != null) {
				if (fn(task)) {
					if (thread != null) {
						thread.interrupt()
						true
					} else false
				} else false
			} else false

		fun pauseTask(task: Task?, thread: Thread?) = changeTaskState(task, thread) { it.pause() }
		fun resumeTask(task: Task?, thread: Thread?) = changeTaskState(task, thread) { it.resume() }

		fun stopTask(task: Task?, thread: Thread?) {
			if (changeTaskState(task, thread) { it.stop() })
				try {
					thread?.join()
				} catch (ie: InterruptedException) {
					Log.d(TASK_TAG, "Task interrupted while waiting for join.", ie)
				}
		}
	}
}