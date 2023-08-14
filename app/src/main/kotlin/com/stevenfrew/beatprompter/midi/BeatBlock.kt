package com.stevenfrew.beatprompter.midi

data class BeatBlock(
	val blockStartTime: Long,
	val midiBeatCount: Int,
	val nanoPerBeat: Double
)
