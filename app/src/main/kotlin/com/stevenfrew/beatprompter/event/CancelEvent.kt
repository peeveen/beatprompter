package com.stevenfrew.beatprompter.event

class CancelEvent {
    var isCancelled = false
        private set

    fun set() {
        isCancelled = true
    }
}
