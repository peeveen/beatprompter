package com.stevenfrew.beatprompter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.OverScroller
import android.widget.Toast
import com.stevenfrew.beatprompter.bluetooth.*
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.MIDIController
import com.stevenfrew.beatprompter.songload.SongLoaderTask
import java.io.FileInputStream

class SongView : AppCompatImageView, GestureDetector.OnGestureListener {

    private var mBeatCountRect = Rect()
    private var mEndSongByPedalCounter = 0
    private var mMetronomeBeats: Long = 0
    private var mInitialized = false
    private var mScreenWidth: Int = 0
    private var mScreenHeight: Int = 0
    private var mPageUpDownScreenHeight: Int = 0
    private var mSongScrollEndPixel: Int = 0
    private var mSkipping = false
    private var mCurrentVolume = 80
    private var mLastCommentEvent: CommentEvent? = null
    private var mLastCommentTime: Long = 0
    private var mLastTempMessageTime: Long = 0
    private var mLastBeatTime: Long = 0
    private var mPaint: Paint? = null           // The paint (e.g. style, color) used for drawing
    private val mScroller: OverScroller

    private  var mMetronomeTask: MetronomeTask? = null
    private  var mMetronomeThread: Thread? = null

    private var mSong: Song? = null

    private var mPageDownPixel = 0
    private var mPageUpPixel = 0
    private var mLineDownPixel = 0
    private var mLineUpPixel = 0

    private var mSongStartTime: Long = 0
    private var mStartState = PlayState.AtTitleScreen
    private var mUserHasScrolled = false
    private var mPauseTime: Long = 0
    private var mNanosecondsPerBeat = Utils.nanosecondsPerBeat(120.0)
    private val mBackgroundColorLookup = IntArray(101)
    private var mCommentTextColor: Int = 0
    private var mDefaultCurrentLineHighlightColour: Int = 0
    private var mSongPixelPosition = 0
    private var mTargetPixelPosition = -1
    private var mTargetAcceleration = 1
    private var mShowScrollIndicator = true
    private var mShowSongTitle = false
    private var mHighlightCurrentLine = false
    private var mSongTitleContrastBackground: Int = 0
    private var mSongTitleContrastBeatCounter: Int = 0
    private var mScrollIndicatorRect: Rect? = null
    private val mLastProcessedColorEvent: ColorEvent? = null
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScreenAction = ScreenAction.Scroll
    private var mBeatCounterColor = Color.WHITE
    private var mScrollMarkerColor = Color.BLACK
    private var mPulse = true
    private var mTrackMediaPlayer: MediaPlayer= MediaPlayer()
    private var mSilenceMediaPlayer: MediaPlayer= MediaPlayer.create(context, R.raw.silence)
    private var mClickSoundPool: SoundPool= SoundPool.Builder().setMaxStreams(16).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build()
    private var mClickAudioID: Int = 0
    private var mCommentDisplayTimeNanoseconds = Utils.milliToNano(4000)
    private var mSongDisplayActivity: SongDisplayActivity? = null
    private  var mExternalTriggerSafetyCatch: TriggerSafetyCatch?=null
    private  var mSendMidiClock = false

    internal enum class ScreenAction {
        Scroll, Volume, None
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mScroller = OverScroller(context)
        mGestureDetector = GestureDetectorCompat(context, this)
        initView()
    }

    // Constructor
    constructor(context: Context) : super(context) {
        mScroller = OverScroller(context)
        mGestureDetector = GestureDetectorCompat(context, this)
        initView()
    }

    fun init(songDisplayActivity: SongDisplayActivity) {
        mSongDisplayActivity = songDisplayActivity
        mSong = SongLoaderTask.currentSong
        calculateScrollEnd()
        val sharedPref = BeatPrompterApplication.preferences
        mExternalTriggerSafetyCatch = TriggerSafetyCatch.valueOf(sharedPref.getString(songDisplayActivity.getString(R.string.pref_midiTriggerSafetyCatch_key), songDisplayActivity.getString(R.string.pref_midiTriggerSafetyCatch_defaultValue)))
        val metronomePref = sharedPref.getString(songDisplayActivity.getString(R.string.pref_metronome_key), songDisplayActivity.getString(R.string.pref_metronome_defaultValue))

        if (mSong!!.mSongFile.mBPM != 0.0) {
            var metronomeOn = metronomePref == songDisplayActivity.getString(R.string.metronomeOnValue)
            val metronomeOnWhenNoBackingTrack = metronomePref == songDisplayActivity.getString(R.string.metronomeOnWhenNoBackingTrackValue)
            val metronomeCount = metronomePref == songDisplayActivity.getString(R.string.metronomeDuringCountValue)

            if (metronomeOnWhenNoBackingTrack && mSong!!.mChosenBackingTrack == null)
                metronomeOn = true

            if (metronomeOn)
                mMetronomeBeats = java.lang.Long.MAX_VALUE
            else if (metronomeCount)
                mMetronomeBeats = (mSong!!.mCountIn * mSong!!.mInitialBPB).toLong()
        }

        if (mSong != null) {
            mSendMidiClock = mSong!!.mSendMidiClock || sharedPref.getBoolean(songDisplayActivity.getString(R.string.pref_sendMidi_key), false)
            mBeatCountRect = Rect(0, 0, mSong!!.mBeatCounterRect!!.width(), mSong!!.mBeatCounterHeight)
            mHighlightCurrentLine = mSong!!.mScrollingMode === ScrollingMode.Beat && sharedPref.getBoolean(songDisplayActivity.getString(R.string.pref_highlightCurrentLine_key), java.lang.Boolean.parseBoolean(songDisplayActivity.getString(R.string.pref_highlightCurrentLine_defaultValue)))
        }
    }

    private fun calculateScrollEnd() {
        var songDisplayEndPixel = mSong!!.mSongHeight
        if (mSong!!.mScrollingMode !== ScrollingMode.Beat)
            songDisplayEndPixel -= mScreenHeight
        mSongScrollEndPixel = Math.max(0, songDisplayEndPixel)
        if (mSong!!.mScrollingMode === ScrollingMode.Smooth) {
            mSongScrollEndPixel += mSong!!.mSmoothScrollOffset
            mSongScrollEndPixel += mSong!!.mBeatCounterHeight
        }
    }

