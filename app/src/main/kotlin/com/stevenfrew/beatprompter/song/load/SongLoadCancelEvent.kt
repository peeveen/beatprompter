package com.stevenfrew.beatprompter.song.load

class SongLoadCancelEvent {
    var isCancelled = false
        private set

    fun set() {
        isCancelled = true
    }
}
