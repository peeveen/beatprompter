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
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.MIDIController
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
    private val mPaint=Paint()
    private val mScroller: OverScroller
    private val mMetronomePref:MetronomeContext

    private  var mManualMetronomeTask: ManualMetronomeTask? = null
    private  var mManualMetronomeThread: Thread? = null

    private var mSong: Song? = null

    private var mSongStartTime: Long = 0
    private var mStartState = PlayState.AtTitleScreen
    private var mUserHasScrolled = false
    private var mPauseTime: Long = 0
    private var mNanosecondsPerBeat = Utils.nanosecondsPerBeat(120.0)

    private val mBackgroundColorLookup = IntArray(101)
    private val mCommentTextColor: Int
    private val mBeatCounterColor:Int
    private val mDefaultCurrentLineHighlightColour: Int
    private val mShowScrollIndicator: Boolean
    private val mShowSongTitle:Boolean
    private val mCommentDisplayTimeNanoseconds:Long
    private val mMediaPlayers= mutableMapOf<AudioFile,MediaPlayer>()
    private val mSilenceMediaPlayer: MediaPlayer= MediaPlayer.create(context, R.raw.silence)

    private var mPulse: Boolean
    private var mSongPixelPosition = 0
    private var mTargetPixelPosition = -1
    private var mTargetAcceleration = 1
    private var mHighlightCurrentLine = false
    private var mSongTitleContrastBackground: Int = 0
    private var mSongTitleContrastBeatCounter: Int = 0
    private var mScrollIndicatorRect: Rect? = null
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScreenAction = ScreenAction.Scroll
    private var mScrollMarkerColor = Color.BLACK
    private var mSongDisplayActivity: SongDisplayActivity? = null
    private var mExternalTriggerSafetyCatch: TriggerSafetyCatch?=null
    private var mSendMidiClock = false

    private val mClickSoundPool: SoundPool= SoundPool.Builder().setMaxStreams(16).setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).build()
    private val mClickAudioID=mClickSoundPool.load(this.context, R.raw.click, 0)

    internal enum class ScreenAction {
        Scroll, Volume, None
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mScroller = OverScroller(context)
        mGestureDetector = GestureDetectorCompat(context, this)
        mSongPixelPosition = 0

        val sharedPrefs = BeatPrompterApplication.preferences
        val screenAction = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_screenAction_key), BeatPrompterApplication.getResourceString(R.string.pref_screenAction_defaultValue))
        if (screenAction!!.equals(BeatPrompterApplication.getResourceString(R.string.screenActionNoneValue), ignoreCase = true))
            mScreenAction = ScreenAction.None
        if (screenAction.equals(BeatPrompterApplication.getResourceString(R.string.screenActionVolumeValue), ignoreCase = true))
            mScreenAction = ScreenAction.Volume
        if (screenAction.equals(BeatPrompterApplication.getResourceString(R.string.screenActionScrollPauseAndRestartValue), ignoreCase = true))
            mScreenAction = ScreenAction.Scroll
        mShowScrollIndicator = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showScrollIndicator_key), BeatPrompterApplication.getResourceString(R.string.pref_showScrollIndicator_defaultValue).toBoolean())
        mShowSongTitle = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showSongTitle_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongTitle_defaultValue).toBoolean())
        var commentDisplayTimeSeconds = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_commentDisplayTime_key), BeatPrompterApplication.getResourceString(R.string.pref_commentDisplayTime_default).toInt())
        commentDisplayTimeSeconds += Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_commentDisplayTime_offset))
        mCommentDisplayTimeNanoseconds = Utils.milliToNano(commentDisplayTimeSeconds * 1000)

        mBeatCounterColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)))
        mCommentTextColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_commentTextColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_commentTextColor_default)))
        mDefaultCurrentLineHighlightColour = Utils.makeHighlightColour(sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_currentLineHighlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_currentLineHighlightColor_default))))
        mPulse = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_pulse_key), BeatPrompterApplication.getResourceString(R.string.pref_pulse_defaultValue).toBoolean())
        mMetronomePref = MetronomeContext.getMetronomeContextPreference(sharedPrefs)

        mSongTitleContrastBeatCounter = Utils.makeContrastingColour(mBeatCounterColor)
        val backgroundColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
        val pulseColor=
                if (mPulse)
                    sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
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

    // Constructor
    constructor(context: Context) : this(context,null)

    fun init(songDisplayActivity: SongDisplayActivity,song:Song) {
        mSongDisplayActivity = songDisplayActivity
        mSongScrollEndPixel=calculateScrollEnd(song,mScreenHeight)
        val sharedPrefs = BeatPrompterApplication.preferences
        mExternalTriggerSafetyCatch = TriggerSafetyCatch.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_midiTriggerSafetyCatch_key), BeatPrompterApplication.getResourceString(R.string.pref_midiTriggerSafetyCatch_defaultValue))!!)

            // TODO: FIX METRONOME FOR MIXED MODE