    private fun initView() {
        mClickAudioID = mClickSoundPool.load(this.context, R.raw.click, 0)
        mPaint = Paint()
        mSongPixelPosition = 0

        val context = this.context
        val sharedPref = BeatPrompterApplication.preferences
        val screenAction = sharedPref.getString(context.getString(R.string.pref_screenAction_key), context.getString(R.string.pref_screenAction_defaultValue))
        if (screenAction!!.equals(context.getString(R.string.screenActionNoneValue), ignoreCase = true))
            mScreenAction = ScreenAction.None
        if (screenAction.equals(context.getString(R.string.screenActionVolumeValue), ignoreCase = true))
            mScreenAction = ScreenAction.Volume
        if (screenAction.equals(context.getString(R.string.screenActionScrollPauseAndRestartValue), ignoreCase = true))
            mScreenAction = ScreenAction.Scroll
        mShowScrollIndicator = sharedPref.getBoolean(context.getString(R.string.pref_showScrollIndicator_key), java.lang.Boolean.parseBoolean(context.getString(R.string.pref_showScrollIndicator_defaultValue)))
        mShowSongTitle = sharedPref.getBoolean(context.getString(R.string.pref_showSongTitle_key), java.lang.Boolean.parseBoolean(context.getString(R.string.pref_showSongTitle_defaultValue)))
        var commentDisplayTimeSeconds = sharedPref.getInt(context.getString(R.string.pref_commentDisplayTime_key), Integer.parseInt(context.getString(R.string.pref_commentDisplayTime_default)))
        commentDisplayTimeSeconds += Integer.parseInt(context.getString(R.string.pref_commentDisplayTime_offset))
        mCommentDisplayTimeNanoseconds = Utils.milliToNano(commentDisplayTimeSeconds * 1000)

        mCommentTextColor = Utils.makeHighlightColour(sharedPref.getInt(context.getString(R.string.pref_commentTextColor_key), Color.parseColor(context.getString(R.string.pref_commentTextColor_default))))
        mDefaultCurrentLineHighlightColour = Utils.makeHighlightColour(sharedPref.getInt(context.getString(R.string.pref_currentLineHighlightColor_key), Color.parseColor(context.getString(R.string.pref_currentLineHighlightColor_default))))
        mPulse = sharedPref.getBoolean(context.getString(R.string.pref_pulse_key), java.lang.Boolean.parseBoolean(context.getString(R.string.pref_pulse_defaultValue)))
    }

    private fun ensureInitialised() {
        if (mSong == null)
            return
        if (!mInitialized) {
            if (mSong!!.mScrollingMode === ScrollingMode.Smooth)
                mPulse = false
            mInitialized = true
            // First event will ALWAYS be a style event.
            processColorEvent(mSong!!.mFirstEvent as ColorEvent)

            if (mSong!!.mChosenBackingTrack != null)
                if (mSong!!.mChosenBackingTrack!!.mFile.exists()) {
                    // Play silence to kickstart audio system, allowing snappier playback.
                    mSilenceMediaPlayer.isLooping = true
                    mSilenceMediaPlayer.setVolume(0.01f, 0.01f)
                    // Shitty Archos workaround.
                    var fis: FileInputStream? = null
                    try {
                        fis = FileInputStream(mSong!!.mChosenBackingTrack!!.mFile.absolutePath)
                        mTrackMediaPlayer.setDataSource(fis.fd)
                        mTrackMediaPlayer.prepare()
                        seekTrack(0)
                        mCurrentVolume = mSong!!.mChosenBackingTrackVolume
                        mTrackMediaPlayer.setVolume(0.01f * mCurrentVolume, 0.01f * mCurrentVolume)
                        mTrackMediaPlayer.isLooping = false
                        mSilenceMediaPlayer.start()
                    } catch (e: Exception) {
                        val toast = Toast.makeText(context, R.string.crap_audio_file_warning, Toast.LENGTH_LONG)
                        toast.show()
                    }

                    try {
                        fis?.close()
                    } catch (ee: Exception) {
                        Log.e(BeatPrompterApplication.TAG, "Failed to close audio file input stream.", ee)
                    }
                } else {
                    val toast = Toast.makeText(context, R.string.missing_audio_file_warning, Toast.LENGTH_LONG)
                    toast.show()
                }
            if (mSong!!.mScrollingMode === ScrollingMode.Manual) {
                if (mMetronomeBeats > 0) {
                    mMetronomeTask = MetronomeTask(mSong!!.mSongFile.mBPM, mMetronomeBeats)
                    mMetronomeThread = Thread(mMetronomeTask)
                    // Infinite metronome? Might as well start it now.
                    if (mMetronomeBeats == java.lang.Long.MAX_VALUE)
                        mMetronomeThread!!.start()
                }
            }

        }
    }

