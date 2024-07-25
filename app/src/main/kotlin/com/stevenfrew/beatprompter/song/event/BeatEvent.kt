package com.stevenfrew.beatprompter.song.event

/**
 * A BeatEvent signals the event processor to update the beat counter display, and optionally play a
 * metronome click sound (this will actually just generate a ClickEvent to represent the sound).
 */
class BeatEvent(
	eventTime: Long,
	val bpm: Double,
	var bpb: Int,
	val beat: Int,
	val click: Boolean,
	val willScrollOnBeat: Int
) : BaseEvent(eventTime) {
	override fun offset(nanoseconds: Long): BaseEvent =
		BeatEvent(eventTime + nanoseconds, bpm, bpb, beat, click, willScrollOnBeat)
}