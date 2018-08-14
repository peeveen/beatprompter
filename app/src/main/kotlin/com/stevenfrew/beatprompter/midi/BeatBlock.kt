package com.stevenfrew.beatprompter.midi

data class BeatBlock(var blockStartTime: Long, var midiBeatCount: Int, var nanoPerBeat: Double)