    // Called back to draw the view. Also called by invalidate().
    override fun onDraw(canvas: Canvas) {
        if (mSong == null)
            return
        ensureInitialised()
        var scrolling = false
        if (mStartState !== PlayState.AtTitleScreen)
            scrolling = calculateScrolling()
        var timePassed: Long = 0
        var beatPercent = 1.0
        var showTempMessage = false
        var showComment = false
        if (mStartState === PlayState.Playing && !scrolling) {
            val time = System.nanoTime()
            timePassed = Math.max(0, time - mSongStartTime)
            if (mLastBeatTime > 0) {
                val beatTimePassed = Math.max(0, time - mLastBeatTime)
                val beatTime = (beatTimePassed % mNanosecondsPerBeat).toDouble()
                beatPercent = beatTime / mNanosecondsPerBeat
            }
            if (mSong!!.mScrollingMode !== ScrollingMode.Manual) {
                var event: BaseEvent?
                do {
                    event = mSong!!.getNextEvent(timePassed)
                    if (event is ColorEvent)
                        processColorEvent(event)
                    else if (event is CommentEvent)
                        processCommentEvent(event, time)
                    else if (event is BeatEvent)
                        processBeatEvent(event, true)
                    else if (event is MIDIEvent)
                        processMIDIEvent(event)
                    else if (event is PauseEvent)
                        processPauseEvent(event)
                    else if (event is LineEvent)
                        processLineEvent(event)
                    else if (event is TrackEvent)
                        processTrackEvent()
                    else if (event is EndEvent)
                        if (processEndEvent())
                            return
                } while(event!=null)
            }
            showTempMessage = time - mLastTempMessageTime < SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS
            if (mLastCommentEvent != null)
                if (time - mLastCommentTime < mCommentDisplayTimeNanoseconds)
                    showComment = true
        }
        var currentY = mSong!!.mBeatCounterHeight
        var currentLine = mSong!!.mCurrentLine
        var yScrollOffset = 0
        val color = mBackgroundColorLookup[(beatPercent * 100.0).toInt()]
        canvas.drawColor(color, PorterDuff.Mode.SRC)
        if (currentLine != null) {
            var scrollPercentage = 0.0
            // If a scroll event in underway, move currentY up
            if (mStartState !== PlayState.Playing || mSong!!.mScrollingMode === ScrollingMode.Manual) {
                yScrollOffset = mSongPixelPosition - currentLine.mSongPixelPosition
                if (mSong!!.mScrollingMode === ScrollingMode.Smooth)
                    scrollPercentage = yScrollOffset.toDouble() / currentLine.mLineMeasurements!!.mLineHeight.toDouble()
            } else {
                if (!scrolling && mSong!!.mScrollingMode !== ScrollingMode.Manual) {
                    if (currentLine.mYStopScrollTime > timePassed && currentLine.mYStartScrollTime <= timePassed)
                        scrollPercentage = (timePassed - currentLine.mYStartScrollTime).toDouble() / (currentLine.mYStopScrollTime - currentLine.mYStartScrollTime).toDouble()
                    else if (currentLine.mYStopScrollTime <= timePassed)
                        scrollPercentage = 1.0
                    if (mSong!!.mScrollingMode === ScrollingMode.Smooth)
                        yScrollOffset = (currentLine.mLineMeasurements!!.mLineHeight * scrollPercentage).toInt()
                    else if (mSong!!.mScrollingMode === ScrollingMode.Beat)
                        yScrollOffset = currentLine.mLineMeasurements!!.mJumpScrollIntervals[(scrollPercentage * 100.0).toInt()]
                }
            }
            currentY -= yScrollOffset
            if (mStartState === PlayState.Playing)
                mSongPixelPosition = currentLine.mSongPixelPosition + yScrollOffset
            if (mSong!!.mScrollingMode === ScrollingMode.Smooth)
                currentY += mSong!!.mSmoothScrollOffset

            val startY = currentY
            var firstLineOnscreen: Line? = null
            var startOnscreen = false
            var endOnscreen = false
            var highlight = mHighlightCurrentLine
            while (currentLine != null && currentY < mScreenHeight) {
                if (currentY > mSong!!.mBeatCounterHeight - currentLine.mLineMeasurements!!.mLineHeight) {
                    if (firstLineOnscreen == null) {
                        firstLineOnscreen = currentLine
                        startOnscreen = currentY >= mSong!!.mBeatCounterHeight
                        endOnscreen = currentY + currentLine.mLineMeasurements!!.mLineHeight <= mScreenHeight
                    }
                    val graphics = currentLine.graphics
                    val lineTop = currentY
                    for ((lineCounter, graphic) in graphics.withIndex()) {
                        if (!graphic.mBitmap.isRecycled)
                            canvas.drawBitmap(graphic.mBitmap, 0f, currentY.toFloat(), mPaint)
                        currentY += currentLine.mLineMeasurements!!.mGraphicHeights[lineCounter]
                    }
                    if (highlight) {
                        mPaint!!.color = mDefaultCurrentLineHighlightColour
                        canvas.drawRect(0f, lineTop.toFloat(), mScreenWidth.toFloat(), (lineTop + currentLine.mLineMeasurements!!.mLineHeight).toFloat(), mPaint!!)
                        mPaint!!.alpha = 255
                    }
                } else
                    currentY += currentLine.mLineMeasurements!!.mLineHeight
                currentLine = currentLine.mNextLine
                highlight = false
            }
            // Calculate pageup/pagedown/lineup/linedown lines
            if (mSong!!.mScrollingMode === ScrollingMode.Manual)
                calculateManualScrollingPositions(firstLineOnscreen, currentLine, currentY, startOnscreen, endOnscreen)

            if (mSong!!.mScrollingMode === ScrollingMode.Smooth) {
                // If we've drawn the end of the last line, stop smooth scrolling.
                val prevLine = mSong!!.mCurrentLine!!.mPrevLine
                if (prevLine != null && startY > 0) {
                    mPaint!!.alpha = (255.0 - 255.0 * scrollPercentage).toInt()
                    currentY = startY - prevLine.mLineMeasurements!!.mLineHeight
                    val graphics = prevLine.graphics
                    for ((lineCounter, graphic) in graphics.withIndex()) {
                        canvas.drawBitmap(graphic.mBitmap, 0f, currentY.toFloat(), mPaint)
                        currentY += prevLine.mLineMeasurements!!.mGraphicHeights[lineCounter]
                    }
                    mPaint!!.alpha = 255
                }
            }
        }
        mPaint!!.color = mBackgroundColorLookup[100]
        canvas.drawRect(0f, 0f, mScreenWidth.toFloat(), mSong!!.mBeatCounterHeight.toFloat(), mPaint!!)
        mPaint!!.color = mScrollMarkerColor
        if (mSong!!.mScrollingMode === ScrollingMode.Beat && mShowScrollIndicator && mScrollIndicatorRect != null)
            canvas.drawRect(mScrollIndicatorRect!!, mPaint!!)
        mPaint!!.color = mBeatCounterColor
        canvas.drawRect(mBeatCountRect, mPaint!!)
        canvas.drawLine(0f, mSong!!.mBeatCounterHeight.toFloat(), mScreenWidth.toFloat(), mSong!!.mBeatCounterHeight.toFloat(), mPaint!!)
        if (mShowSongTitle)
            showSongTitle(canvas)
        if (showTempMessage) {
            if (mEndSongByPedalCounter == 0)
                showTempMessage(mCurrentVolume.toString() + "%", 80, Color.BLACK, canvas)
            else {
                val message = "Press pedal " + (SONG_END_PEDAL_PRESSES - mEndSongByPedalCounter) + " more times to end song."
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

    private fun calculateScrolling(): Boolean {
        var scrolling = false
        if ((mScreenAction == ScreenAction.Scroll || mSong!!.mScrollingMode === ScrollingMode.Manual) && mScroller.computeScrollOffset()) {
            mSongPixelPosition = mScroller.currY
            //if (mSong.mScrollingMode != ScrollingMode.Manual)
            run {
                val songTime = mSong!!.mCurrentLine!!.getTimeFromPixel(mSongPixelPosition)
                setSongTime(songTime, mStartState === PlayState.Paused, true, false)
            }
            scrolling = true
        } else if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition) {
            val diff = Math.min(2048, Math.max(-2048, mTargetPixelPosition - mSongPixelPosition))
            val absDiff = Math.abs(diff)
            val targetAcceleration = Math.min(mAccelerations[absDiff - 1], absDiff)
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
            val songTime = mSong!!.mCurrentLine!!.getTimeFromPixel(mSongPixelPosition)
            setSongTime(songTime, mStartState === PlayState.Paused, true, false)
        }
        return scrolling
    }

    private fun drawTitleScreen(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        val midX = mScreenWidth shr 1
        val fifteenPercent = mScreenHeight * 0.15f
        var startY = Math.floor(((mScreenHeight - mSong!!.mTotalStartScreenTextHeight) / 2).toDouble()).toInt()
        val nextSongSS = mSong!!.mNextSongString
        if (nextSongSS != null) {
            mPaint!!.color = if (mSkipping) Color.RED else Color.WHITE
            val halfDiff = (fifteenPercent - nextSongSS.mHeight) / 2.0f
            canvas.drawRect(0f, mScreenHeight - fifteenPercent, mScreenWidth.toFloat(), mScreenHeight.toFloat(), mPaint!!)
            val nextSongY = mScreenHeight - (nextSongSS.mDescenderOffset + halfDiff).toInt()
            startY -= (fifteenPercent / 2.0f).toInt()
            mPaint!!.color = nextSongSS.mColor
            mPaint!!.textSize = nextSongSS.mFontSize * Utils.FONT_SCALING
            mPaint!!.typeface = nextSongSS.mFace
            mPaint!!.flags = Paint.ANTI_ALIAS_FLAG
            canvas.drawText(nextSongSS.mText, (midX - (nextSongSS.mWidth shr 1)).toFloat(), nextSongY.toFloat(), mPaint!!)
        }
        for (ss in mSong!!.mStartScreenStrings) {
            startY += ss.mHeight
            with(mPaint!!){
                color = ss.mColor
                textSize = ss.mFontSize * Utils.FONT_SCALING
                typeface = ss.mFace
                flags = Paint.ANTI_ALIAS_FLAG
            }
            canvas.drawText(ss.mText, (midX - (ss.mWidth shr 1)).toFloat(), (startY - ss.mDescenderOffset).toFloat(), mPaint!!)
        }
    }

    private fun calculateManualScrollingPositions(firstLineOnscreen: Line?, currentLine: Line?, currentY: Int, startOnscreen: Boolean, endOnscreen: Boolean) {
        var vCurrentLine = currentLine
        // If the end of the current line is on-screen, the linedown pixel position should be the start of the next line.
        // Otherwise, it should be 80% of the screen further down than it currently is.
        mLineDownPixel = if (endOnscreen) {
            if (firstLineOnscreen?.mNextLine != null)
                firstLineOnscreen.mNextLine!!.mSongPixelPosition
            else
                mSongPixelPosition
        } else
            mSongPixelPosition + mPageUpDownScreenHeight

        val lineUpLine: Line?
        if (startOnscreen) {
            if (firstLineOnscreen?.mPrevLine != null) {
                mLineUpPixel = firstLineOnscreen.mPrevLine!!.mSongPixelPosition
                lineUpLine = firstLineOnscreen.mPrevLine
            } else {
                mLineUpPixel = mSongPixelPosition
                lineUpLine = null
            }
        } else {
            // If the start of the firstLineOnScreen is less than 80% of the screen away, scroll to it.
            // Otherwise, scroll up 80%.
            if (firstLineOnscreen != null && mSongPixelPosition - firstLineOnscreen.mSongPixelPosition < mPageUpDownScreenHeight) {
                mLineUpPixel = firstLineOnscreen.mSongPixelPosition
                lineUpLine = firstLineOnscreen
            } else {
                mLineUpPixel = mSongPixelPosition - mPageUpDownScreenHeight
                mPageUpPixel = mLineUpPixel
                lineUpLine = null
            }
        }

        // If we managed to draw any of the last line, then page down should take us to the last line.
        // Is the END of the last line onscreen?
        when {
            currentY < mScreenHeight -> // Is the START of the last line onscreen?
                mPageDownPixel = if (mSong!!.mLastLine != null && currentY - mSong!!.mLastLine!!.mLineMeasurements!!.mLineHeight >= 0)
                // Yes? Then move the last line to the top of the screen
                    mSong!!.mLastLine!!.mSongPixelPosition
                else
                // No? Then don't move.
                    mSongPixelPosition
            vCurrentLine == null -> // Does the entire last line fit onscreen?
                mPageDownPixel = if (mSong!!.mLastLine != null && mSong!!.mLastLine!!.mLineMeasurements!!.mLineHeight < mScreenHeight)
                // Yes? Then move the last line to the top of the screen.
                    mSong!!.mLastLine!!.mSongPixelPosition
                else
                // No? Then scroll 80%.
                    mSongPixelPosition + mPageUpDownScreenHeight
            else -> // Is the end of the first line drawn onscreen?
                mPageDownPixel = if (vCurrentLine.mPrevLine != null && endOnscreen)
                // Yes? Then scroll to the last line drawn.
                    vCurrentLine.mPrevLine!!.mSongPixelPosition
                else
                // No? Then scroll 80%
                    mSongPixelPosition + mPageUpDownScreenHeight
        }// We haven't reached the last line yet.
        // Did we draw at least SOME of the last line?

        vCurrentLine = lineUpLine
        val pageUpLine = vCurrentLine
        if (pageUpLine != null) {
            var totalHeight = mSongPixelPosition - lineUpLine!!.mSongPixelPosition
            vCurrentLine = vCurrentLine!!.mPrevLine
            mPageUpPixel = mSongPixelPosition
            while (vCurrentLine != null) {
                totalHeight += vCurrentLine.mLineMeasurements!!.mLineHeight
                if (totalHeight < mScreenHeight)
                    mPageUpPixel = vCurrentLine.mSongPixelPosition
                else
                    break
                vCurrentLine = vCurrentLine.mPrevLine
            }
            if (mPageUpPixel == mSongPixelPosition)
                mPageUpPixel = mSongPixelPosition - mPageUpDownScreenHeight
        }

        mPageDownPixel = Math.min(mSongScrollEndPixel, mPageDownPixel)
        mLineDownPixel = Math.min(mSongScrollEndPixel, mLineDownPixel)
        mPageUpPixel = Math.max(0, mPageUpPixel)
        mLineUpPixel = Math.min(0, mLineUpPixel)
    }

    // Called back when the view is first created or its size changes.
    public override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        // Set the movement bounds for the ball
        // Can't use wider than 4096, as can't create bitmaps bigger than that.
        mScreenWidth = Math.min(w, 4096)
        mScreenHeight = h
        if (mSong != null)
            calculateScrollEnd()
        mPageUpDownScreenHeight = (mScreenHeight * 0.8).toInt()
    }

    private fun showTempMessage(message: String, textSize: Int, textColor: Int, canvas: Canvas) {
        val popupMargin = 25
        mPaint!!.textSize = textSize * Utils.FONT_SCALING
        mPaint!!.flags = Paint.ANTI_ALIAS_FLAG
        val outRect = Rect()
        mPaint!!.getTextBounds(message, 0, message.length, outRect)
        val textWidth = mPaint!!.measureText(message)
        val textHeight = outRect.height()
        val volumeControlWidth = textWidth + popupMargin * 2.0f
        val volumeControlHeight = textHeight + popupMargin * 2
        val x = (mScreenWidth - volumeControlWidth) / 2.0f
        val y = (mScreenHeight - volumeControlHeight) / 2
        mPaint!!.color = Color.BLACK
        canvas.drawRect(x, y.toFloat(), x + volumeControlWidth, (y + volumeControlHeight).toFloat(), mPaint!!)
        mPaint!!.color = Color.rgb(255, 255, 200)
        canvas.drawRect(x + 1, (y + 1).toFloat(), x + (volumeControlWidth - 2), (y + (volumeControlHeight - 2)).toFloat(), mPaint!!)
        mPaint!!.color = textColor
        canvas.drawText(message, (mScreenWidth - textWidth) / 2, ((mScreenHeight - textHeight) / 2 + textHeight).toFloat(), mPaint!!)
    }

    private fun showSongTitle(canvas: Canvas) {
        if (mSong == null || mSong!!.mSongTitleHeader == null)
            return

        with(mPaint!!)
        {
            textSize = mSong!!.mSongTitleHeader!!.mFontSize * Utils.FONT_SCALING
            typeface = mSong!!.mSongTitleHeader!!.mFace
            flags = Paint.ANTI_ALIAS_FLAG
            color = mSongTitleContrastBackground
        }

        canvas.drawText(mSong!!.mSongTitleHeader!!.mText, mSong!!.mSongTitleHeaderLocation!!.x, mSong!!.mSongTitleHeaderLocation!!.y, mPaint!!)

        canvas.save()
        canvas.clipRect(mBeatCountRect)
        mPaint!!.color = mSongTitleContrastBeatCounter
        canvas.drawText(mSong!!.mSongTitleHeader!!.mText, mSong!!.mSongTitleHeaderLocation!!.x, mSong!!.mSongTitleHeaderLocation!!.y, mPaint!!)
        canvas.restore()

        if (mScrollIndicatorRect != null) {
            canvas.save()
            canvas.clipRect(mScrollIndicatorRect!!)
            canvas.drawText(mSong!!.mSongTitleHeader!!.mText, mSong!!.mSongTitleHeaderLocation!!.x, mSong!!.mSongTitleHeaderLocation!!.y, mPaint!!)
            canvas.restore()
        }

        mPaint!!.alpha = 255
    }

    private fun showComment(canvas: Canvas) {
        if (mLastCommentEvent != null) {
            with(mPaint!!)
            {
                textSize = mLastCommentEvent!!.mScreenString!!.mFontSize * Utils.FONT_SCALING
                flags = Paint.ANTI_ALIAS_FLAG
                color = Color.BLACK
            }
            canvas.drawRect(mLastCommentEvent!!.mPopupRect!!, mPaint!!)
            mPaint!!.color = Color.WHITE
            canvas.drawRect(mLastCommentEvent!!.mPopupRect!!.left + 1, mLastCommentEvent!!.mPopupRect!!.top + 1, mLastCommentEvent!!.mPopupRect!!.right - 1, mLastCommentEvent!!.mPopupRect!!.bottom - 1, mPaint!!)
            mPaint!!.color = mCommentTextColor
            mPaint!!.alpha = 255
            canvas.drawText(mLastCommentEvent!!.mComment.mText, mLastCommentEvent!!.mTextDrawLocation!!.x, mLastCommentEvent!!.mTextDrawLocation!!.y, mPaint!!)
        }
    }

    fun startToggle(e: MotionEvent?, midiInitiated: Boolean, playState: PlayState) {
        mStartState = playState
        startToggle(e, midiInitiated)
    }

    private fun startToggle(e: MotionEvent?, midiInitiated: Boolean): Boolean {
        if (mSong == null)
            return true
        if (mStartState !== PlayState.Playing) {
            if (mStartState === PlayState.AtTitleScreen)
                if (e != null)
                    if (e.y > mScreenHeight * 0.85f)
                        if (mSong!!.mNextSong != null && mSong!!.mNextSong!!.isNotEmpty()) {
                            endSong(true)
                            return true
                        }
            val oldPlayState = mStartState
            mStartState = PlayState.increase(mStartState)
            if (mStartState === PlayState.Playing) {
                if (mSong!!.mScrollingMode === ScrollingMode.Manual) {
                    // Start the count in.
                    if (mMetronomeThread != null) {
                        if (!mMetronomeThread!!.isAlive) {
                            return if (mMetronomeBeats != 0L) {
                                mMetronomeThread!!.start()
                                true
                            } else
                                processTrackEvent()
                        }
                    } else
                        return processTrackEvent()
                } else {
                    val time: Long
                    if (mUserHasScrolled) {
                        mUserHasScrolled = false
                        time = mSong!!.getTimeFromPixel(mSongPixelPosition)
                        setSongTime(time, false, false, false)
                    } else {
                        Log.d(BeatPrompterApplication.TAG, "Resuming, pause time=$mPauseTime")
                        time = mPauseTime
                        setSongTime(time, false, false, true)
                    }
                    BluetoothManager.broadcastMessageToClients(ToggleStartStopMessage(oldPlayState, time))
                }
            } else
                BluetoothManager.broadcastMessageToClients(ToggleStartStopMessage(oldPlayState, 0))
        } else {
            if (mScreenAction == ScreenAction.Volume) {
                if (e != null) {
                    if (e.y < mScreenHeight * 0.5)
                        changeVolume(+5)
                    else if (e.y > mScreenHeight * 0.5)
                        changeVolume(-5)
                }
            } else if (mSong!!.mScrollingMode !== ScrollingMode.Manual) {
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
        BluetoothManager.broadcastMessageToClients(ToggleStartStopMessage(mStartState, mPauseTime))
        mStartState = PlayState.reduce(mStartState)
        if (mTrackMediaPlayer.isPlaying)
            mTrackMediaPlayer.pause()
        if (!midiInitiated)
            if (mSongDisplayActivity != null)
                mSongDisplayActivity!!.onSongStop()
    }

    fun stop(destroyed: Boolean) {
        if (mStartState === PlayState.Playing)
            pause(false)
        if (destroyed) {
            BluetoothManager.broadcastMessageToClients(QuitSongMessage())
            if (mSong != null)
                mSong!!.recycleGraphics()
            mSong = null
            Task.stopTask(mMetronomeTask, mMetronomeThread)
            mTrackMediaPlayer.stop()
            mTrackMediaPlayer.release()
            mSilenceMediaPlayer.stop()
            mSilenceMediaPlayer.release()
            mClickSoundPool.release()
            System.gc()
        }
    }

    private fun processCommentEvent(event: CommentEvent, systemTime: Long) {
        mLastCommentTime = systemTime
        mLastCommentEvent = event
    }

    private fun processColorEvent(event: ColorEvent?) {
        if (event == mLastProcessedColorEvent)
            return
        mBeatCounterColor = event!!.mBeatCounterColor
        mScrollMarkerColor = event.mScrollMarkerColor
        mSongTitleContrastBeatCounter = Utils.makeContrastingColour(mBeatCounterColor)
        val backgroundColor = event.mBackgroundColor
        val pulseColor = if (mPulse) event.mPulseColor else event.mBackgroundColor
        val bgR = Color.red(backgroundColor)
        val bgG = Color.green(backgroundColor)
        val bgB = Color.blue(backgroundColor)
        val pR = Color.red(pulseColor)
        val pG = Color.green(pulseColor)
        val pB = Color.blue(pulseColor)
        val rDiff = pR - bgR
        val gDiff = pG - bgG
        val bDiff = pB - bgB
        for (f in 0..100) {
            val sineLookup = Utils.mSineLookup[(90.0 * (f.toDouble() / 100.0)).toInt()]
            val red = pR - (sineLookup * rDiff.toDouble()).toInt()
            val green = pG - (sineLookup * gDiff.toDouble()).toInt()
            val blue = pB - (sineLookup * bDiff.toDouble()).toInt()
            val color = Color.rgb(red, green, blue)
            mBackgroundColorLookup[f] = color
        }
        mSongTitleContrastBackground = Utils.makeContrastingColour(mBackgroundColorLookup[100])
    }

    private fun processBeatEvent(event: BeatEvent?, allowClick: Boolean) {
        if (event == null)
            return
        mNanosecondsPerBeat = Utils.nanosecondsPerBeat(event.mBPM)
        val beatWidth = mScreenWidth.toDouble() / event.mBPB.toDouble()
        val currentBeatCounterWidth = (beatWidth * (event.mBeat + 1).toDouble()).toInt()
        mScrollIndicatorRect = if (event.mWillScrollOnBeat != -1) {
            val thirdWidth = beatWidth / 3
            val thirdHeight = mSong!!.mBeatCounterHeight / 3.0
            val scrollIndicatorStart = (beatWidth * event.mWillScrollOnBeat + thirdWidth).toInt()
            val scrollIndicatorEnd = (beatWidth * (event.mWillScrollOnBeat + 1) - thirdWidth).toInt()
            Rect(scrollIndicatorStart, thirdHeight.toInt(), scrollIndicatorEnd, (thirdHeight * 2.0).toInt())
        } else
            null
        mBeatCountRect = Rect((currentBeatCounterWidth - beatWidth).toInt(), 0, currentBeatCounterWidth, mSong!!.mBeatCounterHeight)
        mLastBeatTime = mSongStartTime + event.mEventTime
        if (event.mClick && mStartState === PlayState.Playing && mSong!!.mScrollingMode !== ScrollingMode.Manual && allowClick)
            mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f)
        if (mSongDisplayActivity != null/*&&(!event.mCount)*/)
            mSongDisplayActivity!!.onSongBeat(event.mBPM)
    }

    private fun processPauseEvent(event: PauseEvent?) {
        if (event == null)
            return
        mLastBeatTime = -1
        val currentBeatCounterWidth = (mScreenWidth.toDouble() / (event.mBeats - 1).toDouble() * event.mBeat.toDouble()).toInt()
        mBeatCountRect = Rect(0, 0, currentBeatCounterWidth, mSong!!.mBeatCounterHeight)
        mScrollIndicatorRect = Rect(-1, -1, -1, -1)
    }

    private fun processMIDIEvent(event: MIDIEvent) {
        MIDIController.mMIDIOutQueue.addAll(event.mMessages)
    }

    private fun processLineEvent(event: LineEvent) {
        if (mSong == null)
            return
        mSong!!.mCurrentLine = event.mLine
    }

    private fun processTrackEvent(): Boolean {
        Log.d(BeatPrompterApplication.TAG, "Track event hit: starting MediaPlayer")
        mTrackMediaPlayer.start()
        return true
    }

    private fun processEndEvent(): Boolean {
        // End the song in beat mode, or if we're using a track in any other mode.
        val end = mSong!!.mScrollingMode === ScrollingMode.Beat
        if (end)
            endSong(false)
        return end
    }

    private fun endSong(skipped: Boolean) {
        if (mSongDisplayActivity != null) {
            mSkipping = skipped
            SongList.mSongEndedNaturally = true
            mStartState = PlayState.AtTitleScreen
            mSongDisplayActivity = null
            if (mSong != null)
                mSong!!.recycleGraphics()
            mSong = null
            EventHandler.sendEventToSongDisplay(EventHandler.END_SONG)
            System.gc()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.mGestureDetector!!.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun setSongTime(sstm: SetSongTimeMessage) {
        if (mSong != null) {
            Log.d(BeatPrompterApplication.TAG, "Setting time=" + sstm.mTime)
            setSongTime(sstm.mTime, true, false, true)
        }
    }

    private fun seekTrack(time: Int) {
        mTrackMediaPlayer.seekTo(time)
        //        while(mTrackMediaPlayer.getCurrentPosition()!=time)
        //        while(!getSeekCompleted())
        /*            try {
                Thread.sleep(10);
            }
            catch(InterruptedException e)
            {
            }*/
    }

    fun setSongTime(nano: Long, redraw: Boolean, broadcast: Boolean, setPixelPosition: Boolean) {
        if (mSong == null)
            return
        // No time context in Manual mode.
        if (setPixelPosition)
            mSongPixelPosition = mSong!!.getPixelFromTime(nano)
        //        if(mSong.mScrollingMode!=ScrollingMode.Manual)
        run {
            if (mStartState !== PlayState.Playing)
                mPauseTime = nano
            if (broadcast)
                BluetoothManager.broadcastMessageToClients(SetSongTimeMessage(nano))
            mSong!!.setProgress(nano)
            processColorEvent(mSong!!.mCurrentEvent!!.mPrevColorEvent)
            if (mSong!!.mScrollingMode !== ScrollingMode.Manual) {
                val prevBeatEvent = mSong!!.mCurrentEvent!!.mPrevBeatEvent
                val nextBeatEvent = mSong!!.mCurrentEvent!!.nextBeatEvent
                if (prevBeatEvent != null)
                    processBeatEvent(prevBeatEvent, nextBeatEvent != null)
            }
            mSongStartTime = System.nanoTime() - nano
            if (mSong!!.mScrollingMode !== ScrollingMode.Manual) {
                val trackEvent = mSong!!.mCurrentEvent!!.mPrevTrackEvent
                if (trackEvent != null) {
                    val nTime = Utils.nanoToMilli(nano - trackEvent.mEventTime)
                    seekTrack(nTime)
                    //                    Log.d(BeatPrompterApplication.TAG, "Seek to=" + nTime);
                    if (mStartState === PlayState.Playing) {
                        Log.d(BeatPrompterApplication.TAG, "Starting MediaPlayer")
                        mTrackMediaPlayer.start()
                    }
                } else {
                    Log.d(BeatPrompterApplication.TAG, "Seek to=0")
                    seekTrack(0)
                }
            }
            if (redraw)
                invalidate()
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        if (mSong!!.mScrollingMode === ScrollingMode.Manual)
            if (mMetronomeThread != null)
                if (mStartState === PlayState.Playing)
                    mMetronomeThread!!.interrupt()
        // Abort any active scroll animations and invalidate.
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollingMode === ScrollingMode.Manual)
            clearScrollTarget()
        mScroller.forceFinished(true)
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        startToggle(e, false)
        return true
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (mScreenAction == ScreenAction.None)
            return false
        if (mStartState === PlayState.AtTitleScreen)
            return false
        if (mSong == null)
            return false
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollingMode === ScrollingMode.Manual) {
            clearScrollTarget()
            mSongPixelPosition += distanceY.toInt()
            mSongPixelPosition = Math.max(0, mSongPixelPosition)
            mSongPixelPosition = Math.min(mSongScrollEndPixel, mSongPixelPosition)
            pauseOnScrollStart()
            //            if(mSong.mScrollingMode!=ScrollingMode.Manual)
            setSongTime(mSong!!.mCurrentLine!!.getTimeFromPixel(mSongPixelPosition), true, true, false)
        } else if (mScreenAction == ScreenAction.Volume) {
            mCurrentVolume += (distanceY / 10.0).toInt()
            onVolumeChanged()
        }
        return true
    }

    fun pauseOnScrollStart() {
        if (mSong!!.mScrollingMode === ScrollingMode.Manual)
            return
        if (mScreenAction != ScreenAction.Scroll)
            return
        BluetoothManager.broadcastMessageToClients(PauseOnScrollStartMessage())
        mUserHasScrolled = true
        mStartState = PlayState.Paused
        if (mTrackMediaPlayer.isPlaying) {
            Log.d(BeatPrompterApplication.TAG, "Pausing MediaPlayer")
            mTrackMediaPlayer.pause()
        }
        if (mSongDisplayActivity != null)
            mSongDisplayActivity!!.onSongStop()
    }

    private fun onVolumeChanged() {
        mCurrentVolume = Math.max(0, mCurrentVolume)
        mCurrentVolume = Math.min(100, mCurrentVolume)
        mTrackMediaPlayer.setVolume(0.01f * mCurrentVolume, 0.01f * mCurrentVolume)
        mLastTempMessageTime = System.nanoTime()
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (mScreenAction == ScreenAction.None)
            return false
        if (mStartState === PlayState.AtTitleScreen)
            return false
        if (mSong == null)
            return false
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollingMode === ScrollingMode.Manual) {
            clearScrollTarget()
            pauseOnScrollStart()
            mScroller.fling(0, mSongPixelPosition, 0, (-velocityY).toInt(), 0, 0, 0, mSongScrollEndPixel)
        } else if (mScreenAction == ScreenAction.Volume)
            mScroller.fling(0, mCurrentVolume, 0, velocityY.toInt(), 0, 0, 0, 1000)
        return true
    }

    private fun changeThePageDown() {
        if (mSongPixelPosition == mSongScrollEndPixel) {
            if (++mEndSongByPedalCounter == SONG_END_PEDAL_PRESSES)
                endSong(false)
            else
                mLastTempMessageTime = System.nanoTime()
        } else
            changePage(true)
    }

    fun onOtherPageDownActivated() {
        if (mStartState !== PlayState.AtTitleScreen)
            onPageDownKeyPressed()
    }

    fun onPageDownKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changeThePageDown()
        } else if (mSong!!.mScrollingMode === ScrollingMode.Manual) {
            changeThePageDown()
        } else
            changeVolume(+5)
    }

