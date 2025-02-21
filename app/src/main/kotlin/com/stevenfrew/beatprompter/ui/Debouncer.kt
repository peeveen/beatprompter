package com.stevenfrew.beatprompter.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Nicked from https://github.com/MilindAmrutkar/KotlinDebouncer
class Debouncer(private val scope: CoroutineScope) {
	// A job represents a cancellable piece of work. In this case, the work is the debounced
	// action that will be executed after a certain delay.
	// The job is cancelled if a new event comes in before the delay period ends.
	private var debounceJob: Job? = null


	/**
	 * Delays the execution of the specified [action] by the [interval].
	 *
	 * If this method is called again before the [interval] is over, the previous [action] is
	 * cancelled and the delay resets for the latest invocation of the method.
	 *
	 * @param interval The time in milliseconds to wait before executing the [action].
	 * @param action The action to execute after the delay.
	 */
	fun debounce(interval: Long, action: () -> Unit) {
		debounceJob?.cancel() // Cancel the previous job if it hasn't completed yet.
		debounceJob = scope.launch {// launch a coroutine in the provided scope.
			delay(interval) // Suspend the coroutine for the specified interval.
			action() // Execute the provided action.
		}
	}
}