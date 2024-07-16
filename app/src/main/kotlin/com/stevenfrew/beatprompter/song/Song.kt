package com.stevenfrew.beatprompter.song

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
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
	val mSongFile: SongFile,
	val mDisplaySettings: DisplaySettings,
	firstEvent: LinkedEvent,
	private val mLines: List<Line>,
	internal val mAudioEvents: List<AudioEvent>,
	val mInitialMIDIMessages: List<OutgoingMessage>,
	private val mBeatBlocks: List<BeatBlock>,
	val mSendMIDIClock: Boolean,
	val mStartScreenStrings: List<ScreenString>,
	val mNextSongString: ScreenString?,
	val mTotalStartScreenTextHeight: Int,
	val mStartedByBandLeader: Boolean,
	val mNextSong: String,
	val mDisplayOffset: Int,
	val mHeight: Int,
	val mScrollEndPixel: Int,
	val mNoScrollLines: List<Line>,
	val mBeatCounterRect: Rect,
	val mSongTitleHeader: ScreenString,
	val mSongTitleHeaderLocation: PointF,
	val mLoadID: UUID
) {
	internal var mCurrentLine: Line = mLines.first()
	internal var mCurrentEvent = firstEvent // Last event that executed.
	private var mNextEvent: LinkedEvent? = firstEvent.mNextEvent // Upcoming event.
	var mCancelled = false
	private val mNumberOfMIDIBeatBlocks = mBeatBlocks.size
	val mSmoothMode: Boolean =
		mLines.asSequence().filter { it.mScrollMode === ScrollingMode.Smooth }.any()
	val mManualMode: Boolean = mLines.all { it.mScrollMode === ScrollingMode.Manual }
	internal val mBackingTrack = findBackingTrack(firstEvent)

	private fun getProgressLineEvent(event: LinkedEvent): LineEvent? {
		var nextEvent: LinkedEvent? = event
		val latencyCompensatedEventTime = event.time + Utils.milliToNano(Preferences.audioLatency)
		// Look at events where the time is the SAME as the progress event, or
		// the same with audio latency compensation.
		while (nextEvent != null) {
			if (nextEvent.mEvent is LineEvent)
				if (nextEvent.time == latencyCompensatedEventTime) // This is the line
					return nextEvent.mEvent as LineEvent
				else // Found a line event with a daft time
					break
			nextEvent = nextEvent.mNextEvent
		}
		// Nothing else for it, use the previous line
		return event.mPrevLineEvent
	}

	internal fun setProgress(nano: Long) {
		val e = mCurrentEvent
		val newCurrentEvent = e.findLatestEventOnOrBefore(nano)
		mCurrentEvent = newCurrentEvent
		mNextEvent = mCurrentEvent.mNextEvent
		// Annoyingly, mPrevLineEvent is sometimes the line event
		// BEFORE the current one ... this happens if the progress
		// event is an Audio or Midi event, which are placed higher
		// in the priority queue.
		val lineEvent = getProgressLineEvent(newCurrentEvent)
		mCurrentLine = lineEvent?.mLine ?: mLines.first()
	}

	internal fun getNextEvent(time: Long): LinkedEvent? {
		if (mNextEvent != null && mNextEvent!!.time <= time) {
			mCurrentEvent = mNextEvent!!
			mNextEvent = mNextEvent!!.mNextEvent
			return mCurrentEvent
		}
		return null
	}

	internal fun getTimeFromPixel(pixel: Int): Long {
		if (pixel == 0)
			return 0
		return mCurrentLine.getTimeFromPixel(pixel)
	}

	internal fun getPixelFromTime(time: Long): Int {
		if (time == 0L)
			return 0
		return mCurrentLine.getPixelFromTime(time)
	}

	internal fun recycleGraphics() {
		mLines.forEach {
			it.recycleGraphics()
		}
	}

	internal fun getMIDIBeatTime(beat: Int): Long {
		repeat(mNumberOfMIDIBeatBlocks) {
			val (blockStartTime, midiBeatCount, nanoPerBeat) = mBeatBlocks[it]
			if (midiBeatCount <= beat && (it + 1 == mNumberOfMIDIBeatBlocks || mBeatBlocks[it + 1].midiBeatCount > beat))
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
		private val mCommentGraphic = ScreenComment(mText, screenSize, paint, font)

		fun isIntendedFor(audience: String): Boolean {
			return commentAudience.isEmpty() ||
				audience.isBlank() ||
				audience.lowercase().splitAndTrim(",").intersect(commentAudience.toSet()).any()
		}

		fun draw(canvas: Canvas, paint: Paint, textColor: Int) {
			mCommentGraphic.draw(canvas, paint, textColor)
		}
	}

	companion object {
		private fun findBackingTrack(firstEvent: LinkedEvent): AudioFile? {
			// Find the backing track (if any)
			var thisEvent: LinkedEvent? = firstEvent
			while (thisEvent != null) {
				val innerEvent = thisEvent.mEvent
				if (innerEvent is AudioEvent && innerEvent.mBackingTrack)
					return innerEvent.mAudioFile
				thisEvent = thisEvent.mNextEvent
			}
			return null
		}
	}
}