/*        if (song.mSongFile.mBPM > 0.0) {
            var metronomeOn = metronomePref==MetronomeContext.On
            val metronomeOnWhenNoBackingTrack = metronomePref==MetronomeContext.OnWhenNoTrack
            val metronomeCount = metronomePref==MetronomeContext.DuringCountIn

            if (metronomeOnWhenNoBackingTrack && song.mEvents.any{it is AudioEvent})
                metronomeOn = true

            if (metronomeOn)
                mMetronomeBeats = Long.MAX_VALUE
            else if (metronomeCount)
                mMetronomeBeats = (song.mCountIn * song.mInitialBPB).toLong()
        }*/

        mSilenceMediaPlayer.isLooping = true
        mSilenceMediaPlayer.setVolume(0.01f, 0.01f)
        mSilenceMediaPlayer.start()

        song.mAudioEvents.forEach{
            val mediaPlayer=MediaPlayer()
            // Shitty Archos workaround.
            try {
                val fis = FileInputStream(it.mAudioFile.mFile.absolutePath)
                fis.use {stream->
                    with(mediaPlayer)
                    {
                        setDataSource(stream.fd)
                        prepare()
                        seekTo(0)
                        setVolume(0.01f * it.mVolume, 0.01f * it.mVolume)
                        isLooping = false
                    }
                }
            } catch (e: Exception) {
                val toast = Toast.makeText(context, R.string.crap_audio_file_warning, Toast.LENGTH_LONG)
                toast.show()
            }
            mMediaPlayers[it.mAudioFile] = mediaPlayer
        }

        mSendMidiClock = song.mSendMIDIClock || sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        mBeatCountRect = Rect(0, 0, song.mBeatCounterRect.width(), song.mBeatCounterRect.height())
        mHighlightCurrentLine = song.mScrollMode === SongScrollingMode.Beat && sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_highlightCurrentLine_key), BeatPrompterApplication.getResourceString(R.string.pref_highlightCurrentLine_defaultValue).toBoolean())
        mSong=song
    }

    private fun ensureInitialised() {
        if (mSong == null)
            return
        if (!mInitialized) {
            if (mSong!!.mScrollMode === SongScrollingMode.Smooth)
                mPulse = false
            mInitialized = true
            // TODO: FIX METRONOME
/*
            if (mSong!!.mScrollMode === SongScrollingMode.Manual) {
                if (mMetronomeBeats > 0) {
                    mMetronomeTask = MetronomeTask(mSong!!.mSongFile.mBPM, mMetronomeBeats)
                    mMetronomeThread = Thread(mMetronomeTask)
                    // Infinite metronome? Might as well start it now.
                    if (mMetronomeBeats == Long.MAX_VALUE)
                        mMetronomeThread!!.start()
                }
            }
*/
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
            if (mSong!!.mScrollMode !== SongScrollingMode.Manual) {
                var event: BaseEvent?
                do {
                    event = mSong!!.getNextEvent(timePassed)
                    if (event is CommentEvent)
                        processCommentEvent(event, time)
                    else if (event is BeatEvent)
                        processBeatEvent(event, true)
                    else if (event is MIDIEvent)
                        processMIDIEvent(event)
                    else if (event is PauseEvent)
                        processPauseEvent(event)
                    else if (event is LineEvent)
                        processLineEvent(event)
                    else if (event is AudioEvent)
                        processAudioEvent(event)
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
        var currentY = mSong!!.mBeatCounterRect.height()
        var currentLine = mSong!!.mCurrentLine
        var yScrollOffset = 0
        val color = mBackgroundColorLookup[(beatPercent * 100.0).toInt()]
        canvas.drawColor(color, PorterDuff.Mode.SRC)
        if (mStartState !== PlayState.AtTitleScreen)
            if (currentLine != null) {
                var scrollPercentage = 0.0
                // If a scroll event in underway, move currentY up
                if (mStartState !== PlayState.Playing || mSong!!.mScrollMode === SongScrollingMode.Manual) {
                    yScrollOffset = mSongPixelPosition - currentLine.mSongPixelPosition
                    if (mSong!!.mScrollMode === SongScrollingMode.Smooth)
                        scrollPercentage = yScrollOffset.toDouble() / currentLine.mMeasurements.mLineHeight.toDouble()
                } else {
                    if (!scrolling && mSong!!.mScrollMode !== SongScrollingMode.Manual) {
                        if (currentLine.mYStopScrollTime > timePassed && currentLine.mYStartScrollTime <= timePassed)
                            scrollPercentage = (timePassed - currentLine.mYStartScrollTime).toDouble() / (currentLine.mYStopScrollTime - currentLine.mYStartScrollTime).toDouble()
                        else if (currentLine.mYStopScrollTime <= timePassed)
                            scrollPercentage = 1.0
                        if (mSong!!.mScrollMode === SongScrollingMode.Smooth)
                            yScrollOffset = (currentLine.mMeasurements.mLineHeight * scrollPercentage).toInt()
                        else if (mSong!!.mScrollMode === SongScrollingMode.Beat)
                            yScrollOffset = currentLine.mMeasurements.mJumpScrollIntervals[(scrollPercentage * 100.0).toInt()]
                    }
                }
                currentY -= yScrollOffset
                if (mStartState === PlayState.Playing)
                    mSongPixelPosition = currentLine.mSongPixelPosition + yScrollOffset
                if (mSong!!.mScrollMode === SongScrollingMode.Smooth)
                    currentY += mSong!!.mSmoothScrollOffset

                val startY = currentY
                var firstLineOnscreen: Line? = null
                var highlight = mHighlightCurrentLine
                while (currentLine != null && currentY < mScreenHeight) {
                    if (currentY > mSong!!.mBeatCounterRect.height() - currentLine.mMeasurements.mLineHeight) {
                        if (firstLineOnscreen == null)
                            firstLineOnscreen = currentLine
                        val graphics = currentLine.getGraphics()
                        val lineTop = currentY
                        for ((lineCounter, graphic) in graphics.withIndex()) {
                            if (!graphic.mBitmap.isRecycled)
                                canvas.drawBitmap(graphic.mBitmap, 0f, currentY.toFloat(), mPaint)
                            currentY += currentLine.mMeasurements.mGraphicHeights[lineCounter]
                        }
                        if (highlight) {
                            mPaint.color = mDefaultCurrentLineHighlightColour
                            canvas.drawRect(0f, lineTop.toFloat(), mScreenWidth.toFloat(), (lineTop + currentLine.mMeasurements.mLineHeight).toFloat(), mPaint)
                            mPaint.alpha = 255
                        }
                    } else
                        currentY += currentLine.mMeasurements.mLineHeight
                    currentLine = currentLine.mNextLine
                    highlight = false
                }

                if (mSong!!.mScrollMode === SongScrollingMode.Smooth) {
                    // If we've drawn the end of the last line, stop smooth scrolling.
                    val prevLine = mSong!!.mCurrentLine!!.mPrevLine
                    if (prevLine != null && startY > 0) {
                        mPaint.alpha = (255.0 - 255.0 * scrollPercentage).toInt()
                        currentY = startY - prevLine.mMeasurements.mLineHeight
                        val graphics = prevLine.getGraphics()
                        for ((lineCounter, graphic) in graphics.withIndex()) {
                            canvas.drawBitmap(graphic.mBitmap, 0f, currentY.toFloat(), mPaint)
                            currentY += prevLine.mMeasurements.mGraphicHeights[lineCounter]
                        }
                        mPaint.alpha = 255
                    }
                }
            }
        mPaint.color = mBackgroundColorLookup[100]
        canvas.drawRect(0f, 0f, mScreenWidth.toFloat(), mSong!!.mBeatCounterRect.height().toFloat(), mPaint)
        mPaint.color = mScrollMarkerColor
        if (mSong!!.mScrollMode === SongScrollingMode.Beat && mShowScrollIndicator && mScrollIndicatorRect != null)
            canvas.drawRect(mScrollIndicatorRect!!, mPaint)
        mPaint.color = mBeatCounterColor
        canvas.drawRect(mBeatCountRect, mPaint)
        canvas.drawLine(0f, mSong!!.mBeatCounterRect.height().toFloat(), mScreenWidth.toFloat(), mSong!!.mBeatCounterRect.height().toFloat(), mPaint)
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
        if ((mScreenAction == ScreenAction.Scroll || mSong!!.mScrollMode === SongScrollingMode.Manual) && mScroller.computeScrollOffset()) {
            mSongPixelPosition = mScroller.currY
            //if (mSong.mSongScrollingMode != SongScrollingMode.Manual)
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
            mPaint.color = if (mSkipping) Color.RED else Color.WHITE
            val halfDiff = (fifteenPercent - nextSongSS.mHeight) / 2.0f
            canvas.drawRect(0f, mScreenHeight - fifteenPercent, mScreenWidth.toFloat(), mScreenHeight.toFloat(), mPaint)
            val nextSongY = mScreenHeight - (nextSongSS.mDescenderOffset + halfDiff).toInt()
            startY -= (fifteenPercent / 2.0f).toInt()
            with(mPaint) {
                color = nextSongSS.mColor
                textSize = nextSongSS.mFontSize * Utils.FONT_SCALING
                typeface = nextSongSS.mFace
                flags = Paint.ANTI_ALIAS_FLAG
            }
            canvas.drawText(nextSongSS.mText, (midX - (nextSongSS.mWidth shr 1)).toFloat(), nextSongY.toFloat(), mPaint)
        }
        for (ss in mSong!!.mStartScreenStrings) {
            startY += ss.mHeight
            with(mPaint){
                color = ss.mColor
                textSize = ss.mFontSize * Utils.FONT_SCALING
                typeface = ss.mFace
                flags = Paint.ANTI_ALIAS_FLAG
            }
            canvas.drawText(ss.mText, (midX - (ss.mWidth shr 1)).toFloat(), (startY - ss.mDescenderOffset).toFloat(), mPaint)
        }
    }

/*    private fun calculateManualScrollingPositions(firstLineOnscreen: Line?, currentLine: Line?, currentY: Int, startOnscreen: Boolean, endOnscreen: Boolean) {
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
                mPageDownPixel = if (mSong!!.mLastLine != null && currentY - mSong!!.mLastLine!!.mMeasurements.mLineHeight >= 0)
                // Yes? Then move the last line to the top of the screen
                    mSong!!.mLastLine!!.mSongPixelPosition
                else
                // No? Then don't move.
                    mSongPixelPosition
            vCurrentLine == null -> // Does the entire last line fit onscreen?
                mPageDownPixel = if (mSong!!.mLastLine != null && mSong!!.mLastLine!!.mMeasurements.mLineHeight < mScreenHeight)
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
                totalHeight += vCurrentLine.mMeasurements.mLineHeight
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
    }*/

    // Called back when the view is first created or its size changes.
    public override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        // Set the movement bounds for the ball
        // Can't use wider than 4096, as can't create bitmaps bigger than that.
        mScreenWidth = Math.min(w, 4096)
        mScreenHeight = h
        if (mSong != null)
            mSongScrollEndPixel=calculateScrollEnd(mSong!!,mScreenHeight)
        mPageUpDownScreenHeight = (mScreenHeight * 0.8).toInt()
    }

    private fun showTempMessage(message: String, textSize: Int, textColor: Int, canvas: Canvas) {
        val popupMargin = 25
        mPaint.textSize = textSize * Utils.FONT_SCALING
        mPaint.flags = Paint.ANTI_ALIAS_FLAG
        val outRect = Rect()
        mPaint.getTextBounds(message, 0, message.length, outRect)
        val textWidth = mPaint.measureText(message)
        val textHeight = outRect.height()
        val volumeControlWidth = textWidth + popupMargin * 2.0f
        val volumeControlHeight = textHeight + popupMargin * 2
        val x = (mScreenWidth - volumeControlWidth) / 2.0f
        val y = (mScreenHeight - volumeControlHeight) / 2
        mPaint.color = Color.BLACK
        canvas.drawRect(x, y.toFloat(), x + volumeControlWidth, (y + volumeControlHeight).toFloat(), mPaint)
        mPaint.color = Color.rgb(255, 255, 200)
        canvas.drawRect(x + 1, (y + 1).toFloat(), x + (volumeControlWidth - 2), (y + (volumeControlHeight - 2)).toFloat(), mPaint)
        mPaint.color = textColor
        canvas.drawText(message, (mScreenWidth - textWidth) / 2, ((mScreenHeight - textHeight) / 2 + textHeight).toFloat(), mPaint)
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

        canvas.drawText(mSong!!.mSongTitleHeader.mText, mSong!!.mSongTitleHeaderLocation.x, mSong!!.mSongTitleHeaderLocation.y, mPaint)

        canvas.save()
        canvas.clipRect(mBeatCountRect)
        mPaint.color = mSongTitleContrastBeatCounter
        canvas.drawText(mSong!!.mSongTitleHeader.mText, mSong!!.mSongTitleHeaderLocation.x, mSong!!.mSongTitleHeaderLocation.y, mPaint)
        canvas.restore()

        if (mScrollIndicatorRect != null) {
            canvas.save()
            canvas.clipRect(mScrollIndicatorRect!!)
            canvas.drawText(mSong!!.mSongTitleHeader.mText, mSong!!.mSongTitleHeaderLocation.x, mSong!!.mSongTitleHeaderLocation.y, mPaint)
            canvas.restore()
        }

        mPaint.alpha = 255
    }

    private fun showComment(canvas: Canvas) {
        if (mLastCommentEvent != null) {
            with(mPaint)
            {
                textSize = mLastCommentEvent!!.mComment.mScreenString!!.mFontSize * Utils.FONT_SCALING
                flags = Paint.ANTI_ALIAS_FLAG
                color = Color.BLACK
            }
            canvas.drawRect(mLastCommentEvent!!.mComment.mPopupRect!!, mPaint)
            mPaint.color = Color.WHITE
            canvas.drawRect(mLastCommentEvent!!.mComment.mPopupRect!!.left + 1, mLastCommentEvent!!.mComment.mPopupRect!!.top + 1, mLastCommentEvent!!.mComment.mPopupRect!!.right - 1, mLastCommentEvent!!.mComment.mPopupRect!!.bottom - 1, mPaint)
            mPaint.color = mCommentTextColor
            mPaint.alpha = 255
            canvas.drawText(mLastCommentEvent!!.mComment.mText, mLastCommentEvent!!.mComment.mTextDrawLocation!!.x, mLastCommentEvent!!.mComment.mTextDrawLocation!!.y, mPaint)
        }
    }

    fun startToggle(e: MotionEvent?, midiInitiated: Boolean, playState: PlayState) {
        mStartState = playState
        startToggle(e, midiInitiated)
    }

    private fun startBackingTrack():Boolean
    {
        val mediaPlayer=mMediaPlayers[mSong!!.mBackingTrack]
        mediaPlayer?.start()
        return mediaPlayer!=null
    }

    private fun startToggle(e: MotionEvent?, midiInitiated: Boolean): Boolean {
        if (mSong == null)
            return true
        if (mStartState !== PlayState.Playing) {
            if (mStartState === PlayState.AtTitleScreen)
                if (e != null)
                    if (e.y > mScreenHeight * 0.85f)
                        if (!mSong!!.mNextSong.isBlank()) {
                            endSong(true)
                            return true
                        }
            val oldPlayState = mStartState
            mStartState = PlayState.increase(mStartState)
            if (mStartState === PlayState.Playing) {
                if (mSong!!.mScrollMode=== SongScrollingMode.Manual) {
                    // Start the count in.
                    if (mManualMetronomeThread != null) {
                        if (!mManualMetronomeThread!!.isAlive) {
                            return if (mMetronomeBeats != 0L) {
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
            } else if (mSong!!.mScrollMode !== SongScrollingMode.Manual) {
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
        mMediaPlayers.values.forEach{
            if(it.isPlaying)
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
            BluetoothManager.broadcastMessageToClients(QuitSongMessage())
            if (mSong != null)
                mSong!!.recycleGraphics()
            mSong = null
            Task.stopTask(mManualMetronomeTask, mManualMetronomeThread)
            mMediaPlayers.values.forEach{
                it.stop()
                it.release()
            }
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

    private fun processBeatEvent(event: BeatEvent?, allowClick: Boolean) {
        if (event == null)
            return
        val playClick=allowClick && (mMetronomePref!==MetronomeContext.OnWhenNoTrack || !isTrackPlaying())
        mNanosecondsPerBeat = Utils.nanosecondsPerBeat(event.mBPM)
        val beatWidth = mScreenWidth.toDouble() / event.mBPB.toDouble()
        val currentBeatCounterWidth = (beatWidth * (event.mBeat + 1).toDouble()).toInt()
        mScrollIndicatorRect = if (event.mWillScrollOnBeat != -1) {
            val thirdWidth = beatWidth / 3
            val thirdHeight = mSong!!.mBeatCounterRect.height() / 3.0
            val scrollIndicatorStart = (beatWidth * event.mWillScrollOnBeat + thirdWidth).toInt()
            val scrollIndicatorEnd = (beatWidth * (event.mWillScrollOnBeat + 1) - thirdWidth).toInt()
            Rect(scrollIndicatorStart, thirdHeight.toInt(), scrollIndicatorEnd, (thirdHeight * 2.0).toInt())
        } else
            null
        mBeatCountRect = Rect((currentBeatCounterWidth - beatWidth).toInt(), 0, currentBeatCounterWidth, mSong!!.mBeatCounterRect.height())
        mLastBeatTime = mSongStartTime + event.mEventTime
        if (event.mClick && mStartState === PlayState.Playing && mSong!!.mScrollMode !== SongScrollingMode.Manual && playClick)
            mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f)
        if (mSongDisplayActivity != null/*&&(!event.mCount)*/)
            mSongDisplayActivity!!.onSongBeat(event.mBPM)
    }

    private fun isTrackPlaying():Boolean{
        return mMediaPlayers.values.any{it.isPlaying}
    }

    private fun processPauseEvent(event: PauseEvent?) {
        if (event == null)
            return
        mLastBeatTime = -1
        val currentBeatCounterWidth = (mScreenWidth.toDouble() / (event.mBeats - 1).toDouble() * event.mBeat.toDouble()).toInt()
        mBeatCountRect = Rect(0, 0, currentBeatCounterWidth, mSong!!.mBeatCounterRect.height())
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

    private fun processAudioEvent(event:AudioEvent): Boolean {
        val mediaPlayer= mMediaPlayers[event.mAudioFile] ?: return false
        Log.d(BeatPrompterApplication.TAG, "Track event hit: starting MediaPlayer")
        mediaPlayer.start()
        return true
    }

    private fun processEndEvent(): Boolean {
        // End the song in beat mode, or if we're using a track in any other mode.
        val end = mSong!!.mScrollMode === SongScrollingMode.Beat
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

    private fun seekTrack(audioFile:AudioFile,time: Int):MediaPlayer {
        val mediaPlayer= mMediaPlayers[audioFile]
        mediaPlayer!!.seekTo(time)
        return mediaPlayer
    }

    fun setSongTime(nano: Long, redraw: Boolean, broadcast: Boolean, setPixelPosition: Boolean) {
        if (mSong == null)
            return
        // No time context in Manual mode.
        if (setPixelPosition)
            mSongPixelPosition = mSong!!.getPixelFromTime(nano)
        //        if(mSong.mSongScrollingMode!=SongScrollingMode.Manual)
        run {
            if (mStartState !== PlayState.Playing)
                mPauseTime = nano
            if (broadcast)
                BluetoothManager.broadcastMessageToClients(SetSongTimeMessage(nano))
            mSong!!.setProgress(nano)
            if (mSong!!.mScrollMode !== SongScrollingMode.Manual) {
                val prevBeatEvent = mSong!!.mCurrentEvent.mPrevBeatEvent
                val nextBeatEvent = mSong!!.mCurrentEvent.nextBeatEvent
                if (prevBeatEvent != null)
                    processBeatEvent(prevBeatEvent, nextBeatEvent != null)
            }
            mSongStartTime = System.nanoTime() - nano
            if (mSong!!.mScrollMode !== SongScrollingMode.Manual) {
                val audioEvent = mSong!!.mCurrentEvent.mPrevAudioEvent
                if (audioEvent != null) {
                    val nTime = Utils.nanoToMilli(nano - audioEvent.mEventTime)
                    val mediaPlayer=seekTrack(audioEvent.mAudioFile,nTime)
                    if (mStartState === PlayState.Playing) {
                        Log.d(BeatPrompterApplication.TAG, "Starting MediaPlayer")
                        mediaPlayer.start()
                    }
                }
            }
            if (redraw)
                invalidate()
        }
    }

    override fun onDown(e: MotionEvent): Boolean {
        if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            if (mManualMetronomeThread != null)
                if (mStartState === PlayState.Playing)
                    mManualMetronomeThread!!.interrupt()
        // Abort any active scroll animations and invalidate.
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollMode === SongScrollingMode.Manual)
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
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollMode === SongScrollingMode.Manual) {
            clearScrollTarget()
            mSongPixelPosition += distanceY.toInt()
            mSongPixelPosition = Math.max(0, mSongPixelPosition)
            mSongPixelPosition = Math.min(mSongScrollEndPixel, mSongPixelPosition)
            pauseOnScrollStart()
            //            if(mSong.mSongScrollingMode!=SongScrollingMode.Manual)
            setSongTime(mSong!!.mCurrentLine!!.getTimeFromPixel(mSongPixelPosition), true, true, false)
        } else if (mScreenAction == ScreenAction.Volume) {
            mCurrentVolume += (distanceY / 10.0).toInt()
            onVolumeChanged()
        }
        return true
    }

    fun pauseOnScrollStart() {
        if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            return
        if (mScreenAction != ScreenAction.Scroll)
            return
        BluetoothManager.broadcastMessageToClients(PauseOnScrollStartMessage())
        mUserHasScrolled = true
        mStartState = PlayState.Paused
        mMediaPlayers.values.forEach{
            Log.d(BeatPrompterApplication.TAG, "Pausing MediaPlayers")
            if(it.isPlaying)
                it.pause()
        }
        if (mSongDisplayActivity != null)
            mSongDisplayActivity!!.onSongStop()
    }

    private fun onVolumeChanged() {
        mCurrentVolume = Math.max(0, mCurrentVolume)
        mCurrentVolume = Math.min(100, mCurrentVolume)
        mMediaPlayers.values.forEach{
            it.setVolume(0.01f * mCurrentVolume, 0.01f * mCurrentVolume)
        }
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
        if (mScreenAction == ScreenAction.Scroll || mSong!!.mScrollMode === SongScrollingMode.Manual) {
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
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changeThePageDown()
        } else if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            changeThePageDown()
        else
            changeVolume(+5)
    }

    fun onPageUpKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changePage(false)
        } else if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            changePage(false)
        else
            changeVolume(-5)
    }

    private fun changeTheLineDown() {
        if (mSongPixelPosition == mSongScrollEndPixel)
            if (++mEndSongByPedalCounter == SONG_END_PEDAL_PRESSES)
                endSong(false)
            else
                mLastTempMessageTime = System.nanoTime()
        else
            changeLine(true)
    }

    fun onLineDownKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changeTheLineDown()
        } else if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            changeTheLineDown()
        else
            changeVolume(+5)
    }

    fun onLineUpKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changeLine(false)
        } else if (mSong!!.mScrollMode === SongScrollingMode.Manual)
            changeLine(false)
        else
            changeVolume(-5)
    }

    fun onLeftKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changeVolume(-5)
        } else
            changeVolume(-5)
    }

    fun onRightKeyPressed() {
        if (mStartState !== PlayState.Playing) {
            if (!startToggle(null, false) && mSong!!.mScrollMode === SongScrollingMode.Manual)
                changeVolume(+5)
        } else
            changeVolume(+5)
    }

    private fun changePage(down: Boolean) {
        if (mStartState === PlayState.AtTitleScreen)
            return
        if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
            return
        mTargetPixelPosition = if (down) mSong!!.mCurrentLine?.mManualScrollPositions?.mPageDown?:mSongScrollEndPixel else mSong!!.mCurrentLine?.mManualScrollPositions?.mPageUp?:0
    }

    private fun changeLine(down: Boolean) {
        if (mStartState === PlayState.AtTitleScreen)
            return
        if (mTargetPixelPosition != -1 && mTargetPixelPosition != mSongPixelPosition)
            return
        mTargetPixelPosition = if (down) mSong!!.mCurrentLine?.mManualScrollPositions?.mLineDown?:mSongScrollEndPixel else mSong!!.mCurrentLine?.mManualScrollPositions?.mLineUp?:0
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
            TriggerSafetyCatch.WhenAtTitleScreenOrPaused -> mStartState !== PlayState.Playing || mSong != null && mSong!!.mScrollMode === SongScrollingMode.Manual
            TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine -> mStartState !== PlayState.Playing || mSong == null || mSong!!.mCurrentLine == null || mSong!!.mCurrentLine!!.mNextLine == null || mSong!!.mScrollMode === SongScrollingMode.Manual
            TriggerSafetyCatch.Never -> false
            else -> false
        }
    }

    internal inner class ManualMetronomeTask(bpm: Double, private var mBeats: Long) : Task(true) {
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

        private fun calculateScrollEnd(song:Song,screenHeight:Int):Int {
            var songDisplayEndPixel = song.mSongHeight
            if (song.mScrollMode !== SongScrollingMode.Beat)
                songDisplayEndPixel -= screenHeight
            var songScrollEndPixel = Math.max(0, songDisplayEndPixel)
            if (song.mScrollMode === SongScrollingMode.Smooth) {
                songScrollEndPixel += song.mSmoothScrollOffset
                songScrollEndPixel += song.mBeatCounterRect.height()
            }
            return songScrollEndPixel
        }
    }
}
