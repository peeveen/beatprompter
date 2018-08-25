package com.stevenfrew.beatprompter.songload

class SongLoadCancelEvent {
    var isCancelled = false
        private set

    fun set() {
        isCancelled = true
    }
}
