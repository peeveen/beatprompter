package com.stevenfrew.beatprompter

import android.graphics.*
import android.os.Handler
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.event.BaseEvent
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.event.CommentEvent
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class Song(var mSongFile: SongFile, internal var mChosenBackingTrack: AudioFile?,
           internal var mChosenBackingTrackVolume: Int, private val mInitialComments: List<Comment>, firstEvent: BaseEvent,
           firstLine: Line, private val mParseErrors: MutableList<FileParseError>, internal var mScrollingMode: ScrollingMode,
           internal var mSendMidiClock: Boolean, internal var mStartedByBandLeader: Boolean, internal var mNextSong: String?,
           internal var mOrientation: Int, internal var mInitialMIDIMessages: List<OutgoingMessage>,
           private val mBeatBlocks: List<BeatBlock>, internal var mInitialBPB: Int, var mCountIn: Int) {
    private var mFirstLine: Line? = null // First line to show.
    internal var mCurrentLine: Line? = null
    internal var mLastLine: Line? = null
    internal var mSongTitleHeaderLocation: PointF?=null
    internal var mSongTitleHeader: ScreenString?=null
    internal var mFirstEvent: BaseEvent // First event in the event chain.
    internal var mCurrentEvent: BaseEvent? = null // Last event that executed.
    private var mNextEvent: BaseEvent? = null // Upcoming event.
    var mCancelled = false
    private val mNumberOfMIDIBeatBlocks: Int
    internal var mBeatCounterRect: Rect=Rect()
    internal var mBeatCounterHeight: Int = 0
    internal var mSmoothScrollOffset: Int = 0
    internal var mSongHeight = 0
    private var mMaxLineHeight = 0
    internal var mStartScreenStrings = mutableListOf<ScreenString>()
    internal var mNextSongString: ScreenString? = null
    internal var mTotalStartScreenTextHeight: Int = 0

    init {
        mCurrentEvent = firstEvent
        mFirstEvent = mCurrentEvent!!
        mNumberOfMIDIBeatBlocks = mBeatBlocks.size
        mNextEvent = mFirstEvent
        mFirstLine = firstLine
        mCurrentLine = mFirstLine
        mLastLine = mFirstLine
        if (mLastLine != null)
            while (mLastLine!!.mNextLine != null)
                mLastLine = mLastLine!!.mNextLine
    }

    internal fun setProgress(nano: Long) {
        var e = mCurrentEvent
        if (e == null)
            e = mFirstEvent
        val newCurrentEvent = e.findEventOnOrBefore(nano)
        mCurrentEvent = newCurrentEvent
        mNextEvent = mCurrentEvent!!.mNextEvent
        val newCurrentLineEvent = newCurrentEvent!!.mPrevLineEvent
        mCurrentLine = newCurrentLineEvent?.mLine ?: mFirstLine
    }

    fun doMeasurements(paint: Paint, cancelEvent: CancelEvent, handler: Handler, nativeSettings: SongDisplaySettings, sourceSettings: SongDisplaySettings) {
        val boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val notBoldFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val sourceScreenWidth = sourceSettings.mScreenWidth
        val sourceScreenHeight = sourceSettings.mScreenHeight
        val sourceRatio = sourceScreenWidth.toDouble() / sourceScreenHeight.toDouble()

        var nativeScreenWidth = nativeSettings.mScreenWidth
        var nativeScreenHeight = nativeSettings.mScreenHeight
        val screenWillRotate = nativeSettings.mOrientation != sourceSettings.mOrientation
        if (screenWillRotate) {
            val temp = nativeScreenHeight

            nativeScreenHeight = nativeScreenWidth
            nativeScreenWidth = temp
        }
        val nativeRatio = nativeScreenWidth.toDouble() / nativeScreenHeight.toDouble()
        val minRatio = Math.min(nativeRatio, sourceRatio)
        val maxRatio = Math.max(nativeRatio, sourceRatio)
        val ratioMultiplier = minRatio / maxRatio

        var minimumFontSize = sourceSettings.mMinFontSize.toFloat()
        var maximumFontSize = sourceSettings.mMaxFontSize.toFloat()
        minimumFontSize *= ratioMultiplier.toFloat()
        maximumFontSize *= ratioMultiplier.toFloat()

        if (minimumFontSize > maximumFontSize) {
            mParseErrors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.fontSizesAllMessedUp)))
            maximumFontSize = minimumFontSize
        }

        val sharedPref = BeatPrompterApplication.preferences

        val defaultHighlightColour = Utils.makeHighlightColour(sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default))))
        var showKey = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_defaultValue).toBoolean())
        showKey = showKey and mSongFile.mKey.isNotBlank()
        val showBPMString = sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_defaultValue))

        mBeatCounterHeight = 0
        // Top 5% of screen is used for beat counter
        if (mScrollingMode !== ScrollingMode.Manual)
            mBeatCounterHeight = (nativeScreenHeight / 20.0).toInt()
        mBeatCounterRect = Rect(0, 0, nativeScreenWidth, mBeatCounterHeight)

        val maxSongTitleWidth = nativeScreenWidth * 0.9f
        val maxSongTitleHeight = mBeatCounterHeight * 0.9f
        val vMargin = (mBeatCounterHeight - maxSongTitleHeight) / 2.0f
        mSongTitleHeader = ScreenString.create(mSongFile.mTitle, paint, maxSongTitleWidth.toInt(), maxSongTitleHeight.toInt(), Utils.makeHighlightColour(Color.BLACK, 0x80.toByte()), notBoldFont, false)
        val extraMargin = (maxSongTitleHeight - mSongTitleHeader!!.mHeight) / 2.0f
        val x = ((nativeScreenWidth - mSongTitleHeader!!.mWidth) / 2.0).toFloat()
        val y = mBeatCounterHeight - (extraMargin + mSongTitleHeader!!.mDescenderOffset.toFloat() + vMargin)
        mSongTitleHeaderLocation = PointF(x, y)
        var line = mFirstLine
        var lineCount = 0
        while (line != null) {
            ++lineCount
            line = line.mNextLine
        }
        line = mFirstLine
        var highlightColour = 0
        mSongHeight = 0
        var lastNonZeroLineHeight = 0
        var lineCounter = 0
        handler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, lineCount).sendToTarget()
        while (line != null && !cancelEvent.isCancelled) {
            highlightColour = line.measure(paint, minimumFontSize, maximumFontSize, nativeScreenWidth, nativeScreenHeight, notBoldFont, highlightColour, defaultHighlightColour, mParseErrors, mSongHeight, mScrollingMode, cancelEvent)
            var thisLineHeight = 0
            if (line.mLineMeasurements != null)
                thisLineHeight = line.mLineMeasurements!!.mLineHeight
            if (thisLineHeight > mMaxLineHeight)
                mMaxLineHeight = thisLineHeight
            if (thisLineHeight > 0)
                lastNonZeroLineHeight = thisLineHeight
            mSongHeight += thisLineHeight
            line = line.mNextLine
            handler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, ++lineCounter, lineCount).sendToTarget()
        }
        if (cancelEvent.isCancelled)
            return

        mSmoothScrollOffset = 0
        if (mScrollingMode === ScrollingMode.Smooth)
            mSmoothScrollOffset = Math.min(mMaxLineHeight, (nativeScreenHeight / 3.0).toInt())
        else if (mScrollingMode === ScrollingMode.Beat)
            mSongHeight -= lastNonZeroLineHeight

        // Measure the popup comments.
        var event: BaseEvent? = mFirstEvent
        while (event != null) {
            if (event is CommentEvent) {
                val ce = event as CommentEvent?
                ce!!.doMeasurements(nativeScreenWidth, nativeScreenHeight, paint, notBoldFont)
            }
            event = event.mNextEvent
        }

        // As for the start screen display (title/artist/comments/"press go"),
        // the title should take up no more than 20% of the height, the artist
        // no more than 10%, also 10% for the "press go" message.
        // The rest of the space is allocated for the comments and error messages,
        // each line no more than 10% of the screen height.
        var availableScreenHeight = nativeScreenHeight
        if (mNextSong != null && mNextSong!!.isNotEmpty()) {
            // OK, we have a next song title to display.
            // This should take up no more than 15% of the screen.
            // But that includes a border, so use 13 percent for the text.
            val eightPercent = (nativeScreenHeight * 0.13).toInt()
            val fullString = ">>> $mNextSong >>>"
            mNextSongString = ScreenString.create(fullString, paint, nativeScreenWidth, eightPercent, Color.BLACK, boldFont, true)
            availableScreenHeight -= (nativeScreenHeight * 0.15f).toInt()
        }
        val tenPercent = (availableScreenHeight / 10.0).toInt()
        val twentyPercent = (availableScreenHeight / 5.0).toInt()
        mStartScreenStrings.add(ScreenString.create(mSongFile.mTitle, paint, nativeScreenWidth, twentyPercent, Color.YELLOW, boldFont, true))
        if (mSongFile.mArtist != null && mSongFile.mArtist!!.isNotEmpty())
            mStartScreenStrings.add(ScreenString.create(mSongFile.mArtist!!, paint, nativeScreenWidth, tenPercent, Color.YELLOW, boldFont, true))
        val commentLines = mutableListOf<String>()
        for (c in mInitialComments)
            commentLines.add(c.mText)
        val nonBlankCommentLines = mutableListOf<String>()
        for (commentLine in commentLines)
            if (commentLine.trim().isNotEmpty())
                nonBlankCommentLines.add(commentLine.trim())
        var errors = mParseErrors.size
        var messages = Math.min(errors, 6) + nonBlankCommentLines.size
        val showBPM = !BeatPrompterApplication.getResourceString(R.string.showBPMNo).equals(showBPMString!!, ignoreCase = true) && mSongFile.mBPM != 0.0
        if (showBPM)
            ++messages
        if (showKey)
            ++messages
        if (messages > 0) {
            val remainingScreenSpace = nativeScreenHeight - twentyPercent * 2
            var spacePerMessageLine = Math.floor((remainingScreenSpace / messages).toDouble()).toInt()
            spacePerMessageLine = Math.min(spacePerMessageLine, tenPercent)
            var errorCounter = 0
            for (error in mParseErrors) {
                if (cancelEvent.isCancelled)
                    break
                mStartScreenStrings.add(ScreenString.create(error.errorMessage, paint, nativeScreenWidth, spacePerMessageLine, Color.RED, notBoldFont, false))
                ++errorCounter
                --errors
                if (errorCounter == 5 && errors > 0) {
                    mStartScreenStrings.add(ScreenString.create(String.format(BeatPrompterApplication.getResourceString(R.string.otherErrorCount), errors), paint, nativeScreenWidth, spacePerMessageLine, Color.RED, notBoldFont, false))
                    break
                }
            }
            for (nonBlankComment in nonBlankCommentLines) {
                if (cancelEvent.isCancelled)
                    break
                mStartScreenStrings.add(ScreenString.create(nonBlankComment, paint, nativeScreenWidth, spacePerMessageLine, Color.WHITE, notBoldFont, false))
            }
            if (showKey) {
                val keyString = BeatPrompterApplication.getResourceString(R.string.keyPrefix) + ": " + mSongFile.mKey
                mStartScreenStrings.add(ScreenString.create(keyString, paint, nativeScreenWidth, spacePerMessageLine, Color.CYAN, notBoldFont, false))
            }
            if (showBPM) {
                var rounded = BeatPrompterApplication.getResourceString(R.string.showBPMYesRoundedValue).equals(showBPMString, ignoreCase = true)
                if (mSongFile.mBPM == mSongFile.mBPM.toInt().toDouble())
                    rounded = true
                var bpmString = BeatPrompterApplication.getResourceString(R.string.bpmPrefix) + ": "
                bpmString += if (rounded)
                    Math.round(mSongFile.mBPM).toInt()
                else
                    mSongFile.mBPM
                mStartScreenStrings.add(ScreenString.create(bpmString, paint, nativeScreenWidth, spacePerMessageLine, Color.CYAN, notBoldFont, false))
            }
        }
        if (cancelEvent.isCancelled)
            return
        if (mScrollingMode !== ScrollingMode.Manual)
            mStartScreenStrings.add(ScreenString.create(BeatPrompterApplication.getResourceString(R.string.tapTwiceToStart), paint, nativeScreenWidth, tenPercent, Color.GREEN, boldFont, true))
        mTotalStartScreenTextHeight = 0
        for (ss in mStartScreenStrings)
            mTotalStartScreenTextHeight += ss.mHeight

        /*        if((mScrollingMode==ScrollingMode.Smooth)&&(mFirstLine!=null))
        {
            // Prevent Y scroll of all final lines that fit onscreen.
            int totalHeight=0;
            Line lastLine=mFirstLine.getLastLine();
            boolean onLastLine=true;
            while(lastLine!=null)
            {
                if(totalHeight+lastLine.mActualLineHeight>availableScreenHeight)
                    break;
                totalHeight+=lastLine.mActualLineHeight;
                lastLine.mLineEvent.remove();
                lastLine.mYStartScrollTime=lastLine.mYStopScrollTime=Long.MAX_VALUE;
                if(!onLastLine)
                    mSongHeight-=lastLine.mActualLineHeight;
                lastLine=lastLine.mPrevLine;
                onLastLine=false;
            }
            // BUT! Add height of tallest line to compensate for scrollmode line offset
            mSongHeight+=mMaxLineHeight;
        }*/

        // Allocate graphics objects.
        val maxGraphicsRequired = getMaximumGraphicsRequired(nativeScreenHeight)
        val lineGraphics = CircularGraphicsList()
        for (f in 0 until maxGraphicsRequired)
            lineGraphics.add(LineGraphic(getBiggestLineSize(f, maxGraphicsRequired)))

        line = mFirstLine
        if (line != null) {
            var graphic: LineGraphic = lineGraphics[0]
            while (line != null) {
                //                if(!line.hasOwnGraphics())
                for (f in 0 until line.mLineMeasurements!!.mLines) {
                    line.setGraphic(graphic)
                    graphic = graphic.mNextGraphic
                }
                line = line.mNextLine
            }
        }

        // In smooth scrolling mode, the last screenful of text should never leave the screen.
        if (mScrollingMode === ScrollingMode.Smooth) {
            var total = nativeScreenHeight - mSmoothScrollOffset - mBeatCounterHeight
            var prevLastLine: Line? = null
            line = mLastLine

            while (line != null) {
                total -= line.mLineMeasurements!!.mLineHeight
                if (total <= 0) {
                    if (prevLastLine != null)
                        prevLastLine.mYStopScrollTime = Long.MAX_VALUE
                    break
                }
                line.mLineEvent.remove()
                // TODO: POTENTIAL BREAKING CHANGE!!! line.mLineEvent = null
                //                line.mLineEvent.mEventTime=Long.MAX_VALUE;
                prevLastLine = line
                line = line.mPrevLine
            }
        }
    }

    internal fun getNextEvent(time: Long): BaseEvent? {
        if (mNextEvent != null && mNextEvent!!.mEventTime <= time) {
            mCurrentEvent = mNextEvent
            mNextEvent = mNextEvent!!.mNextEvent
            return mCurrentEvent
        }
        return null
    }

    private fun getBiggestLineSize(index: Int, modulus: Int): Rect {
        var line = mFirstLine
        var maxHeight = 0
        var maxWidth = 0
        var lineCount = 0
        while (line != null) {
            //if(!line.hasOwnGraphics())
            run {
                for (lh in line!!.mLineMeasurements!!.mGraphicHeights) {
                    if (lineCount % modulus == index) {
                        maxHeight = Math.max(maxHeight, lh)
                        maxWidth = Math.max(maxWidth, line!!.mLineMeasurements!!.mLineWidth)
                    }
                    ++lineCount
                }
            }
            line = line.mNextLine
        }
        return Rect(0, 0, maxWidth - 1, maxHeight - 1)
    }

    private fun getMaximumGraphicsRequired(screenHeight: Int): Int {
        var line = mFirstLine
        var maxLines = 0
        while (line != null) {
            var thisLine = line
            var heightCounter = 0
            var lineCounter = 0
            while (thisLine != null && heightCounter < screenHeight) {
                //                if(!thisLine.hasOwnGraphics())
                run {
                    // Assume height of first line to be 1 pixel
                    // This is the state of affairs when the top line is almost
                    // scrolled offscreen, but not quite.
                    var lineHeight = 1
                    if (lineCounter > 0)
                        lineHeight = thisLine!!.mLineMeasurements!!.mLineHeight
                    heightCounter += lineHeight
                    lineCounter += thisLine!!.mLineMeasurements!!.mLines
                }
                thisLine = thisLine.mNextLine
            }

            maxLines = Math.max(maxLines, lineCounter)
            line = line.mNextLine
        }
        return maxLines
    }

    internal fun getTimeFromPixel(pixel: Int): Long {
        if (pixel == 0)
            return 0
        return if (mCurrentLine != null) mCurrentLine!!.getTimeFromPixel(pixel) else mFirstLine!!.getTimeFromPixel(pixel)
    }

    internal fun getPixelFromTime(time: Long): Int {
        if (time == 0L)
            return 0
        return if (mCurrentLine != null) mCurrentLine!!.getPixelFromTime(time) else mFirstLine!!.getPixelFromTime(time)
    }

    internal fun recycleGraphics() {
        var line = mFirstLine
        while (line != null) {
            line.recycleGraphics()
            line = line.mNextLine
        }
    }

    internal fun getMIDIBeatTime(beat: Int): Long {
        for (f in 0 until mNumberOfMIDIBeatBlocks) {
            val (blockStartTime, midiBeatCount, nanoPerBeat) = mBeatBlocks[f]
            if (midiBeatCount <= beat && (f + 1 == mNumberOfMIDIBeatBlocks || mBeatBlocks[f + 1].midiBeatCount > beat)) {
                return (blockStartTime + nanoPerBeat * (beat - midiBeatCount)).toLong()
            }
        }
        return 0
    }
}
