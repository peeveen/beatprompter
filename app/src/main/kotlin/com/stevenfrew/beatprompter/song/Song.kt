package com.stevenfrew.beatprompter.song

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.ScreenComment
import com.stevenfrew.beatprompter.graphics.ScreenString
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.song.event.AudioEvent
import com.stevenfrew.beatprompter.song.event.LineEvent
import com.stevenfrew.beatprompter.song.event.LinkedEvent
import com.stevenfrew.beatprompter.song.line.Line
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.splitAndTrim
import java.util.UUID

class Song(
	val songFile: SongFile,
	val displaySettings: DisplaySettings,
	firstEvent: LinkedEvent,
	private val lines: List<Line>,
	internal val audioEvents: List<AudioEvent>,
	val initialMidiMessages: List<MidiMessage>,
	private val beatBlocks: List<BeatBlock>,
	val sendMidiClock: Boolean,
	val startScreenStrings: List<ScreenString>,
	val nextSongString: ScreenString?,
	val totalStartScreenTextHeight: Int,
	val wasStartedByBandLeader: Boolean,
	val nextSong: String,
	val displayOffset: Int,
	val height: Int,
	val scrollEndPixel: Int,
	val noScrollLines: List<Line>,
	val beatCounterRect: Rect,
	val songTitleHeader: ScreenString,
	val songTitleHeaderLocation: PointF,
	val loadId: UUID,
	private val audioLatency: Int
) {
	internal var currentLine: Line = lines.first()
	internal var currentEvent = firstEvent // Last event that executed.
	private var nextEvent: LinkedEvent? = firstEvent.nextEvent // Upcoming event.
	private val numberOfMidiBeatBlocks = beatBlocks.size

	var cancelled = false
	val smoothMode: Boolean =
		lines.asSequence().filter { it.scrollMode === ScrollingMode.Smooth }.any()
	val manualMode: Boolean = lines.all { it.scrollMode === ScrollingMode.Manual }
	internal val backingTrack = findBackingTrack(firstEvent)

	private fun getProgressLineEvent(event: LinkedEvent): LineEvent? {
		var nextEvent: LinkedEvent? = event
		val latencyCompensatedEventTime = event.time + Utils.milliToNano(audioLatency)
		// Look at events where the time is the SAME as the progress event, or
		// the same with audio latency compensation.
		while (nextEvent != null) {
			if (nextEvent.event is LineEvent)
				if (nextEvent.time == latencyCompensatedEventTime) // This is the line
					return nextEvent.event as LineEvent
				else // Found a line event with a daft time
					break
			nextEvent = nextEvent.nextEvent
		}
		// Nothing else for it, use the previous line
		return event.previousLineEvent
	}

	internal fun setProgress(nano: Long) {
		val e = currentEvent
		val newCurrentEvent = e.findLatestEventOnOrBefore(nano)
		currentEvent = newCurrentEvent
		nextEvent = currentEvent.nextEvent
		// Annoyingly, mPrevLineEvent is sometimes the line event
		// BEFORE the current one ... this happens if the progress
		// event is an Audio or Midi event, which are placed higher
		// in the priority queue.
		val lineEvent = getProgressLineEvent(newCurrentEvent)
		currentLine = lineEvent?.line ?: lines.first()
	}

	internal fun getNextEvent(time: Long): LinkedEvent? =
		if (nextEvent != null && nextEvent!!.time <= time) {
			currentEvent = nextEvent!!
			nextEvent = nextEvent!!.nextEvent
			currentEvent
		} else null

	internal fun getTimeFromPixel(pixel: Int): Long =
		if (pixel == 0) 0 else currentLine.getTimeFromPixel(pixel)

	internal fun getPixelFromTime(time: Long): Int =
		if (time == 0L) 0 else currentLine.getPixelFromTime(time)

	internal fun recycleGraphics() =
		lines.forEach {
			it.recycleGraphics()
		}

	internal fun getMIDIBeatTime(beat: Int): Long {
		repeat(numberOfMidiBeatBlocks) {
			val (blockStartTime, midiBeatCount, nanoPerBeat) = beatBlocks[it]
			if (midiBeatCount <= beat && (it + 1 == numberOfMidiBeatBlocks || beatBlocks[it + 1].midiBeatCount > beat))
				return (blockStartTime + nanoPerBeat * (beat - midiBeatCount)).toLong()
		}
		return 0
	}

	class Comment internal constructor(
		var mText: String,
		audience: List<String>,
		screenSize: Rect,
		paint: Paint,
		font: Typeface
	) {
		private val commentAudience = audience
		private val commentGraphic = ScreenComment(mText, screenSize, paint, font)

		fun isIntendedFor(audience: String): Boolean =
			commentAudience.isEmpty() ||
				audience.isBlank() ||
				audience.lowercase().splitAndTrim(",").intersect(commentAudience.toSet()).any()

		fun draw(canvas: Canvas, paint: Paint, textColor: Int) =
			commentGraphic.draw(canvas, paint, textColor)
	}

	companion object {
		private fun findBackingTrack(firstEvent: LinkedEvent): AudioFile? {
			// Find the backing track (if any)
			var thisEvent: LinkedEvent? = firstEvent
			while (thisEvent != null) {
				val innerEvent = thisEvent.event
				if (innerEvent is AudioEvent && innerEvent.isBackingTrack)
					return innerEvent.audioFile
				thisEvent = thisEvent.nextEvent
			}
			return null
		}
	}
}