    fun onPageUpKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changePage(false)
        } else if (mSong!!.mScrollingMode === ScrollingMode.Manual)
            changePage(false)
        else
            changeVolume(-5)
    }

    private fun changeTheLineDown() {
        if (mSongPixelPosition == mSongScrollEndPixel) {
            if (++mEndSongByPedalCounter == SONG_END_PEDAL_PRESSES)
                endSong(false)
            else
                mLastTempMessageTime = System.nanoTime()
        } else
            changeLine(true)
    }

    fun onLineDownKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changeTheLineDown()
        } else if (mSong!!.mScrollingMode === ScrollingMode.Manual) {
            changeTheLineDown()
        } else
            changeVolume(+5)
    }

    fun onLineUpKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changeLine(false)
        } else if (mSong!!.mScrollingMode === ScrollingMode.Manual)
            changeLine(false)
        else
            changeVolume(-5)
    }

    fun onLeftKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changeVolume(-5)
        } else
            changeVolume(-5)
    }

    fun onRightKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollingMode === ScrollingMode.Manual)
                changeVolume(+5)
        } else
            changeVolume(+5)
    }

    private fun changePage(down: Boolean) {
        if (mStartState === PlayState.AtTitleScreen)
            return
        if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
            return
        mTargetPixelPosition = if (down) mPageDownPixel else mPageUpPixel
    }

    private fun changeLine(down: Boolean) {
        if (mStartState === PlayState.AtTitleScreen)
            return
        if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
            return
        mTargetPixelPosition = if (down) mLineDownPixel else mLineUpPixel
    }

    private fun clearScrollTarget() {
        mTargetPixelPosition = -1
        mTargetAcceleration = 1
    }

    fun setSongBeatPosition(pointer: Int, midiInitiated: Boolean) {
        val songTime = mSong!!.getMIDIBeatTime(pointer)
        setSongTime(songTime, true, midiInitiated, true)
    }

    fun startSong(midiInitiated: Boolean, fromStart: Boolean) {
        if (fromStart)
            setSongTime(0, true, midiInitiated, true)
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
            TriggerSafetyCatch.WhenAtTitleScreenOrPaused -> mStartState !== PlayState.Playing || mSong != null && mSong!!.mScrollingMode === ScrollingMode.Manual
            TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine -> mStartState !== PlayState.Playing || mSong == null || mSong!!.mCurrentLine == null || mSong!!.mCurrentLine!!.mNextLine == null || mSong!!.mScrollingMode === ScrollingMode.Manual
            TriggerSafetyCatch.Never -> false
            else -> false
        }
    }

    internal inner class MetronomeTask(bpm: Double, private var mBeats: Long) : Task(true) {
        private var mNanosecondsPerBeat: Long = 0
        private var mNextClickTime: Long = 0

        init {
            mNanosecondsPerBeat = Utils.nanosecondsPerBeat(bpm)
        }

        override fun doWork() {
            mMetronomeBeats = 0
            mNextClickTime = System.nanoTime()
            while (!shouldStop) {
                mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f)
                if (--mBeats == 0L)
                    stop()
                mNextClickTime += mNanosecondsPerBeat
                val wait = mNextClickTime - System.nanoTime()
                if (wait > 0) {
                    val millisecondsPerBeat = Utils.nanoToMilli(wait).toLong()
                    val nanosecondRemainder = (wait - Utils.milliToNano(millisecondsPerBeat)).toInt()
                    try {
                        Thread.sleep(millisecondsPerBeat, nanosecondRemainder)
                    } catch (ie: InterruptedException) {
                        Log.d(BeatPrompterApplication.TAG, "Interrupted while waiting ... assuming resync attempt.", ie)
                        mNextClickTime = System.nanoTime()
                    }

                }
            }
            // If we're quitting this loop because we've run out of beats, then start the track.
            if (mBeats == 0L) {
                processTrackEvent()
            }
        }
    }

    companion object {

        private const val SONG_END_PEDAL_PRESSES = 3
        private val SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS = Utils.milliToNano(2000)
        private val mAccelerations = IntArray(2048)

        init {
            for (f in 0..2047)
                mAccelerations[f] = Math.ceil(Math.sqrt((f + 1).toDouble()) * 2.0).toInt()
        }
    }
}
