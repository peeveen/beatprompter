package com.stevenfrew.beatprompter.songload

class CancelEvent {
    var isCancelled = false
        private set

    fun set() {
        isCancelled = true
    }
}
