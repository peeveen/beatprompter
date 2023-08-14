package com.stevenfrew.beatprompter

import android.util.Log

abstract class Task(private var mRunning: Boolean) : Runnable {
	private var mStop = false
	private val runningSync = Any()
	private val stopSync = Any()
	private var isRunning: Boolean
		get() = synchronized(runningSync) {
			return mRunning
		}
		set(value) = synchronized(runningSync) {
			mRunning = value
		}
	protected val shouldStop: Boolean
		get() = synchronized(stopSync) {
			return mStop
		}

	private fun setShouldStop() {
		synchronized(stopSync) {
			mStop = true
		}
	}

	override fun run() {
		Log.d(TASKTAG, "Task initialising.")
		initialise()
		Log.d(TASKTAG, "Task starting.")
		while (!shouldStop) {
			if (isRunning) {
				doWork()
			} else {
				try {
					Thread.sleep(500)
				} catch (ie: InterruptedException) {
					Log.d(TASKTAG, "Thread sleep (while paused) was interrupted.", ie)
				}
			}
		}
		Log.d(TASKTAG, "Task ended.")
	}

	open fun stop() {
		isRunning = false
		setShouldStop()
	}

	protected open fun initialise() {}
	private fun pause() {
		isRunning = false
	}

	private fun resume() {
		isRunning = true
	}

	abstract fun doWork()

	companion object {

		private const val TASKTAG = "task"

		fun pauseTask(task: Task?, thread: Thread?) {
			if (task != null) {
				task.pause()
				thread?.interrupt()
			}
		}

		fun resumeTask(task: Task?) {
			task?.resume()
		}

		fun stopTask(task: Task?, thread: Thread?) {
			if (task != null) {
				task.stop()
				if (thread != null) {
					thread.interrupt()
					try {
						thread.join()
					} catch (ie: InterruptedException) {
						Log.d(TASKTAG, "Task interrupted while waiting for join.", ie)
					}
				}
			}
		}
	}
}