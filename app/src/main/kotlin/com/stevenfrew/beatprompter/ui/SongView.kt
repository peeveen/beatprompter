package com.stevenfrew.beatprompter.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.OverScroller
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GestureDetectorCompat
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.audio.AudioPlayer
import com.stevenfrew.beatprompter.audio.AudioPlayerFactory
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothController
import com.stevenfrew.beatprompter.comm.bluetooth.message.PauseOnScrollStartMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.QuitSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.SetSongTimeMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.midi.MidiController
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.song.PlayState
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.Song
import com.stevenfrew.beatprompter.song.event.AudioEvent
import com.stevenfrew.beatprompter.song.event.BeatEvent
import com.stevenfrew.beatprompter.song.event.CommentEvent
import com.stevenfrew.beatprompter.song.event.EndEvent
import com.stevenfrew.beatprompter.song.event.LineEvent
import com.stevenfrew.beatprompter.song.event.LinkedEvent
import com.stevenfrew.beatprompter.song.event.MIDIEvent
import com.stevenfrew.beatprompter.song.event.PauseEvent
import com.stevenfrew.beatprompter.song.line.Line
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SongView
	: AppCompatImageView,
	GestureDetector.OnGestureListener {

	private val mDestinationGraphicRect = Rect(0, 0, 0, 0)
	private val mCurrentBeatCountRect = Rect()
	private var mEndSongByPedalCounter = 0
	private var mMetronomeOn: Boolean = false
	private var mInitialized = false
	private var mSkipping = false
	private var mCurrentVolume = Preferences.defaultTrackVolume
	private var mLastCommentEvent: CommentEvent? = null
	private var mLastCommentTime: Long = 0
	private var mLastTempMessageTime: Long = 0
	private var mLastBeatTime: Long = 0
	private val mPaint = Paint()
	private val mScroller: OverScroller
	private val mMetronomePref: MetronomeContext

	private var mManualMetronomeTask: ManualMetronomeTask? = null
	private var mManualMetronomeThread: Thread? = null

	private var mSong: Song? = null

	private var mSongStartTime: Long = 0
	private var mStartState = PlayState.AtTitleScreen
	private var mUserHasScrolled = false
	private var mPauseTime: Long = 0
	private var mNanosecondsPerBeat = Utils.nanosecondsPerBeat(120.0)

	private val mBackgroundColorLookup = IntArray(101)
	private val mCommentTextColor: Int
	private val mPageDownMarkerColor: Int
	private val mBeatCounterColor: Int
	private val mDefaultCurrentLineHighlightColor: Int
	private val mBeatSectionStartHighlightColors: IntArray
	private val mDefaultPageDownLineHighlightColor: Int
	private val mShowScrollIndicator: Boolean
	private val mShowSongTitle: Boolean
	private val mCommentDisplayTimeNanoseconds: Long
	private val mAudioPlayerFactory: AudioPlayerFactory
	private var mAudioPlayers = mapOf<AudioFile, AudioPlayer>()
	private val mSilenceAudioPlayer: AudioPlayer

	private var mPulse: Boolean
	private var mSongPixelPosition = 0
	private var mTargetPixelPosition = -1
	private var mTargetAcceleration = 1
	private val mHighlightCurrentLine: Boolean
	private val mHighlightBeatSectionStart: Boolean
	private val mShowPageDownMarker: Boolean
	private var mSongTitleContrastBackground: Int = 0
	private var mSongTitleContrastBeatCounter: Int = 0
	private val mScrollIndicatorRect = Rect()
	private var mGestureDetector: GestureDetectorCompat? = null
	private var mScreenAction = ScreenAction.Scroll
	private val mScrollMarkerColor: Int
	private var mSongDisplayActivity: SongDisplayActivity? = null
	private val mExternalTriggerSafetyCatch: TriggerSafetyCatch
	private val mSendMidiClockPreference: Boolean
	private var mSendMidiClock = false
	private val mManualScrollPositions: ManualScrollPositions = ManualScrollPositions()

	private val mClickSoundPool: SoundPool = SoundPool
		.Builder()
		.setMaxStreams(16)
		.setAudioAttributes(SongViewAudioAttributes)
		.build()
	private val mClickAudioID = mClickSoundPool.load(this.context, R.raw.click, 0)

	enum class ScreenAction {
		Scroll, Volume, None
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		mScroller = OverScroller(context)
		mGestureDetector = GestureDetectorCompat(context, this)
		mSongPixelPosition = 0

		mAudioPlayerFactory = AudioPlayerFactory(Preferences.audioPlayer, context)
		mSilenceAudioPlayer = mAudioPlayerFactory.createSilencePlayer()

		mScreenAction = Preferences.screenAction
		mShowScrollIndicator = Preferences.showScrollIndicator
		mShowSongTitle = Preferences.showSongTitle
		val commentDisplayTimeSeconds = Preferences.commentDisplayTime
		mCommentDisplayTimeNanoseconds = Utils.milliToNano(commentDisplayTimeSeconds * 1000)
		mExternalTriggerSafetyCatch = Preferences.midiTriggerSafetyCatch
		mHighlightCurrentLine = Preferences.highlightCurrentLine
		mShowPageDownMarker = Preferences.showPageDownMarker
		mHighlightBeatSectionStart = Preferences.highlightBeatSectionStart
		mBeatCounterColor = Preferences.beatCounterColor
		mCommentTextColor = Preferences.commentColor
		mPageDownMarkerColor = Preferences.pageDownMarkerColor
		mScrollMarkerColor = Preferences.scrollIndicatorColor
		val mHighlightBeatSectionStartColor = Preferences.beatSectionStartHighlightColor
		mBeatSectionStartHighlightColors =
			createStrobingHighlightColourArray(mHighlightBeatSectionStartColor)

		mDefaultCurrentLineHighlightColor =
			Utils.makeHighlightColour(Preferences.currentLineHighlightColor)
		mDefaultPageDownLineHighlightColor = Utils.makeHighlightColour(Preferences.pageDownMarkerColor)
		mPulse = Preferences.pulseDisplay
		mSendMidiClockPreference = Preferences.sendMIDIClock
		mMetronomePref = if (Preferences.mute) MetronomeContext.Off else Preferences.metronomeContext

		mSongTitleContrastBeatCounter = Utils.makeContrastingColour(mBeatCounterColor)
		val backgroundColor = Preferences.backgroundColor
		val pulseColor =
			if (mPulse)
				Preferences.pulseColor
			else
				backgroundColor
		val bgR = Color.red(backgroundColor)
		val bgG = Color.green(backgroundColor)
		val bgB = Color.blue(backgroundColor)
		val pR = Color.red(pulseColor)
		val pG = Color.green(pulseColor)
		val pB = Color.blue(pulseColor)
		val rDiff = pR - bgR
		val gDiff = pG - bgG
		val bDiff = pB - bgB
		repeat(101) {
			val sineLookup = Utils.mSineLookup[(90.0 * (it.toDouble() / 100.0)).toInt()]
			val red = pR - (sineLookup * rDiff.toDouble()).toInt()
			val green = pG - (sineLookup * gDiff.toDouble()).toInt()
			val blue = pB - (sineLookup * bDiff.toDouble()).toInt()
			val color = Color.rgb(red, green, blue)
			mBackgroundColorLookup[it] = color
		}
		mSongTitleContrastBackground = Utils.makeContrastingColour(mBackgroundColorLookup[100])
	}

	// Constructor
	constructor(context: Context) : this(context, null)

	fun init(songDisplayActivity: SongDisplayActivity, song: Song) {
		mSongDisplayActivity = songDisplayActivity

		if (song.mSongFile.mBPM > 0.0) {
			val metronomeOnPref = mMetronomePref == MetronomeContext.On
			val metronomeOnWhenNoBackingTrackPref = mMetronomePref == MetronomeContext.OnWhenNoTrack
			// We switch the metronome on (full time) if the pref says "on"
			// or if it says "when no audio track" and there are no audio events.
			mMetronomeOn =
				metronomeOnPref || (metronomeOnWhenNoBackingTrackPref && !song.mAudioEvents.any())
		}

		if (song.mAudioEvents.any())
			mSilenceAudioPlayer.start()

		val audioPlayerMap = song.mAudioEvents.associateBy(
			{ it.mAudioFile },
			{
				try {
					mAudioPlayerFactory.create(it.mAudioFile.mFile, it.mVolume)
				} catch (e: Exception) {
					null
				}
			}
		)

		if (audioPlayerMap.any { it.value == null })
			Toast.makeText(context, R.string.crap_audio_file_warning, Toast.LENGTH_LONG).show()

		@Suppress("UNCHECKED_CAST")
		mAudioPlayers = audioPlayerMap.filterValues { it != null } as Map<AudioFile, AudioPlayer>

		mSendMidiClock = song.mSendMIDIClock || mSendMidiClockPreference
		mCurrentBeatCountRect.apply {
			left = song.mBeatCounterRect.left
			top = song.mBeatCounterRect.top
			right = song.mBeatCounterRect.right
			bottom = song.mBeatCounterRect.bottom
		}
		mSong = song

		calculateManualScrollPositions()
	}

	private fun ensureInitialised() {
		if (mSong == null)
			return
		if (!mInitialized) {
			if (mSong!!.mSmoothMode)
				mPulse = false
			mInitialized = true
			if (mSong!!.mManualMode) {
				if (mMetronomeOn) {
					mManualMetronomeTask = ManualMetronomeTask(mSong!!.mSongFile.mBPM)
					mManualMetronomeThread = Thread(mManualMetronomeTask).apply { start() }
				}
			}
		}
	}

	// Called back to draw the view. Also called by invalidate().
	override fun onDraw(canvas: Canvas) {
		if (mSong == null)
			return
		ensureInitialised()
		val scrolling = calculateScrolling()
		var timePassed: Long = 0
		var beatPercent = 1.0
		var showTempMessage = false
		var showComment = false
		val time = System.nanoTime()

		if (mStartState === PlayState.Playing && !scrolling) {
			timePassed = max(0, time - mSongStartTime)
			if (mLastBeatTime > 0) {
				val beatTimePassed = max(0, time - mLastBeatTime)
				val beatTime = (beatTimePassed % mNanosecondsPerBeat).toDouble()
				beatPercent = beatTime / mNanosecondsPerBeat
			}

			if (mSong!!.mCurrentLine.mScrollMode !== ScrollingMode.Manual)
				if (processSongEvents(time, timePassed))
					return

			showTempMessage = time - mLastTempMessageTime < SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS
			if (mLastCommentEvent != null)
				if (time - mLastCommentTime < mCommentDisplayTimeNanoseconds)
					showComment = true
		}
		var currentY = mSong!!.mBeatCounterRect.height() + mSong!!.mDisplayOffset
		var currentLine = mSong!!.mCurrentLine
		var yScrollOffset = 0
		if (currentLine.mScrollMode !== ScrollingMode.Beat)
			beatPercent = 1.0
		val color = mBackgroundColorLookup[(beatPercent * 100.0).toInt()]
		canvas.drawColor(color, PorterDuff.Mode.SRC)
		if (mStartState !== PlayState.AtTitleScreen) {
			var scrollPercentage = 0.0
			// If a scroll event in underway, move currentY up
			if (mStartState !== PlayState.Playing || currentLine.mScrollMode === ScrollingMode.Manual) {
				yScrollOffset = mSongPixelPosition - currentLine.mSongPixelPosition
				if (currentLine.mScrollMode === ScrollingMode.Smooth)
					scrollPercentage =
						yScrollOffset.toDouble() / currentLine.mMeasurements.mLineHeight.toDouble()
			} else {
				if (!scrolling) {
					if (!mSong!!.mNoScrollLines.contains(currentLine)) {
						if (currentLine.mYStopScrollTime > timePassed && currentLine.mYStartScrollTime <= timePassed)
							scrollPercentage =
								(timePassed - currentLine.mYStartScrollTime).toDouble() / (currentLine.mYStopScrollTime - currentLine.mYStartScrollTime).toDouble()
						else if (currentLine.mYStopScrollTime <= timePassed)
							scrollPercentage = 1.0
						// In smooth mode, if we're on the last line, prevent it scrolling up more than necessary ... i.e. keep as much song onscreen as possible.
						if (currentLine.mScrollMode === ScrollingMode.Smooth) {
							val remainingSongHeight = mSong!!.mHeight - currentLine.mSongPixelPosition
							val remainingScreenHeight = mSong!!.mDisplaySettings.mScreenSize.height() - currentY
							yScrollOffset = min(
								(currentLine.mMeasurements.mLineHeight * scrollPercentage).toInt(),
								remainingSongHeight - remainingScreenHeight
							)
						} else if (currentLine.mScrollMode === ScrollingMode.Beat)
							yScrollOffset =
								currentLine.mMeasurements.mJumpScrollIntervals[(scrollPercentage * 100.0).toInt()]
					}
				}
			}

			currentY -= yScrollOffset
			if (mStartState === PlayState.Playing)
				mSongPixelPosition = currentLine.mSongPixelPosition + yScrollOffset

			val startY = currentY
			var firstLineOnscreen: Line? = null
			while (currentY < mSong!!.mDisplaySettings.mScreenSize.height()) {
				if (currentY > mSong!!.mBeatCounterRect.height() - currentLine.mMeasurements.mLineHeight) {
					if (firstLineOnscreen == null)
						firstLineOnscreen = currentLine
					val graphics = currentLine.getGraphics(mPaint)
					val lineTop = currentY
					for (f in graphics.indices) {
						val graphic = graphics[f]
						val sourceRect = currentLine.mMeasurements.mGraphicRectangles[f]
						mDestinationGraphicRect.set(sourceRect)
						mDestinationGraphicRect.offset(0, currentY)
						canvas.drawBitmap(graphic.bitmap, sourceRect, mDestinationGraphicRect, mPaint)
						currentY += currentLine.mMeasurements.mGraphicHeights[f]
					}
					val highlightColor = getLineHighlightColor(currentLine, time)
					if (highlightColor != null) {
						mPaint.color = highlightColor
						canvas.drawRect(
							0f,
							lineTop.toFloat(),
							mSong!!.mDisplaySettings.mScreenSize.width().toFloat(),
							(lineTop + currentLine.mMeasurements.mLineHeight).toFloat(),
							mPaint
						)
						mPaint.alpha = 255
					}

				} else
					currentY += currentLine.mMeasurements.mLineHeight
				if (currentLine.mNextLine == null)
					break
				currentLine = currentLine.mNextLine!!
			}

			if (mSong!!.mSmoothMode) {
				val prevLine = mSong!!.mCurrentLine.mPrevLine
				if (prevLine != null && startY > 0) {
					mPaint.alpha = (255.0 - 255.0 * scrollPercentage).toInt()
					currentY = startY - prevLine.mMeasurements.mLineHeight
					val graphics = prevLine.getGraphics(mPaint)
					for (f in graphics.indices) {
						val graphic = graphics[f]
						canvas.drawBitmap(graphic.bitmap, 0f, currentY.toFloat(), mPaint)
						currentY += prevLine.mMeasurements.mGraphicHeights[f]
					}
					mPaint.alpha = 255
				}
			}
		}
		mPaint.color = mBackgroundColorLookup[100]
		canvas.drawRect(mSong!!.mBeatCounterRect, mPaint)
		mPaint.color = mScrollMarkerColor
		if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Beat && mShowScrollIndicator)
			canvas.drawRect(mScrollIndicatorRect, mPaint)
		mPaint.color = mBeatCounterColor
		mPaint.strokeWidth = 1.0f
		canvas.drawRect(mCurrentBeatCountRect, mPaint)
		canvas.drawLine(
			0f,
			mSong!!.mBeatCounterRect.height().toFloat(),
			mSong!!.mDisplaySettings.mScreenSize.width().toFloat(),
			mSong!!.mBeatCounterRect.height().toFloat(),
			mPaint
		)
		if (mShowPageDownMarker)
			showPageDownMarkers(canvas)
		if (mShowSongTitle)
			showSongTitle(canvas)
		if (showTempMessage) {
			if (mEndSongByPedalCounter == 0)
				showTempMessage("$mCurrentVolume%", 80, Color.BLACK, canvas)
			else {
				val message =
					"Press pedal " + (SONG_END_PEDAL_PRESSES - mEndSongByPedalCounter) + " more times to end song."
				showTempMessage(message, 20, Color.BLUE, canvas)
			}
		} else
			mEndSongByPedalCounter = 0
		if (showComment)
			showComment(canvas)
		if (mStartState !== PlayState.AtTitleScreen)
			invalidate()  // Force a re-draw
		else if (mSong != null)
			drawTitleScreen(canvas)
	}

	private fun processSongEvents(time: Long, timePassed: Long): Boolean {
		var event: LinkedEvent?
		do {
			event = mSong!!.getNextEvent(timePassed)
			if (event != null) {
				when (val innerEvent = event.mEvent) {
					is CommentEvent -> processCommentEvent(innerEvent, time)
					is BeatEvent -> processBeatEvent(innerEvent, true)
					is MIDIEvent -> processMIDIEvent(innerEvent)
					is PauseEvent -> processPauseEvent(innerEvent)
					is LineEvent -> processLineEvent(innerEvent)
					is AudioEvent -> processAudioEvent(innerEvent)
					is EndEvent -> {
						processEndEvent()
						return true
					}
				}
			}
		} while (event != null)
		return false
	}

	private fun calculateScrolling(): Boolean {
		return if (mStartState === PlayState.AtTitleScreen)
			false
		else if ((mScreenAction == ScreenAction.Scroll || mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual) && mScroller.computeScrollOffset()) {
			mSongPixelPosition = mScroller.currY
			val songTime = mSong!!.mCurrentLine.getTimeFromPixel(mSongPixelPosition)
			setSongTime(
				songTime,
				mStartState === PlayState.Paused,
				broadcast = true,
				setPixelPosition = false,
				recalculateManualPositions = true
			)
			true
		} else {
			if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition) {
				val diff = min(2048, max(-2048, mTargetPixelPosition - mSongPixelPosition))
				val absDiff = abs(diff)
				val targetAcceleration = min(mAccelerations[absDiff - 1], absDiff)
				if (mTargetAcceleration * 2 < targetAcceleration)
					mTargetAcceleration *= 2
				else
					mTargetAcceleration = targetAcceleration
				if (diff > 0)
					mSongPixelPosition += mTargetAcceleration
				else
					mSongPixelPosition -= mTargetAcceleration
				if (mSongPixelPosition == mTargetPixelPosition)
					clearScrollTarget()
				val songTime = mSong!!.mCurrentLine.getTimeFromPixel(mSongPixelPosition)
				setSongTime(
					songTime,
					mStartState === PlayState.Paused,
					broadcast = true,
					setPixelPosition = false,
					recalculateManualPositions = false
				)
			}
			false
		}
	}

	private fun drawTitleScreen(canvas: Canvas) {
		canvas.drawColor(Color.BLACK)
		val midX = mSong!!.mDisplaySettings.mScreenSize.width() shr 1
		val fifteenPercent = mSong!!.mDisplaySettings.mScreenSize.height() * 0.15f
		var startY =
			floor(((mSong!!.mDisplaySettings.mScreenSize.height() - mSong!!.mTotalStartScreenTextHeight) / 2).toDouble()).toInt()
		val nextSongSS = mSong!!.mNextSongString
		if (nextSongSS != null) {
			mPaint.color = if (mSkipping) Color.RED else Color.WHITE
			val halfDiff = (fifteenPercent - nextSongSS.mHeight) / 2.0f
			canvas.drawRect(
				0f,
				mSong!!.mDisplaySettings.mScreenSize.height() - fifteenPercent,
				mSong!!.mDisplaySettings.mScreenSize.width().toFloat(),
				mSong!!.mDisplaySettings.mScreenSize.height().toFloat(),
				mPaint
			)
			val nextSongY =
				mSong!!.mDisplaySettings.mScreenSize.height() - (nextSongSS.mDescenderOffset + halfDiff).toInt()
			startY -= (fifteenPercent / 2.0f).toInt()
			with(mPaint) {
				color = nextSongSS.mColor
				textSize = nextSongSS.mFontSize * Utils.FONT_SCALING
				typeface = nextSongSS.mFace
				flags = Paint.ANTI_ALIAS_FLAG
			}
			canvas.drawText(
				nextSongSS.mText,
				(midX - (nextSongSS.mWidth shr 1)).toFloat(),
				nextSongY.toFloat(),
				mPaint
			)
		}
		for (ss in mSong!!.mStartScreenStrings) {
			startY += ss.mHeight
			with(mPaint) {
				color = ss.mColor
				textSize = ss.mFontSize * Utils.FONT_SCALING
				typeface = ss.mFace
				flags = Paint.ANTI_ALIAS_FLAG
			}
			canvas.drawText(
				ss.mText,
				(midX - (ss.mWidth shr 1)).toFloat(),
				(startY - ss.mDescenderOffset).toFloat(),
				mPaint
			)
		}
	}

	private fun showTempMessage(message: String, textSize: Int, textColor: Int, canvas: Canvas) {
		val popupMargin = 25
		mPaint.strokeWidth = 2.0f
		mPaint.textSize = textSize * Utils.FONT_SCALING
		mPaint.flags = Paint.ANTI_ALIAS_FLAG
		val outRect = Rect()
		mPaint.getTextBounds(message, 0, message.length, outRect)
		val textWidth = mPaint.measureText(message)
		val textHeight = outRect.height()
		val volumeControlWidth = textWidth + popupMargin * 2.0f
		val volumeControlHeight = textHeight + popupMargin * 2
		val x = (mSong!!.mDisplaySettings.mScreenSize.width() - volumeControlWidth) / 2.0f
		val y = (mSong!!.mDisplaySettings.mScreenSize.height() - volumeControlHeight) / 2
		mPaint.color = Color.BLACK
		canvas.drawRect(
			x,
			y.toFloat(),
			x + volumeControlWidth,
			(y + volumeControlHeight).toFloat(),
			mPaint
		)
		mPaint.color = Color.rgb(255, 255, 200)
		canvas.drawRect(
			x + 1,
			(y + 1).toFloat(),
			x + (volumeControlWidth - 2),
			(y + (volumeControlHeight - 2)).toFloat(),
			mPaint
		)
		mPaint.color = textColor
		canvas.drawText(
			message,
			(mSong!!.mDisplaySettings.mScreenSize.width() - textWidth) / 2,
			((mSong!!.mDisplaySettings.mScreenSize.height() - textHeight) / 2 + textHeight).toFloat(),
			mPaint
		)
	}

	private fun showPageDownMarkers(canvas: Canvas) {
		if (mSong!!.mCurrentLine.mScrollMode == ScrollingMode.Manual && mSongPixelPosition < mSong!!.mScrollEndPixel) {
			val scrollPosition =
				((mManualScrollPositions.mPageDownPosition - mSongPixelPosition) + mSong!!.mDisplaySettings.mBeatCounterRect.height()).toFloat()
			val screenHeight = mSong!!.mDisplaySettings.mScreenSize.height().toFloat()
			val screenWidth = mSong!!.mDisplaySettings.mScreenSize.width().toFloat()
			val lineSize = screenWidth / 10.0f

			mPaint.strokeWidth = (screenWidth + screenHeight) / 200.0f
			mPaint.color = mPageDownMarkerColor
			canvas.drawLine(0.0f, scrollPosition + lineSize, 0.0f, scrollPosition, mPaint)
			canvas.drawLine(0.0f, scrollPosition, lineSize, scrollPosition, mPaint)
			canvas.drawLine(screenWidth, scrollPosition + lineSize, screenWidth, scrollPosition, mPaint)
			canvas.drawLine(screenWidth, scrollPosition, screenWidth - lineSize, scrollPosition, mPaint)
		}
	}

	private fun showSongTitle(canvas: Canvas) {
		if (mSong == null)
			return

		with(mPaint)
		{
			textSize = mSong!!.mSongTitleHeader.mFontSize * Utils.FONT_SCALING
			typeface = mSong!!.mSongTitleHeader.mFace
			flags = Paint.ANTI_ALIAS_FLAG
			color = mSongTitleContrastBackground
		}

		canvas.drawText(
			mSong!!.mSongTitleHeader.mText,
			mSong!!.mSongTitleHeaderLocation.x,
			mSong!!.mSongTitleHeaderLocation.y,
			mPaint
		)

		canvas.save()
		canvas.clipRect(mCurrentBeatCountRect)
		mPaint.color = mSongTitleContrastBeatCounter
		canvas.drawText(
			mSong!!.mSongTitleHeader.mText,
			mSong!!.mSongTitleHeaderLocation.x,
			mSong!!.mSongTitleHeaderLocation.y,
			mPaint
		)
		canvas.restore()

		canvas.save()
		canvas.clipRect(mScrollIndicatorRect)
		canvas.drawText(
			mSong!!.mSongTitleHeader.mText,
			mSong!!.mSongTitleHeaderLocation.x,
			mSong!!.mSongTitleHeaderLocation.y,
			mPaint
		)
		canvas.restore()

		mPaint.alpha = 255
	}

	private fun showComment(canvas: Canvas) {
		if (mLastCommentEvent != null)
			mLastCommentEvent!!.mComment.draw(canvas, mPaint, mCommentTextColor)
	}

	private fun startToggle(playState: PlayState) {
		mStartState = playState
		startToggle(null, false)
	}

	private fun startAudioPlayer(audioPlayer: AudioPlayer?): AudioPlayer? {
		return audioPlayer?.apply {
			Logger.log("Starting AudioPlayer")
			start()
			mCurrentVolume = volume
		}
	}

	private fun startBackingTrack(): Boolean {
		return startAudioPlayer(mAudioPlayers[mSong!!.mBackingTrack]) != null
	}

	private fun startToggle(e: MotionEvent?, midiInitiated: Boolean): Boolean {
		if (mSong == null)
			return true
		if (mStartState !== PlayState.Playing) {
			if (mStartState === PlayState.AtTitleScreen)
				if (e != null)
					if (e.y > mSong!!.mDisplaySettings.mScreenSize.height() * 0.85f)
						if (mSong!!.mNextSong.isNotBlank()) {
							endSong(true)
							return true
						}
			val oldPlayState = mStartState
			mStartState = PlayState.increase(mStartState)
			if (mStartState === PlayState.Playing) {
				if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual) {
					// Start the count in.
					if (mManualMetronomeThread != null) {
						if (!mManualMetronomeThread!!.isAlive) {
							return if (mMetronomeOn) {
								mManualMetronomeThread!!.start()
								true
							} else
								startBackingTrack()
						}
					} else
						return startBackingTrack()
				} else {
					val time: Long
					if (mUserHasScrolled) {
						mUserHasScrolled = false
						time = mSong!!.getTimeFromPixel(mSongPixelPosition)
						setSongTime(
							time,
							redraw = false,
							broadcast = false,
							setPixelPosition = false,
							recalculateManualPositions = true
						)
					} else {
						Logger.log { "Resuming, pause time=$mPauseTime" }
						time = mPauseTime
						setSongTime(
							time,
							redraw = false,
							broadcast = false,
							setPixelPosition = true,
							recalculateManualPositions = true
						)
					}
					BluetoothController.putMessage(
						ToggleStartStopMessage(
							ToggleStartStopMessage.StartStopToggleInfo(
								oldPlayState,
								time
							)
						)
					)
				}
			} else
				BluetoothController.putMessage(
					ToggleStartStopMessage(
						ToggleStartStopMessage.StartStopToggleInfo(
							oldPlayState,
							0
						)
					)
				)
		} else {
			if (mScreenAction == ScreenAction.Volume) {
				if (e != null) {
					if (e.y < mSong!!.mDisplaySettings.mScreenSize.height() * 0.5)
						changeVolume(+5)
					else if (e.y > mSong!!.mDisplaySettings.mScreenSize.height() * 0.5)
						changeVolume(-5)
				}
			} else if (mSong!!.mCurrentLine.mScrollMode !== ScrollingMode.Manual) {
				if (mScreenAction == ScreenAction.Scroll)
					pause(midiInitiated)
			}
		}
		invalidate()
		return true
	}

	private fun changeVolume(amount: Int) {
		if (mStartState === PlayState.Paused)
			return
		mCurrentVolume += amount
		onVolumeChanged()
	}

	fun pause(midiInitiated: Boolean) {
		if (mScreenAction != ScreenAction.Scroll)
			return
		val nanoTime = System.nanoTime()
		mPauseTime = nanoTime - if (mSongStartTime == 0L) nanoTime else mSongStartTime
		BluetoothController.putMessage(
			ToggleStartStopMessage(
				ToggleStartStopMessage.StartStopToggleInfo(
					mStartState,
					mPauseTime
				)
			)
		)
		mStartState = PlayState.reduce(mStartState)
		mAudioPlayers.values.forEach {
			if (it.isPlaying)
				it.pause()
		}
		if (!midiInitiated)
			if (mSongDisplayActivity != null)
				mSongDisplayActivity!!.onSongStop()
	}

	fun stop(destroyed: Boolean) {
		if (mStartState === PlayState.Playing)
			pause(false)
		if (destroyed) {
			if (mSong != null) {
				BluetoothController.putMessage(
					QuitSongMessage(
						mSong!!.mSongFile.mNormalizedTitle,
						mSong!!.mSongFile.mNormalizedArtist
					)
				)
				mSong!!.recycleGraphics()
			}
			mSong = null
			Task.stopTask(mManualMetronomeTask, mManualMetronomeThread)
			mAudioPlayers.values.forEach {
				it.stop()
				it.release()
			}
			mSilenceAudioPlayer.stop()
			mSilenceAudioPlayer.release()
			mClickSoundPool.release()
			System.gc()
		}
	}

	private fun processCommentEvent(event: CommentEvent, systemTime: Long) {
		mLastCommentTime = systemTime
		mLastCommentEvent = event
	}

	private fun processBeatEvent(event: BeatEvent, allowClick: Boolean) {
		val playClick =
			allowClick && (mMetronomePref !== MetronomeContext.OnWhenNoTrack || !isTrackPlaying())
		mNanosecondsPerBeat = Utils.nanosecondsPerBeat(event.mBPM)
		val beatWidth = mSong!!.mDisplaySettings.mScreenSize.width().toDouble() / event.mBPB.toDouble()
		val currentBeatCounterWidth = (beatWidth * (event.mBeat + 1).toDouble()).toInt()
		if (event.mWillScrollOnBeat != -1) {
			val thirdWidth = beatWidth / 3
			val thirdHeight = mSong!!.mBeatCounterRect.height() / 3.0
			val scrollIndicatorStart = (beatWidth * event.mWillScrollOnBeat + thirdWidth).toInt()
			val scrollIndicatorEnd = (beatWidth * (event.mWillScrollOnBeat + 1) - thirdWidth).toInt()
			mScrollIndicatorRect.apply {
				left = scrollIndicatorStart
				top = thirdHeight.toInt()
				right = scrollIndicatorEnd
				bottom = (thirdHeight * 2.0).toInt()
			}
		} else
			clearScrollIndicatorRect()
		mCurrentBeatCountRect.apply {
			if (mSong!!.mCurrentLine.mScrollMode == ScrollingMode.Beat) {
				left = (currentBeatCounterWidth - beatWidth).toInt()
				top = 0
				right = currentBeatCounterWidth
				bottom = mSong!!.mBeatCounterRect.height()
			} else {
				left = mSong!!.mBeatCounterRect.left
				top = mSong!!.mBeatCounterRect.top
				right = mSong!!.mBeatCounterRect.right
				bottom = mSong!!.mBeatCounterRect.bottom
			}
		}
		mLastBeatTime = mSongStartTime + event.mEventTime
		if (event.mClick && mStartState === PlayState.Playing && mSong!!.mCurrentLine.mScrollMode !== ScrollingMode.Manual && playClick)
			mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f)
		if (mSongDisplayActivity != null/*&&(!event.mCount)*/)
			mSongDisplayActivity!!.onSongBeat(event.mBPM)
	}

	private fun isTrackPlaying(): Boolean {
		return mAudioPlayers.values.any { it.isPlaying }
	}

	fun hasSong(title: String, artist: String): Boolean {
		return mSong?.mSongFile?.mNormalizedArtist == artist && mSong?.mSongFile?.mNormalizedTitle == title
	}

	private fun processPauseEvent(event: PauseEvent) {
		mLastBeatTime = -1
		val currentBeatCounterWidth = (mSong!!.mDisplaySettings.mScreenSize.width()
			.toDouble() / (event.mBeats - 1).toDouble() * event.mBeat.toDouble()).toInt()
		mCurrentBeatCountRect.apply {
			left = 0
			top = 0
			right = currentBeatCounterWidth
			bottom = mSong!!.mBeatCounterRect.height()
		}
		clearScrollIndicatorRect()
	}

	private fun clearScrollIndicatorRect() {
		mScrollIndicatorRect.apply {
			left = -1
			top = -1
			right = -1
			bottom = -1
		}
	}

	private fun processMIDIEvent(event: MIDIEvent) {
		event.mMessages.forEach {
			MidiController.putMessage(it)
		}
	}

	private fun processLineEvent(event: LineEvent) {
		if (mSong == null)
			return
		mSong!!.mCurrentLine = event.mLine
		if (mSong!!.mCurrentLine.mScrollMode == ScrollingMode.Manual) {
			mCurrentBeatCountRect.apply {
				left = mSong!!.mBeatCounterRect.left
				top = mSong!!.mBeatCounterRect.top
				right = mSong!!.mBeatCounterRect.right
				bottom = mSong!!.mBeatCounterRect.bottom
			}
			calculateManualScrollPositions()
		}
	}

	private fun processAudioEvent(event: AudioEvent): Boolean {
		val audioPlayer = mAudioPlayers[event.mAudioFile] ?: return false
		Logger.log("Track event hit: starting AudioPlayer")
		audioPlayer.seekTo(0)
		startAudioPlayer(audioPlayer)
		return true
	}

	private fun processEndEvent() {
		// Only end the song in non-manual mode.
		endSong(false)
	}

	private fun endSong(skipped: Boolean) {
		if (mSongDisplayActivity != null) {
			mSkipping = skipped
			SongListFragment.mSongEndedNaturally = true
			mStartState = PlayState.AtTitleScreen
			mSongDisplayActivity = null
			if (mSong != null)
				mSong!!.recycleGraphics()
			mSong = null
			EventRouter.sendEventToSongDisplay(Events.END_SONG)
			System.gc()
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		this.mGestureDetector!!.onTouchEvent(event)
		return super.onTouchEvent(event)
	}

	fun processBluetoothToggleStartStopMessage(startStopInfo: ToggleStartStopMessage.StartStopToggleInfo) {
		if (startStopInfo.mTime >= 0)
			setSongTime(
				startStopInfo.mTime,
				redraw = true,
				broadcast = false,
				setPixelPosition = true,
				recalculateManualPositions = true
			)
		startToggle(startStopInfo.mStartState)
	}

	private fun seekTrack(audioFile: AudioFile, time: Int): AudioPlayer? {
		val audioPlayer = mAudioPlayers[audioFile]
		if (audioPlayer != null && audioPlayer.duration > time) {
			audioPlayer.seekTo(time.toLong())
			return audioPlayer
		}
		return null
	}

	fun setSongTime(
		nano: Long,
		redraw: Boolean,
		broadcast: Boolean,
		setPixelPosition: Boolean,
		recalculateManualPositions: Boolean
	) {
		if (mSong == null)
			return
		// No time context in Manual mode.
		if (setPixelPosition)
			mSongPixelPosition = mSong!!.getPixelFromTime(nano)
		run {
			if (mStartState !== PlayState.Playing)
				mPauseTime = nano
			if (broadcast)
				BluetoothController.putMessage(SetSongTimeMessage(nano))
			mSong!!.setProgress(nano)
			var musicPlaying = false
			if (mSong!!.mCurrentLine.mScrollMode !== ScrollingMode.Manual) {
				val audioEvent = mSong!!.mCurrentEvent.mPrevAudioEvent
				if (audioEvent != null) {
					val nTime = Utils.nanoToMilli(nano - audioEvent.mEventTime)
					musicPlaying = seekTrack(audioEvent.mAudioFile, nTime)?.apply {
						if (mStartState === PlayState.Playing)
							startAudioPlayer(this)
					} != null
				}
			}
			if (mSong!!.mCurrentLine.mScrollMode !== ScrollingMode.Manual) {
				val prevBeatEvent = mSong!!.mCurrentEvent.mPrevBeatEvent
				if (prevBeatEvent != null) {
					val nextBeatEvent = mSong!!.mCurrentEvent.mNextBeatEvent
					processBeatEvent(prevBeatEvent, nextBeatEvent != null && !musicPlaying)
				}
			} else {
				mCurrentBeatCountRect.apply {
					left = mSong!!.mBeatCounterRect.left
					top = mSong!!.mBeatCounterRect.top
					right = mSong!!.mBeatCounterRect.right
					bottom = mSong!!.mBeatCounterRect.bottom
				}
			}
			mSongStartTime = System.nanoTime() - nano
			if (redraw)
				invalidate()
		}
		if (recalculateManualPositions)
			calculateManualScrollPositions()
	}

	override fun onDown(e: MotionEvent): Boolean {
		if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			if (mManualMetronomeThread != null)
				if (mStartState === PlayState.Playing)
					mManualMetronomeThread!!.interrupt()
		// Abort any active scroll animations and invalidate.
		if (mScreenAction == ScreenAction.Scroll || mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			clearScrollTarget()
		mScroller.forceFinished(true)
		return true
	}

	override fun onShowPress(e: MotionEvent) {}

	override fun onSingleTapUp(e: MotionEvent): Boolean {
		startToggle(e, false)
		return true
	}

	override fun onScroll(
		e1: MotionEvent?,
		e2: MotionEvent,
		distanceX: Float,
		distanceY: Float
	): Boolean {
		if (mScreenAction == ScreenAction.None)
			return false
		if (mStartState === PlayState.AtTitleScreen)
			return false
		if (mSong == null)
			return false
		if (mScreenAction == ScreenAction.Scroll || mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual) {
			clearScrollTarget()
			mSongPixelPosition += distanceY.toInt()
			mSongPixelPosition = max(0, mSongPixelPosition)
			mSongPixelPosition = min(mSong!!.mScrollEndPixel, mSongPixelPosition)
			pauseOnScrollStart()
			setSongTime(
				mSong!!.mCurrentLine.getTimeFromPixel(mSongPixelPosition),
				redraw = true,
				broadcast = true,
				setPixelPosition = false,
				recalculateManualPositions = true
			)
		} else if (mScreenAction == ScreenAction.Volume) {
			mCurrentVolume += (distanceY / 10.0).toInt()
			onVolumeChanged()
		}
		return true
	}

	fun pauseOnScrollStart() {
		if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			return
		if (mScreenAction != ScreenAction.Scroll)
			return
		BluetoothController.putMessage(PauseOnScrollStartMessage)
		mUserHasScrolled = true
		mStartState = PlayState.Paused
		mAudioPlayers.values.forEach {
			Logger.log("Pausing AudioPlayers")
			if (it.isPlaying)
				it.pause()
		}
		if (mSongDisplayActivity != null)
			mSongDisplayActivity!!.onSongStop()
	}

	private fun onVolumeChanged() {
		mCurrentVolume = max(0, mCurrentVolume)
		mCurrentVolume = min(100, mCurrentVolume)
		mAudioPlayers.values.forEach {
			it.volume = mCurrentVolume
		}
		mLastTempMessageTime = System.nanoTime()
	}

	override fun onLongPress(e: MotionEvent) {}

	override fun onFling(
		e1: MotionEvent?,
		e2: MotionEvent,
		velocityX: Float,
		velocityY: Float
	): Boolean {
		if (mScreenAction == ScreenAction.None)
			return false
		if (mStartState === PlayState.AtTitleScreen)
			return false
		if (mSong == null)
			return false
		if (mScreenAction == ScreenAction.Scroll || mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual) {
			clearScrollTarget()
			pauseOnScrollStart()
			mScroller.fling(
				0,
				mSongPixelPosition,
				0,
				(-velocityY).toInt(),
				0,
				0,
				0,
				mSong!!.mScrollEndPixel
			)
		} else if (mScreenAction == ScreenAction.Volume)
			mScroller.fling(0, mCurrentVolume, 0, velocityY.toInt(), 0, 0, 0, 1000)
		return true
	}

	private fun changeThePageDown() {
		if (mSongPixelPosition == mSong!!.mScrollEndPixel)
			if (++mEndSongByPedalCounter == SONG_END_PEDAL_PRESSES)
				endSong(false)
			else
				mLastTempMessageTime = System.nanoTime()
		else
			changePage(true)
	}

	fun onOtherPageDownActivated() {
		if (mStartState !== PlayState.AtTitleScreen)
			onPageDownKeyPressed()
	}

	fun onPageDownKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changeThePageDown()
		} else if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			changeThePageDown()
		else
			changeVolume(+5)
	}

	fun onPageUpKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changePage(false)
		} else if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			changePage(false)
		else
			changeVolume(-5)
	}

	private fun changeTheLineDown() {
		if (mSongPixelPosition == mSong!!.mScrollEndPixel)
			if (++mEndSongByPedalCounter == SONG_END_PEDAL_PRESSES)
				endSong(false)
			else
				mLastTempMessageTime = System.nanoTime()
		else
			changeLine(true)
	}

	fun onLineDownKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changeTheLineDown()
		} else if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			changeTheLineDown()
		else
			changeVolume(+5)
	}

	fun onLineUpKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changeLine(false)
		} else if (mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
			changeLine(false)
		else
			changeVolume(-5)
	}

	fun onLeftKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changeVolume(-5)
		} else
			changeVolume(-5)
	}

	fun onRightKeyPressed() {
		if (mStartState !== PlayState.Playing) {
			if (!startToggle(null, false) && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual)
				changeVolume(+5)
		} else
			changeVolume(+5)
	}

	private fun changePage(down: Boolean) {
		if (mStartState === PlayState.AtTitleScreen)
			return
		if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
			return
		if (down && mManualScrollPositions.mBeatJumpScrollLine != null)
			setSongTime(
				mManualScrollPositions.mBeatJumpScrollLine!!.mLineTime,
				redraw = true,
				broadcast = true,
				setPixelPosition = true,
				recalculateManualPositions = false
			)
		else
			mTargetPixelPosition =
				if (down)
					mManualScrollPositions.mPageDownPosition
				else
					mManualScrollPositions.mPageUpPosition
	}

	private fun changeLine(down: Boolean) {
		if (mStartState === PlayState.AtTitleScreen)
			return
		if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
			return
		val targetLine =
			if (down)
				mSong!!.mCurrentLine.mNextLine
			else
				mSong!!.mCurrentLine.mPrevLine
		mTargetPixelPosition = (targetLine ?: mSong!!.mCurrentLine).mSongPixelPosition
	}

	private fun clearScrollTarget() {
		mTargetPixelPosition = -1
		mTargetAcceleration = 1
		calculateManualScrollPositions()
	}

	private fun calculateManualScrollPositions() {
		val currentLine = mSong!!.mCurrentLine
		// Don't bother doing this if we aren't in manual mode.
		if (currentLine.mScrollMode == ScrollingMode.Manual) {
			val usableScreenHeight = mSong!!.mDisplaySettings.mUsableScreenHeight

			// We don't always want to scroll bang onto a line. If the candidate line is really big
			// and starts too far up/down the page, then we will perform a "default scroll", which is
			// just a basic percentage of the screen size.
			val defaultScrollAmount = (usableScreenHeight * DEFAULT_PAGE_SCROLL_AMOUNT).toInt()

			// Okay, let's work out the page-up point first (that's a bit simpler).
			// Let's do page up first ... it's the simpler of the two.
			// The rules are dead easy.
			// - Scroll back by the default scroll amount, UNLESS EITHER
			// a) it takes us beyond the start of the song
			// b) it takes us into or through a non-manual section.

			// Here's the default scroll position
			val defaultPageUpScrollPosition = mSongPixelPosition - defaultScrollAmount

			// So now find the manual block start.
			var manualModeBlockStartPosition = currentLine.mSongPixelPosition
			var pageUpLine = currentLine
			while (pageUpLine.mPrevLine != null && pageUpLine.mPrevLine!!.mScrollMode == ScrollingMode.Manual) {
				pageUpLine = pageUpLine.mPrevLine!!
				manualModeBlockStartPosition = pageUpLine.mSongPixelPosition
			}

			// And take the greater of the two.
			val pageUpPosition = max(defaultPageUpScrollPosition, manualModeBlockStartPosition)

			// Now for page-down. Bit trickier.
			// Again, we might be using the "default scroll" amount.
			val defaultPageDownScrollPosition = mSongPixelPosition + defaultScrollAmount

			// We're going to traverse downwards from the current line. Initialise some vars.
			var pageDownLine = currentLine
			var beatJumpScrollLine: Line? = null
			var pageDownPosition = defaultPageDownScrollPosition

			// Keep going while there are still lines to check.
			while (pageDownLine.mNextLine != null) {
				val nextLine = pageDownLine.mNextLine!!

				// Escape clause 1: beat line!
				if (nextLine.mScrollMode == ScrollingMode.Beat) {
					// Whoa, we've found a beat line! End of the road.
					// If it is onscreen enough, we will use it.
					beatJumpScrollLine =
						if (nextLine.isFullyOnScreen(mSongPixelPosition) || nextLine.screenCoverage(
								mSongPixelPosition
							) > MINIMUM_SCREEN_COVERAGE_FOR_BEAT_SCROLL
						)
							nextLine
						else
							null
					pageDownPosition =
						when {
							// If we found a beat scroll line, we'll be scrolling to that.
							beatJumpScrollLine != null -> beatJumpScrollLine.mSongPixelPosition
							// Otherwise we will use the last manual line, unless that line is HUGE ...
							pageDownLine.screenCoverage(mSongPixelPosition) <= MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> pageDownLine.mSongPixelPosition
							// ...in which case, we will just do a default scroll.
							else -> defaultPageDownScrollPosition
						}
					break
				}
				// Escape clause 2: we've found a manual line that ISN'T fully onscreen.
				if (!nextLine.isFullyOnScreen(mSongPixelPosition)) {
					// Figure out how much of the screen this line covers.
					val nextLineScreenCoverage = nextLine.screenCoverage(mSongPixelPosition)
					pageDownPosition =
						when {
							// If it takes up an enormous amount of screen, we'll just do a default scroll.
							nextLineScreenCoverage > MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> defaultPageDownScrollPosition
							// If it takes up a reasonable amount of screen, we'll use it.
							nextLineScreenCoverage >= MINIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> nextLine.mSongPixelPosition
							// Last possibility: it wasn't onscreen enough, so probably isn't very readable.
							// We've still got the previous line "in the bank", so we'll use that unless THAT
							// line takes up a huge amount of the screen ...
							pageDownLine.screenCoverage(mSongPixelPosition) <= MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> pageDownPosition
							// ...in which case, we will just do a default scroll.
							else -> defaultPageDownScrollPosition
						}
					break
				}
				// Move to the next line.
				pageDownLine = nextLine
				pageDownPosition = pageDownLine.mSongPixelPosition
			}

			// Phew! Got there in the end.
			// Never scroll beyond the pre-calculated end point (though this should never happen).
			pageDownPosition = min(mSong!!.mScrollEndPixel, pageDownPosition)

			mManualScrollPositions.mPageUpPosition = pageUpPosition
			mManualScrollPositions.mPageDownPosition = pageDownPosition
			mManualScrollPositions.mBeatJumpScrollLine = beatJumpScrollLine
		}
	}

	fun setSongBeatPosition(pointer: Int, midiInitiated: Boolean) {
		val songTime = mSong!!.getMIDIBeatTime(pointer)
		setSongTime(
			songTime,
			true,
			midiInitiated,
			setPixelPosition = true,
			recalculateManualPositions = true
		)
	}

	fun startSong(midiInitiated: Boolean, fromStart: Boolean) {
		if (fromStart)
			setSongTime(
				0,
				true,
				midiInitiated,
				setPixelPosition = true,
				recalculateManualPositions = true
			)
		while (mStartState !== PlayState.Playing)
			startToggle(null, midiInitiated)
	}

	fun stopSong(midiInitiated: Boolean) {
		if (mStartState === PlayState.Playing)
			startToggle(null, midiInitiated)
	}

	internal fun canYieldToExternalTrigger(): Boolean {
		return when (mExternalTriggerSafetyCatch) {
			TriggerSafetyCatch.Always -> true
			TriggerSafetyCatch.WhenAtTitleScreen -> mStartState === PlayState.AtTitleScreen
			TriggerSafetyCatch.WhenAtTitleScreenOrPaused -> mStartState !== PlayState.Playing || mSong != null && mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual
			TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine -> mStartState !== PlayState.Playing || mSong == null || mSong!!.mCurrentLine.mNextLine == null || mSong!!.mCurrentLine.mScrollMode === ScrollingMode.Manual
			TriggerSafetyCatch.Never -> false
		}
	}

	private fun getLineHighlightColor(line: Line, time: Long): Int? {
		if (line == mSong!!.mCurrentLine && mHighlightCurrentLine && line.mScrollMode == ScrollingMode.Beat)
			return mDefaultCurrentLineHighlightColor
		if (mHighlightBeatSectionStart && line == mManualScrollPositions.mBeatJumpScrollLine)
			return mBeatSectionStartHighlightColors[((time / 1000000.0) % mBeatSectionStartHighlightColors.size).toInt()]
		return null
	}

	internal inner class ManualMetronomeTask(bpm: Double) : Task(true) {
		private var mNanosecondsPerBeat: Long = 0
		private var mNextClickTime: Long = 0

		init {
			mNanosecondsPerBeat = Utils.nanosecondsPerBeat(bpm)
		}

		override fun doWork() {
			mNextClickTime = System.nanoTime()
			mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f)
			mNextClickTime += mNanosecondsPerBeat
			val wait = mNextClickTime - System.nanoTime()
			if (wait > 0) {
				val millisecondsPerBeat = Utils.nanoToMilli(wait).toLong()
				val nanosecondRemainder = (wait - Utils.milliToNano(millisecondsPerBeat)).toInt()
				try {
					Thread.sleep(millisecondsPerBeat, nanosecondRemainder)
				} catch (ie: InterruptedException) {
					Logger.log("Interrupted while waiting ... assuming resync attempt.", ie)
					mNextClickTime = System.nanoTime()
				}
			}
		}
	}

	data class ManualScrollPositions(
		var mPageUpPosition: Int,
		var mPageDownPosition: Int,
		var mBeatJumpScrollLine: Line?
	) {
		constructor() : this(0, 0, null)
	}

	enum class TriggerSafetyCatch {
		Never, Always, WhenAtTitleScreen, WhenAtTitleScreenOrPaused, WhenAtTitleScreenOrPausedOrLastLine
	}

	companion object {
		private val SongViewAudioAttributes = AudioAttributes
			.Builder()
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
			.build()

		private const val SONG_END_PEDAL_PRESSES = 3
		private val SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS = Utils.milliToNano(2000)
		private val mAccelerations = IntArray(2048)

		// Expressed as a fraction, this is the percentage of the usable screen that will be scrolled
		// by default when we have stupidly-big lines.
		private const val DEFAULT_PAGE_SCROLL_AMOUNT = 0.85

		// If the candidate manual scroll line occupies less than 5% of the screen, we will not
		// scroll to it, and will instead scroll to the one above.
		private const val MINIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL = 0.08

		// If the candidate manual scroll line is taking up at least 60% of the screen, we will not
		// scroll to it, and will instead perform the default 90% screen scroll
		private const val MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL = 0.6

		// If the candidate beat jumpscroll line occupies less than 10% of the screen, we will not
		// scroll to it.
		private const val MINIMUM_SCREEN_COVERAGE_FOR_BEAT_SCROLL = 0.1

		init {
			repeat(2048) {
				mAccelerations[it] = ceil(sqrt((it + 1).toDouble()) * 2.0).toInt()
			}
		}

		private fun createStrobingHighlightColourArray(startColor: Int): IntArray {
			val colourArray = IntArray(512)
			repeat(256) {
				colourArray[it] = Utils.makeHighlightColour(startColor, (it / 2).toByte())
				colourArray[511 - it] = colourArray[it]
			}
			return colourArray
		}
	}
}