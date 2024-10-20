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
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.message.PauseOnScrollStartMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.QuitSongMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.SetSongTimeMessage
import com.stevenfrew.beatprompter.comm.bluetooth.message.ToggleStartStopMessage
import com.stevenfrew.beatprompter.comm.midi.Midi
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.song.PlayState
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.Song
import com.stevenfrew.beatprompter.song.event.AudioEvent
import com.stevenfrew.beatprompter.song.event.BeatEvent
import com.stevenfrew.beatprompter.song.event.ClickEvent
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

	private val destinationGraphicRect = Rect(0, 0, 0, 0)
	private var currentBeatCountRect = Rect()
	private var endSongByPedalCounter = 0
	private var metronomeOn: Boolean = false
	private var initialized = false
	private var skipping = false
	private var currentVolume = Preferences.defaultTrackVolume
	private var lastCommentEvent: CommentEvent? = null
	private var lastCommentTime: Long = 0
	private var lastTempMessageTime: Long = 0
	private var lastBeatTime: Long = 0
	private val paint = Paint()
	private val scroller: OverScroller
	private val metronomePref: MetronomeContext

	private var manualMetronomeTask: ManualMetronomeTask? = null
	private var manualMetronomeThread: Thread? = null

	private var song: Song? = null

	private var songStartTime: Long = 0
	private var startState = PlayState.AtTitleScreen
	private var userHasScrolled = false
	private var pauseTime: Long = 0
	private var nanosecondsPerBeat = Utils.nanosecondsPerBeat(120.0)

	private val backgroundColorLookup = IntArray(101)
	private val commentTextColor: Int
	private val pageDownMarkerColor: Int
	private val beatCounterColor: Int
	private val defaultCurrentLineHighlightColor: Int
	private val beatSectionStartHighlightColors: IntArray
	private val defaultPageDownLineHighlightColor: Int
	private val showScrollIndicator: Boolean
	private val showSongTitle: Boolean
	private val commentDisplayTimeNanoseconds: Long
	private val audioPlayerFactory: AudioPlayerFactory
	private var audioPlayers = mapOf<AudioFile, AudioPlayer>()
	private val silenceAudioPlayer: AudioPlayer

	private var pulse: Boolean
	private var songPixelPosition = 0
	private var targetPixelPosition = -1
	private var targetAcceleration = 1
	private val highlightCurrentLine: Boolean
	private val highlightBeatSectionStart: Boolean
	private val showPageDownMarker: Boolean
	private var songTitleContrastBackground: Int = 0
	private var songTitleContrastBeatCounter: Int = 0
	private val scrollIndicatorRect = Rect()
	private var gestureDetector: GestureDetectorCompat? = null
	private var screenAction = ScreenAction.Scroll
	private val scrollMarkerColor: Int
	private var songDisplayActivity: SongDisplayActivity? = null
	private val externalTriggerSafetyCatch: TriggerSafetyCatch
	private val manualScrollPositions: ManualScrollPositions = ManualScrollPositions()

	private val clickSoundPool: SoundPool = SoundPool
		.Builder()
		.setMaxStreams(16)
		.setAudioAttributes(SongViewAudioAttributes)
		.build()
	private val clickAudioId = clickSoundPool.load(context, R.raw.click, 0)

	enum class ScreenAction {
		Scroll, Volume, None
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		scroller = OverScroller(context)
		gestureDetector = GestureDetectorCompat(context, this)
		songPixelPosition = 0

		audioPlayerFactory = AudioPlayerFactory(Preferences.audioPlayer, context)
		silenceAudioPlayer = audioPlayerFactory.createSilencePlayer()

		screenAction = Preferences.screenAction
		showScrollIndicator = Preferences.showScrollIndicator
		showSongTitle = Preferences.showSongTitle
		val commentDisplayTimeSeconds = Preferences.commentDisplayTime
		commentDisplayTimeNanoseconds = Utils.milliToNano(commentDisplayTimeSeconds * 1000)
		externalTriggerSafetyCatch = Preferences.midiTriggerSafetyCatch
		highlightCurrentLine = Preferences.highlightCurrentLine
		showPageDownMarker = Preferences.showPageDownMarker
		highlightBeatSectionStart = Preferences.highlightBeatSectionStart
		beatCounterColor = Preferences.beatCounterColor
		commentTextColor = Preferences.commentColor
		pageDownMarkerColor = Preferences.pageDownMarkerColor
		scrollMarkerColor = Preferences.scrollIndicatorColor
		val mHighlightBeatSectionStartColor = Preferences.beatSectionStartHighlightColor
		beatSectionStartHighlightColors =
			createStrobingHighlightColourArray(mHighlightBeatSectionStartColor)

		defaultCurrentLineHighlightColor =
			Utils.makeHighlightColour(Preferences.currentLineHighlightColor)
		defaultPageDownLineHighlightColor = Utils.makeHighlightColour(Preferences.pageDownMarkerColor)
		pulse = Preferences.pulseDisplay
		metronomePref = if (Preferences.mute) MetronomeContext.Off else Preferences.metronomeContext

		songTitleContrastBeatCounter = Utils.makeContrastingColour(beatCounterColor)
		val backgroundColor = Preferences.backgroundColor
		val pulseColor =
			if (pulse)
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
			backgroundColorLookup[it] = color
		}
		songTitleContrastBackground = Utils.makeContrastingColour(backgroundColorLookup[100])
	}

	// Constructor
	constructor(context: Context) : this(context, null)

	fun init(songDisplayActivity: SongDisplayActivity, song: Song) {
		this.songDisplayActivity = songDisplayActivity

		if (song.songFile.bpm > 0.0) {
			val metronomeOnPref = metronomePref == MetronomeContext.On
			val metronomeOnWhenNoBackingTrackPref = metronomePref == MetronomeContext.OnWhenNoTrack
			// We switch the metronome on (full time) if the pref says "on"
			// or if it says "when no audio track" and there are no audio events.
			metronomeOn =
				metronomeOnPref || (metronomeOnWhenNoBackingTrackPref && !song.audioEvents.any())
		}

		if (song.audioEvents.any())
			silenceAudioPlayer.start()

		val audioPlayerMap = song.audioEvents.associateBy(
			{ it.audioFile },
			{
				try {
					audioPlayerFactory.create(it.audioFile.file, it.volume)
				} catch (e: Exception) {
					null
				}
			}
		)

		if (audioPlayerMap.any { it.value == null })
			Toast.makeText(context, R.string.crap_audio_file_warning, Toast.LENGTH_LONG).show()

		@Suppress("UNCHECKED_CAST")
		audioPlayers = audioPlayerMap.filterValues { it != null } as Map<AudioFile, AudioPlayer>

		// It has been noticed that briefly starting the audio player (near-silently)
		// will improve startup responsiveness.
		audioPlayers.values.forEach {
			val startVolume = it.volume
			it.volume = 1
			it.start()
			Thread.sleep(1)
			it.pause()
			it.volume = startVolume
			it.seekTo(0)
		}

		currentBeatCountRect = Rect(song.beatCounterRect)
		this.song = song

		calculateManualScrollPositions()
	}

	private fun ensureInitialised() = song?.apply {
		if (!initialized) {
			if (smoothMode)
				pulse = false
			initialized = true
			if (manualMode) {
				if (metronomeOn) {
					manualMetronomeTask = ManualMetronomeTask(songFile.bpm)
					manualMetronomeThread = Thread(manualMetronomeTask).apply { start() }
				}
			}
		}
	}

	// Called back to draw the view. Also called by invalidate().
	override fun onDraw(canvas: Canvas) {
		song?.apply {
			ensureInitialised()
			val scrolling = calculateScrolling()
			var timePassed: Long = 0
			var beatPercent = 1.0
			var showTempMessage = false
			var showComment = false
			val time = System.nanoTime()

			if (startState === PlayState.Playing && !scrolling) {
				timePassed = max(0, time - songStartTime)
				if (lastBeatTime > 0) {
					val beatTimePassed = max(0, time - lastBeatTime)
					val beatTime = (beatTimePassed % nanosecondsPerBeat).toDouble()
					beatPercent = beatTime / nanosecondsPerBeat
				}

				if (currentLine.scrollMode !== ScrollingMode.Manual)
					if (processSongEvents(time, timePassed))
						return

				showTempMessage = time - lastTempMessageTime < SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS
				if (lastCommentEvent != null)
					if (time - lastCommentTime < commentDisplayTimeNanoseconds)
						showComment = true
			}
			var currentY = beatCounterRect.height() + displayOffset
			var currentLine = currentLine
			var yScrollOffset = 0
			if (currentLine.scrollMode !== ScrollingMode.Beat)
				beatPercent = 1.0
			val color = backgroundColorLookup[(beatPercent * 100.0).toInt()]
			canvas.drawColor(color, PorterDuff.Mode.SRC)
			if (startState !== PlayState.AtTitleScreen) {
				var scrollPercentage = 0.0
				// If a scroll event in underway, move currentY up
				if (startState !== PlayState.Playing || currentLine.scrollMode === ScrollingMode.Manual) {
					yScrollOffset = songPixelPosition - currentLine.songPixelPosition
					if (currentLine.scrollMode === ScrollingMode.Smooth)
						scrollPercentage =
							yScrollOffset.toDouble() / currentLine.measurements.lineHeight.toDouble()
				} else {
					if (!scrolling) {
						if (!noScrollLines.contains(currentLine)) {
							if (currentLine.yStopScrollTime > timePassed && currentLine.yStartScrollTime <= timePassed)
								scrollPercentage =
									(timePassed - currentLine.yStartScrollTime).toDouble() / (currentLine.yStopScrollTime - currentLine.yStartScrollTime).toDouble()
							else if (currentLine.yStopScrollTime <= timePassed)
								scrollPercentage = 1.0
							// In smooth mode, if we're on the last line, prevent it scrolling up more than necessary ... i.e. keep as much song onscreen as possible.
							if (currentLine.scrollMode === ScrollingMode.Smooth) {
								val remainingSongHeight = height - currentLine.songPixelPosition
								val remainingScreenHeight = displaySettings.screenSize.height() - currentY
								yScrollOffset = min(
									(currentLine.measurements.lineHeight * scrollPercentage).toInt(),
									remainingSongHeight - remainingScreenHeight
								)
							} else if (currentLine.scrollMode === ScrollingMode.Beat)
								yScrollOffset =
									currentLine.measurements.jumpScrollIntervals[(scrollPercentage * 100.0).toInt()]
						}
					}
				}

				currentY -= yScrollOffset
				if (startState === PlayState.Playing)
					songPixelPosition = currentLine.songPixelPosition + yScrollOffset

				val startY = currentY
				var firstLineOnscreen: Line? = null
				while (currentY < displaySettings.screenSize.height()) {
					if (currentY > beatCounterRect.height() - currentLine.measurements.lineHeight) {
						if (firstLineOnscreen == null)
							firstLineOnscreen = currentLine
						val graphics = currentLine.getGraphics(paint)
						val lineTop = currentY
						for (f in graphics.indices) {
							val graphic = graphics[f]
							val sourceRect = currentLine.measurements.graphicRectangles[f]
							destinationGraphicRect.set(sourceRect)
							destinationGraphicRect.offset(0, currentY)
							canvas.drawBitmap(graphic.bitmap, sourceRect, destinationGraphicRect, paint)
							currentY += currentLine.measurements.graphicHeights[f]
						}
						val highlightColor = getLineHighlightColor(currentLine, time)
						if (highlightColor != null) {
							paint.color = highlightColor
							canvas.drawRect(
								0f,
								lineTop.toFloat(),
								displaySettings.screenSize.width().toFloat(),
								(lineTop + currentLine.measurements.lineHeight).toFloat(),
								paint
							)
							paint.alpha = 255
						}

					} else
						currentY += currentLine.measurements.lineHeight
					if (currentLine.nextLine == null)
						break
					currentLine = currentLine.nextLine!!
				}

				if (smoothMode) {
					val prevLine = currentLine.previousLine
					if (prevLine != null && startY > 0) {
						paint.alpha = (255.0 - 255.0 * scrollPercentage).toInt()
						currentY = startY - prevLine.measurements.lineHeight
						val graphics = prevLine.getGraphics(paint)
						for (f in graphics.indices) {
							val graphic = graphics[f]
							canvas.drawBitmap(graphic.bitmap, 0f, currentY.toFloat(), paint)
							currentY += prevLine.measurements.graphicHeights[f]
						}
						paint.alpha = 255
					}
				}
			}
			paint.color = backgroundColorLookup[100]
			canvas.drawRect(beatCounterRect, paint)
			paint.color = scrollMarkerColor
			if (currentLine.scrollMode === ScrollingMode.Beat && showScrollIndicator)
				canvas.drawRect(scrollIndicatorRect, paint)
			paint.color = beatCounterColor
			paint.strokeWidth = 1.0f
			canvas.drawRect(currentBeatCountRect, paint)
			canvas.drawLine(
				0f,
				beatCounterRect.height().toFloat(),
				displaySettings.screenSize.width().toFloat(),
				beatCounterRect.height().toFloat(),
				paint
			)
			if (showPageDownMarker)
				showPageDownMarkers(canvas)
			if (showSongTitle)
				showSongTitle(canvas)
			if (showTempMessage) {
				if (endSongByPedalCounter == 0)
					showTempMessage("$currentVolume%", 80, Color.BLACK, canvas)
				else {
					val message =
						"Press pedal " + (SONG_END_PEDAL_PRESSES - endSongByPedalCounter) + " more times to end song."
					showTempMessage(message, 20, Color.BLUE, canvas)
				}
			} else
				endSongByPedalCounter = 0
			if (showComment)
				showComment(canvas)
			if (startState !== PlayState.AtTitleScreen)
				invalidate()  // Force a re-draw
			else if (song != null)
				drawTitleScreen(canvas)
		}
	}

	private fun processSongEvents(time: Long, timePassed: Long): Boolean {
		var event: LinkedEvent?
		do {
			event = song!!.getNextEvent(timePassed)
			if (event != null) {
				when (val innerEvent = event.event) {
					is CommentEvent -> processCommentEvent(innerEvent, time)
					is BeatEvent -> currentBeatCountRect = processBeatEvent(innerEvent/*, true*/)
					is ClickEvent -> processClickEvent()
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

	private fun calculateScrolling(): Boolean =
		if (startState === PlayState.AtTitleScreen)
			false
		else if ((screenAction == ScreenAction.Scroll || song!!.currentLine.scrollMode === ScrollingMode.Manual) && scroller.computeScrollOffset()) {
			songPixelPosition = scroller.currY
			val songTime = song!!.currentLine.getTimeFromPixel(songPixelPosition)
			setSongTime(
				songTime,
				startState === PlayState.Paused,
				broadcast = true,
				setPixelPosition = false,
				recalculateManualPositions = true
			)
			true
		} else {
			if (targetPixelPosition != -1 && targetPixelPosition != songPixelPosition) {
				val diff = min(2048, max(-2048, targetPixelPosition - songPixelPosition))
				val absDiff = abs(diff)
				val targetAccelerationNow = min(accelerations[absDiff - 1], absDiff)
				if (targetAcceleration * 2 < targetAccelerationNow)
					targetAcceleration *= 2
				else
					targetAcceleration = targetAccelerationNow
				if (diff > 0)
					songPixelPosition += targetAcceleration
				else
					songPixelPosition -= targetAcceleration
				if (songPixelPosition == targetPixelPosition)
					clearScrollTarget()
				val songTime = song!!.currentLine.getTimeFromPixel(songPixelPosition)
				setSongTime(
					songTime,
					startState === PlayState.Paused,
					broadcast = true,
					setPixelPosition = false,
					recalculateManualPositions = false
				)
			}
			false
		}

	private fun drawTitleScreen(canvas: Canvas) {
		canvas.drawColor(Color.BLACK)
		val midX = song!!.displaySettings.screenSize.width() shr 1
		val fifteenPercent = song!!.displaySettings.screenSize.height() * 0.15f
		var startY =
			floor(((song!!.displaySettings.screenSize.height() - song!!.totalStartScreenTextHeight) / 2).toDouble()).toInt()
		val nextSongSS = song!!.nextSongString
		if (nextSongSS != null) {
			paint.color = if (skipping) Color.RED else Color.WHITE
			val halfDiff = (fifteenPercent - nextSongSS.height) / 2.0f
			canvas.drawRect(
				0f,
				song!!.displaySettings.screenSize.height() - fifteenPercent,
				song!!.displaySettings.screenSize.width().toFloat(),
				song!!.displaySettings.screenSize.height().toFloat(),
				paint
			)
			val nextSongY =
				song!!.displaySettings.screenSize.height() - (nextSongSS.descenderOffset + halfDiff).toInt()
			startY -= (fifteenPercent / 2.0f).toInt()
			paint.apply {
				color = nextSongSS.color
				textSize = nextSongSS.fontSize * Utils.FONT_SCALING
				typeface = nextSongSS.typeface
				flags = Paint.ANTI_ALIAS_FLAG
			}
			canvas.drawText(
				nextSongSS.text,
				(midX - (nextSongSS.width shr 1)).toFloat(),
				nextSongY.toFloat(),
				paint
			)
		}
		for (ss in song!!.startScreenStrings) {
			startY += ss.height
			paint.apply {
				color = ss.color
				textSize = ss.fontSize * Utils.FONT_SCALING
				typeface = ss.typeface
				flags = Paint.ANTI_ALIAS_FLAG
			}
			canvas.drawText(
				ss.text,
				(midX - (ss.width shr 1)).toFloat(),
				(startY - ss.descenderOffset).toFloat(),
				paint
			)
		}
	}

	private fun showTempMessage(message: String, textSize: Int, textColor: Int, canvas: Canvas) {
		val popupMargin = 25
		paint.strokeWidth = 2.0f
		paint.textSize = textSize * Utils.FONT_SCALING
		paint.flags = Paint.ANTI_ALIAS_FLAG
		val outRect = Rect()
		paint.getTextBounds(message, 0, message.length, outRect)
		val textWidth = paint.measureText(message)
		val textHeight = outRect.height()
		val volumeControlWidth = textWidth + popupMargin * 2.0f
		val volumeControlHeight = textHeight + popupMargin * 2
		val x = (song!!.displaySettings.screenSize.width() - volumeControlWidth) / 2.0f
		val y = (song!!.displaySettings.screenSize.height() - volumeControlHeight) / 2
		paint.color = Color.BLACK
		canvas.drawRect(
			x,
			y.toFloat(),
			x + volumeControlWidth,
			(y + volumeControlHeight).toFloat(),
			paint
		)
		paint.color = Color.rgb(255, 255, 200)
		canvas.drawRect(
			x + 1,
			(y + 1).toFloat(),
			x + (volumeControlWidth - 2),
			(y + (volumeControlHeight - 2)).toFloat(),
			paint
		)
		paint.color = textColor
		canvas.drawText(
			message,
			(song!!.displaySettings.screenSize.width() - textWidth) / 2,
			((song!!.displaySettings.screenSize.height() - textHeight) / 2 + textHeight).toFloat(),
			paint
		)
	}

	private fun showPageDownMarkers(canvas: Canvas) {
		if (song!!.currentLine.scrollMode == ScrollingMode.Manual && songPixelPosition < song!!.scrollEndPixel) {
			val scrollPosition =
				((manualScrollPositions.mPageDownPosition - songPixelPosition) + song!!.displaySettings.beatCounterRect.height()).toFloat()
			val screenHeight = song!!.displaySettings.screenSize.height().toFloat()
			val screenWidth = song!!.displaySettings.screenSize.width().toFloat()
			val lineSize = screenWidth / 10.0f

			paint.strokeWidth = (screenWidth + screenHeight) / 200.0f
			paint.color = pageDownMarkerColor
			canvas.drawLine(0.0f, scrollPosition + lineSize, 0.0f, scrollPosition, paint)
			canvas.drawLine(0.0f, scrollPosition, lineSize, scrollPosition, paint)
			canvas.drawLine(screenWidth, scrollPosition + lineSize, screenWidth, scrollPosition, paint)
			canvas.drawLine(screenWidth, scrollPosition, screenWidth - lineSize, scrollPosition, paint)
		}
	}

	private fun showSongTitle(canvas: Canvas) = song?.apply {
		paint.apply {
			textSize = songTitleHeader.fontSize * Utils.FONT_SCALING
			typeface = songTitleHeader.typeface
			flags = Paint.ANTI_ALIAS_FLAG
			color = songTitleContrastBackground
		}

		canvas.drawText(
			songTitleHeader.text,
			songTitleHeaderLocation.x,
			songTitleHeaderLocation.y,
			paint
		)

		canvas.save()
		canvas.clipRect(currentBeatCountRect)
		paint.color = songTitleContrastBeatCounter
		canvas.drawText(
			songTitleHeader.text,
			songTitleHeaderLocation.x,
			songTitleHeaderLocation.y,
			paint
		)
		canvas.restore()

		canvas.save()
		canvas.clipRect(scrollIndicatorRect)
		canvas.drawText(
			songTitleHeader.text,
			songTitleHeaderLocation.x,
			songTitleHeaderLocation.y,
			paint
		)
		canvas.restore()

		paint.alpha = 255
	}

	private fun showComment(canvas: Canvas) {
		if (lastCommentEvent != null)
			lastCommentEvent!!.comment.draw(canvas, paint, commentTextColor)
	}

	private fun startToggle(playState: PlayState) {
		startState = playState
		startToggle(null, false)
	}

	private fun startAudioPlayer(audioPlayer: AudioPlayer?): AudioPlayer? =
		audioPlayer?.apply {
			Logger.log("Starting AudioPlayer")
			start()
			currentVolume = volume
		}

	private fun startBackingTrack(): Boolean =
		startAudioPlayer(audioPlayers[song!!.backingTrack]) != null

	private fun startToggle(e: MotionEvent?, midiInitiated: Boolean): Boolean {
		song?.apply {
			if (startState !== PlayState.Playing) {
				if (startState === PlayState.AtTitleScreen)
					if (e != null)
						if (e.y > displaySettings.screenSize.height() * 0.85f)
							if (nextSong.isNotBlank()) {
								endSong(true)
								return true
							}
				val oldPlayState = startState
				startState = PlayState.increase(startState)
				if (startState === PlayState.Playing) {
					if (currentLine.scrollMode === ScrollingMode.Manual) {
						// Top of the page? Start. Else it's a continue.
						if (songPixelPosition == 0) Midi.putStartMessage() else Midi.putContinueMessage()
						// Start the count in.
						if (manualMetronomeThread != null) {
							if (!manualMetronomeThread!!.isAlive) {
								return if (metronomeOn) {
									manualMetronomeThread!!.start()
									true
								} else
									startBackingTrack()
							}
						} else
							return startBackingTrack()
					} else {
						val time: Long
						if (userHasScrolled) {
							userHasScrolled = false
							time = getTimeFromPixel(songPixelPosition)
							setSongTime(
								time,
								redraw = false,
								broadcast = false,
								setPixelPosition = false,
								recalculateManualPositions = true
							)
						} else {
							// Zero-time index? Start. Else it's a continue.
							Logger.log({ "Resuming, pause time=$pauseTime" })
							time = pauseTime
							setSongTime(
								time,
								redraw = false,
								broadcast = false,
								setPixelPosition = true,
								recalculateManualPositions = true
							)
						}
						if (time == 0L) Midi.putStartMessage() else Midi.putContinueMessage()
						Bluetooth.putMessage(
							ToggleStartStopMessage(
								ToggleStartStopMessage.StartStopToggleInfo(
									oldPlayState,
									time
								)
							)
						)
					}
				} else
					Bluetooth.putMessage(
						ToggleStartStopMessage(
							ToggleStartStopMessage.StartStopToggleInfo(
								oldPlayState,
								0
							)
						)
					)
			} else {
				if (screenAction == ScreenAction.Volume) {
					if (e != null) {
						if (e.y < displaySettings.screenSize.height() * 0.5)
							changeVolume(+5)
						else if (e.y > displaySettings.screenSize.height() * 0.5)
							changeVolume(-5)
					}
				} else if (currentLine.scrollMode !== ScrollingMode.Manual) {
					if (screenAction == ScreenAction.Scroll)
						pause(midiInitiated)
				}
			}
			invalidate()
		}
		return true
	}

	private fun changeVolume(amount: Int) {
		if (startState === PlayState.Paused)
			return
		currentVolume += amount
		onVolumeChanged()
	}

	fun pause(midiInitiated: Boolean) {
		if (screenAction != ScreenAction.Scroll)
			return
		val nanoTime = System.nanoTime()
		pauseTime = nanoTime - if (songStartTime == 0L) nanoTime else songStartTime
		Bluetooth.putMessage(
			ToggleStartStopMessage(
				ToggleStartStopMessage.StartStopToggleInfo(
					startState,
					pauseTime
				)
			)
		)
		startState = PlayState.reduce(startState)
		audioPlayers.values.forEach {
			if (it.isPlaying)
				it.pause()
		}
		if (!midiInitiated)
			songDisplayActivity?.onSongPaused()
	}

	fun stop(destroyed: Boolean) {
		if (startState === PlayState.Playing)
			pause(false)
		if (destroyed) {
			song?.apply {
				Bluetooth.putMessage(
					QuitSongMessage(
						songFile.normalizedTitle,
						songFile.normalizedArtist
					)
				)
				recycleGraphics()
			}
			song = null
			Task.stopTask(manualMetronomeTask, manualMetronomeThread)
			audioPlayers.values.forEach {
				it.stop()
				it.release()
			}
			silenceAudioPlayer.stop()
			silenceAudioPlayer.release()
			clickSoundPool.release()
			System.gc()
		}
	}

	private fun processCommentEvent(event: CommentEvent, systemTime: Long) {
		lastCommentTime = systemTime
		lastCommentEvent = event
	}

	private fun processClickEvent() {
		val playClick = metronomePref !== MetronomeContext.OnWhenNoTrack || !isTrackPlaying()
		if (startState === PlayState.Playing && song!!.currentLine.scrollMode !== ScrollingMode.Manual && playClick)
			clickSoundPool.play(clickAudioId, 1.0f, 1.0f, 1, 0, 1.0f)
	}

	private fun processBeatEvent(event: BeatEvent): Rect {
		nanosecondsPerBeat = Utils.nanosecondsPerBeat(event.bpm)
		val beatWidth = song!!.displaySettings.screenSize.width().toDouble() / event.bpb.toDouble()
		val currentBeatCounterWidth = (beatWidth * (event.beat + 1).toDouble()).toInt()
		if (event.willScrollOnBeat != -1) {
			val thirdWidth = beatWidth / 3
			val thirdHeight = song!!.beatCounterRect.height() / 3.0
			val scrollIndicatorStart = (beatWidth * event.willScrollOnBeat + thirdWidth).toInt()
			val scrollIndicatorEnd = (beatWidth * (event.willScrollOnBeat + 1) - thirdWidth).toInt()
			scrollIndicatorRect.apply {
				left = scrollIndicatorStart
				top = thirdHeight.toInt()
				right = scrollIndicatorEnd
				bottom = (thirdHeight * 2.0).toInt()
			}
		} else
			clearScrollIndicatorRect()
		lastBeatTime = songStartTime + event.eventTime
		songDisplayActivity?.onSongBeat(event.bpm)
		return if (song!!.currentLine.scrollMode == ScrollingMode.Beat)
			Rect().apply {
				left = (currentBeatCounterWidth - beatWidth).toInt()
				top = 0
				right = currentBeatCounterWidth
				bottom = song!!.beatCounterRect.height()
			}
		else
			Rect(song!!.beatCounterRect)
	}

	private fun isTrackPlaying(): Boolean = audioPlayers.values.any { it.isPlaying }

	fun hasSong(title: String, artist: String): Boolean =
		song?.songFile?.normalizedArtist == artist && song?.songFile?.normalizedTitle == title

	private fun processPauseEvent(event: PauseEvent) {
		lastBeatTime = -1
		val currentBeatCounterWidth = (song!!.displaySettings.screenSize.width()
			.toDouble() / (event.beats - 1).toDouble() * event.beat.toDouble()).toInt()
		currentBeatCountRect.apply {
			left = 0
			top = 0
			right = currentBeatCounterWidth
			bottom = song!!.beatCounterRect.height()
		}
		clearScrollIndicatorRect()
	}

	private fun clearScrollIndicatorRect() {
		scrollIndicatorRect.apply {
			left = -1
			top = -1
			right = -1
			bottom = -1
		}
	}

	private fun processMIDIEvent(event: MIDIEvent) =
		event.messages.forEach {
			Midi.putMessage(it)
		}

	private fun processLineEvent(event: LineEvent) = song?.apply {
		currentLine = event.line
		if (currentLine.scrollMode == ScrollingMode.Manual) {
			currentBeatCountRect = Rect(beatCounterRect)
			calculateManualScrollPositions()
		}
	}

	private fun processAudioEvent(event: AudioEvent): Boolean {
		val audioPlayer = audioPlayers[event.audioFile] ?: return false
		Logger.log("Track event hit: starting AudioPlayer")
		audioPlayer.seekTo(0)
		startAudioPlayer(audioPlayer)
		return true
	}

	// Only end the song in non-manual mode.
	private fun processEndEvent() = endSong(false)

	private fun endSong(skipped: Boolean) {
		if (songDisplayActivity != null) {
			skipping = skipped
			SongListFragment.mSongEndedNaturally = true
			startState = PlayState.AtTitleScreen
			songDisplayActivity = null
			song?.recycleGraphics()
			song = null
			EventRouter.sendEventToSongDisplay(Events.END_SONG)
			System.gc()
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		gestureDetector!!.onTouchEvent(event)
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
		val audioPlayer = audioPlayers[audioFile]
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
	) = song?.apply {
		// No time context in Manual mode.
		if (setPixelPosition)
			songPixelPosition = getPixelFromTime(nano)
		run {
			if (startState !== PlayState.Playing)
				pauseTime = nano
			if (broadcast)
				Bluetooth.putMessage(SetSongTimeMessage(nano))
			setProgress(nano)
			//var musicPlaying = false
			if (currentLine.scrollMode !== ScrollingMode.Manual) {
				val audioEvent = currentEvent.previousAudioEvent
				if (audioEvent != null) {
					val nTime = Utils.nanoToMilli(nano - audioEvent.eventTime)
					/*musicPlaying = */
					seekTrack(
						audioEvent.audioFile,
						nTime
					)?.apply {
						if (startState === PlayState.Playing)
							startAudioPlayer(this)
					} != null
				}
			}
			val prevBeatEvent =
				if (currentLine.scrollMode !== ScrollingMode.Manual) currentEvent.previousBeatEvent else null
			currentBeatCountRect =
				//val nextBeatEvent = mCurrentEvent.mNextBeatEvent
				if (prevBeatEvent == null) Rect(beatCounterRect) else processBeatEvent(prevBeatEvent/*, nextBeatEvent != null && !musicPlaying*/)
			songStartTime = System.nanoTime() - nano
			if (redraw)
				invalidate()
		}
		if (recalculateManualPositions)
			calculateManualScrollPositions()
	}

	override fun onDown(e: MotionEvent): Boolean {
		if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			if (manualMetronomeThread != null)
				if (startState === PlayState.Playing)
					manualMetronomeThread!!.interrupt()
		// Abort any active scroll animations and invalidate.
		if (screenAction == ScreenAction.Scroll || song!!.currentLine.scrollMode === ScrollingMode.Manual)
			clearScrollTarget()
		scroller.forceFinished(true)
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
		if (screenAction == ScreenAction.None || startState === PlayState.AtTitleScreen || song == null)
			return false
		if (screenAction == ScreenAction.Scroll || song!!.currentLine.scrollMode === ScrollingMode.Manual) {
			clearScrollTarget()
			songPixelPosition += distanceY.toInt()
			songPixelPosition = max(0, songPixelPosition)
			songPixelPosition = min(song!!.scrollEndPixel, songPixelPosition)
			pauseOnScrollStart()
			setSongTime(
				song!!.currentLine.getTimeFromPixel(songPixelPosition),
				redraw = true,
				broadcast = true,
				setPixelPosition = false,
				recalculateManualPositions = true
			)
		} else if (screenAction == ScreenAction.Volume) {
			currentVolume += (distanceY / 10.0).toInt()
			onVolumeChanged()
		}
		return true
	}

	fun pauseOnScrollStart() {
		if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			return
		if (screenAction != ScreenAction.Scroll)
			return
		Bluetooth.putMessage(PauseOnScrollStartMessage)
		userHasScrolled = true
		startState = PlayState.Paused
		audioPlayers.values.forEach {
			Logger.log("Pausing AudioPlayers")
			if (it.isPlaying)
				it.pause()
		}
		songDisplayActivity?.onSongPaused()
	}

	private fun onVolumeChanged() {
		currentVolume = max(0, currentVolume)
		currentVolume = min(100, currentVolume)
		audioPlayers.values.forEach {
			it.volume = currentVolume
		}
		lastTempMessageTime = System.nanoTime()
	}

	override fun onLongPress(e: MotionEvent) {}

	override fun onFling(
		e1: MotionEvent?,
		e2: MotionEvent,
		velocityX: Float,
		velocityY: Float
	): Boolean {
		if (screenAction == ScreenAction.None)
			return false
		if (startState === PlayState.AtTitleScreen)
			return false
		if (song == null)
			return false
		if (screenAction == ScreenAction.Scroll || song!!.currentLine.scrollMode === ScrollingMode.Manual) {
			clearScrollTarget()
			pauseOnScrollStart()
			scroller.fling(
				0,
				songPixelPosition,
				0,
				(-velocityY).toInt(),
				0,
				0,
				0,
				song!!.scrollEndPixel
			)
		} else if (screenAction == ScreenAction.Volume)
			scroller.fling(0, currentVolume, 0, velocityY.toInt(), 0, 0, 0, 1000)
		return true
	}

	private fun changeThePageDown() {
		if (songPixelPosition == song!!.scrollEndPixel)
			if (++endSongByPedalCounter == SONG_END_PEDAL_PRESSES)
				endSong(false)
			else
				lastTempMessageTime = System.nanoTime()
		else
			changePage(true)
	}

	fun onOtherPageDownActivated() {
		if (startState !== PlayState.AtTitleScreen)
			onPageDownKeyPressed()
	}

	fun onPageDownKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changeThePageDown()
		} else if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			changeThePageDown()
		else
			changeVolume(+5)
	}

	fun onPageUpKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changePage(false)
		} else if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			changePage(false)
		else
			changeVolume(-5)
	}

	private fun changeTheLineDown() {
		if (songPixelPosition == song!!.scrollEndPixel)
			if (++endSongByPedalCounter == SONG_END_PEDAL_PRESSES)
				endSong(false)
			else
				lastTempMessageTime = System.nanoTime()
		else
			changeLine(true)
	}

	fun onLineDownKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changeTheLineDown()
		} else if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			changeTheLineDown()
		else
			changeVolume(+5)
	}

	fun onLineUpKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changeLine(false)
		} else if (song!!.currentLine.scrollMode === ScrollingMode.Manual)
			changeLine(false)
		else
			changeVolume(-5)
	}

	fun onLeftKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changeVolume(-5)
		} else
			changeVolume(-5)
	}

	fun onRightKeyPressed() {
		if (startState !== PlayState.Playing) {
			if (!startToggle(null, false) && song!!.currentLine.scrollMode === ScrollingMode.Manual)
				changeVolume(+5)
		} else
			changeVolume(+5)
	}

	private fun changePage(down: Boolean) {
		if (startState === PlayState.AtTitleScreen)
			return
		if (targetPixelPosition != -1 && targetPixelPosition != songPixelPosition)
			return
		if (down && manualScrollPositions.mBeatJumpScrollLine != null)
			setSongTime(
				manualScrollPositions.mBeatJumpScrollLine!!.lineTime,
				redraw = true,
				broadcast = true,
				setPixelPosition = true,
				recalculateManualPositions = false
			)
		else
			targetPixelPosition =
				if (down)
					manualScrollPositions.mPageDownPosition
				else
					manualScrollPositions.mPageUpPosition
	}

	private fun changeLine(down: Boolean) {
		if (startState === PlayState.AtTitleScreen)
			return
		if (targetPixelPosition != -1 && targetPixelPosition != songPixelPosition)
			return
		val targetLine =
			if (down)
				song!!.currentLine.nextLine
			else
				song!!.currentLine.previousLine
		targetPixelPosition = (targetLine ?: song!!.currentLine).songPixelPosition
	}

	private fun clearScrollTarget() {
		targetPixelPosition = -1
		targetAcceleration = 1
		calculateManualScrollPositions()
	}

	private fun calculateManualScrollPositions() {
		val currentLine = song!!.currentLine
		// Don't bother doing this if we aren't in manual mode.
		if (currentLine.scrollMode == ScrollingMode.Manual) {
			val usableScreenHeight = song!!.displaySettings.usableScreenHeight

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
			val defaultPageUpScrollPosition = songPixelPosition - defaultScrollAmount

			// So now find the manual block start.
			var manualModeBlockStartPosition = currentLine.songPixelPosition
			var pageUpLine = currentLine
			while (pageUpLine.previousLine != null && pageUpLine.previousLine!!.scrollMode == ScrollingMode.Manual) {
				pageUpLine = pageUpLine.previousLine!!
				manualModeBlockStartPosition = pageUpLine.songPixelPosition
			}

			// And take the greater of the two.
			val pageUpPosition = max(defaultPageUpScrollPosition, manualModeBlockStartPosition)

			// Now for page-down. Bit trickier.
			// Again, we might be using the "default scroll" amount.
			val defaultPageDownScrollPosition = songPixelPosition + defaultScrollAmount

			// We're going to traverse downwards from the current line. Initialise some vars.
			var pageDownLine = currentLine
			var beatJumpScrollLine: Line? = null
			var pageDownPosition = defaultPageDownScrollPosition

			// Keep going while there are still lines to check.
			while (pageDownLine.nextLine != null) {
				val nextLine = pageDownLine.nextLine!!

				// Escape clause 1: beat line!
				if (nextLine.scrollMode == ScrollingMode.Beat) {
					// Whoa, we've found a beat line! End of the road.
					// If it is onscreen enough, we will use it.
					beatJumpScrollLine =
						if (nextLine.isFullyOnScreen(songPixelPosition) || nextLine.screenCoverage(
								songPixelPosition
							) > MINIMUM_SCREEN_COVERAGE_FOR_BEAT_SCROLL
						)
							nextLine
						else
							null
					pageDownPosition =
						when {
							// If we found a beat scroll line, we'll be scrolling to that.
							beatJumpScrollLine != null -> beatJumpScrollLine.songPixelPosition
							// Otherwise we will use the last manual line, unless that line is HUGE ...
							pageDownLine.screenCoverage(songPixelPosition) <= MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> pageDownLine.songPixelPosition
							// ...in which case, we will just do a default scroll.
							else -> defaultPageDownScrollPosition
						}
					break
				}
				// Escape clause 2: we've found a manual line that ISN'T fully onscreen.
				if (!nextLine.isFullyOnScreen(songPixelPosition)) {
					// Figure out how much of the screen this line covers.
					val nextLineScreenCoverage = nextLine.screenCoverage(songPixelPosition)
					pageDownPosition =
						when {
							// If it takes up an enormous amount of screen, we'll just do a default scroll.
							nextLineScreenCoverage > MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> defaultPageDownScrollPosition
							// If it takes up a reasonable amount of screen, we'll use it.
							nextLineScreenCoverage >= MINIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> nextLine.songPixelPosition
							// Last possibility: it wasn't onscreen enough, so probably isn't very readable.
							// We've still got the previous line "in the bank", so we'll use that unless THAT
							// line takes up a huge amount of the screen ...
							pageDownLine.screenCoverage(songPixelPosition) <= MAXIMUM_SCREEN_COVERAGE_FOR_MANUAL_SCROLL -> pageDownPosition
							// ...in which case, we will just do a default scroll.
							else -> defaultPageDownScrollPosition
						}
					break
				}
				// Move to the next line.
				pageDownLine = nextLine
				pageDownPosition = pageDownLine.songPixelPosition
			}

			// Phew! Got there in the end.
			// Never scroll beyond the pre-calculated end point (though this should never happen).
			pageDownPosition = min(song!!.scrollEndPixel, pageDownPosition)

			manualScrollPositions.mPageUpPosition = pageUpPosition
			manualScrollPositions.mPageDownPosition = pageDownPosition
			manualScrollPositions.mBeatJumpScrollLine = beatJumpScrollLine
		}
	}

	fun setSongBeatPosition(pointer: Int, midiInitiated: Boolean) {
		val songTime = song!!.getMIDIBeatTime(pointer)
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
		while (startState !== PlayState.Playing)
			startToggle(null, midiInitiated)
	}

	fun stopSong(midiInitiated: Boolean) {
		if (startState === PlayState.Playing)
			startToggle(null, midiInitiated)
	}

	internal fun canYieldToExternalTrigger(): Boolean =
		when (externalTriggerSafetyCatch) {
			TriggerSafetyCatch.Always -> true
			TriggerSafetyCatch.WhenAtTitleScreen -> startState === PlayState.AtTitleScreen
			TriggerSafetyCatch.WhenAtTitleScreenOrPaused -> startState !== PlayState.Playing || song?.currentLine?.scrollMode === ScrollingMode.Manual
			TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine -> startState !== PlayState.Playing || song == null || song!!.currentLine.nextLine == null || song!!.currentLine.scrollMode === ScrollingMode.Manual
			TriggerSafetyCatch.Never -> false
		}

	private fun getLineHighlightColor(line: Line, time: Long): Int? {
		if (line == song!!.currentLine && highlightCurrentLine && line.scrollMode == ScrollingMode.Beat)
			return defaultCurrentLineHighlightColor
		if (highlightBeatSectionStart && line == manualScrollPositions.mBeatJumpScrollLine)
			return beatSectionStartHighlightColors[((time / 1000000.0) % beatSectionStartHighlightColors.size).toInt()]
		return null
	}

	internal inner class ManualMetronomeTask(bpm: Double) : Task(true) {
		private var nanosecondsPerBeat: Long = 0
		private var nextClickTime: Long = 0

		init {
			nanosecondsPerBeat = Utils.nanosecondsPerBeat(bpm)
		}

		override fun doWork() {
			nextClickTime = System.nanoTime()
			clickSoundPool.play(clickAudioId, 1.0f, 1.0f, 1, 0, 1.0f)
			nextClickTime += nanosecondsPerBeat
			val wait = nextClickTime - System.nanoTime()
			if (wait > 0) {
				val millisecondsPerBeat = Utils.nanoToMilli(wait).toLong()
				val nanosecondRemainder = (wait - Utils.milliToNano(millisecondsPerBeat)).toInt()
				try {
					Thread.sleep(millisecondsPerBeat, nanosecondRemainder)
				} catch (ie: InterruptedException) {
					Logger.log("Interrupted while waiting ... assuming resync attempt.", ie)
					nextClickTime = System.nanoTime()
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
		private val accelerations = IntArray(2048)

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
				accelerations[it] = ceil(sqrt((it + 1).toDouble()) * 2.0).toInt()
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