package com.stevenfrew.beatprompter.cache.parse

import android.graphics.*
import android.os.Handler
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.collections.CircularGraphicsList
import com.stevenfrew.beatprompter.collections.EventList
import com.stevenfrew.beatprompter.collections.LineList
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.songload.SongLoadCancelEvent
import com.stevenfrew.beatprompter.songload.SongLoadInfo
import com.stevenfrew.beatprompter.songload.SongLoadMode

class SongParser constructor(private val mSongLoadInfo: SongLoadInfo, private val mSongLoadCancelEvent: SongLoadCancelEvent, private val mSongLoadHandler: Handler, private val mRegistered:Boolean):SongFileParser<Song>(mSongLoadInfo.mSongFile,mSongLoadInfo.mSongLoadMode.toSongScrollMode()) {
    private val mMetronomeContext:MetronomeContext
    private val mCustomCommentsUser:String
    private val mShowChords:Boolean
    private val mShowKey:Boolean
    private val mShowBPM:ShowBPM
    private val mTriggerContext: TriggerOutputContext
    private val mNativeDeviceSettings:SongDisplaySettings
    private val mInitialMIDIMessages = mutableListOf<OutgoingMessage>()
    private var mStopAddingStartupItems = false
    private val mStartScreenComments=mutableListOf<Comment>()
    private val mEvents= EventList()
    private val mLines= LineList()
    private val mRolloverBeats=mutableListOf<BeatEvent>()
    private val mBeatBlocks = mutableListOf<BeatBlock>()
    private val mPaint= Paint()
    private val mFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val mBeatCounterRect:Rect
    private val mDefaultHighlightColor:Int
    private val mAudioTags=mutableListOf<AudioTag>()
    private val mSmoothScrollTimings:SmoothScrollingTimings?

    private var mSongHeight=0
    private var mMIDIBeatCounter:Int=0
    private var mLastBeatBlock: BeatBlock? = null
    private var mBeatsToAdjust:Int=0
    private var mCurrentBeat:Int=0
    private var mDisplayLineCounter:Int=0
    private var mCountIn:Int
    private var mSendMidiClock:Boolean=false
    private var mSongTime:Long=0
    private var mDefaultMIDIOutputChannel:Byte

    init
    {
        // All songFile info parsing errors count as our errors too.
        mErrors.addAll(mSongLoadInfo.mSongFile.mErrors)

        val sharedPrefs=BeatPrompterApplication.preferences
        mSendMidiClock = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        mCountIn = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        mMetronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPrefs)
        mDefaultHighlightColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default)))
        mCustomCommentsUser = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))?:""
        mShowChords = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        mTriggerContext = TriggerOutputContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        val defaultMIDIOutputChannelPrefValue = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
        mShowKey = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_defaultValue).toBoolean()) && mSongLoadInfo.mSongFile.mKey.isNotBlank()
        mShowBPM = if(mSongLoadInfo.mSongFile.mBPM>0.0) ShowBPM.getShowBPMPreference(sharedPrefs) else ShowBPM.No

        // Figure out the screen size
        mNativeDeviceSettings=translateSourceDeviceSettingsToNative(mSongLoadInfo.mSourceDisplaySettings,mSongLoadInfo.mNativeDisplaySettings)
        val beatCounterHeight =
                // Top 5% of screen is used for beat counter
                if (mSongLoadInfo.mSongLoadMode !== SongLoadMode.Manual)
                    (mNativeDeviceSettings.mScreenSize.height() / 20.0).toInt()
                else
                    0
        mBeatCounterRect = Rect(0, 0, mNativeDeviceSettings.mScreenSize.width(), beatCounterHeight)

        // Start the progress message dialog
        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mSongLoadInfo.mSongFile.mLines).sendToTarget()

        val lengthOfBackingTrack=mSongLoadInfo.mTrack?.mDuration?:0L
        val songTime=if(mSongLoadInfo.mSongFile.mDuration==Utils.TRACK_AUDIO_LENGTH_VALUE) lengthOfBackingTrack else mSongLoadInfo.mSongFile.mDuration
        mSmoothScrollTimings=
            if(songTime>0L) {
                val linesInTheSong=mSongLoadInfo.mSongFile.mLines
                val barsInTheSong=mSongLoadInfo.mSongFile.mBars
                val timePerLine=(songTime.toDouble()/linesInTheSong).toLong()
                val timePerBar=(songTime.toDouble()/barsInTheSong).toLong()
                SmoothScrollingTimings(timePerLine, timePerBar, lengthOfBackingTrack)
            }
            else
                null
    }

    override fun parseLine(line: TextFileLine<Song>) {
        super.parseLine(line)

        val chordTags=line.mTags.filterIsInstance<ChordTag>()
        val nonChordTags=line.mTags.filter { it !is ChordTag }
        val chordsFound = mShowChords && !chordTags.isEmpty()
        val chordsFoundButNotShowingThem=!mShowChords && chordsFound
        val tags=if(mShowChords)line.mTags.toList() else nonChordTags

        var workLine=line.mTaglessLine

        // Generate clicking beats if the metronome is on.
        // The "on when no track" logic will be performed during song playback.
        val metronomeOn = mMetronomeContext === MetronomeContext.On || mMetronomeContext === MetronomeContext.OnWhenNoTrack

        var imageTag=tags.filterIsInstance<ImageTag>().firstOrNull()

        if(!mSendMidiClock)
            mSendMidiClock=tags.any{it is SendMIDIClockTag}

        if(!mStopAddingStartupItems)
            mCountIn=tags.filterIsInstance<CountTag>().firstOrNull()?.mCount?:mCountIn

        val commentTags=tags.filterIsInstance<CommentTag>()
        commentTags.forEach {
            val comment = Comment(it.mComment,it.mAudience,mNativeDeviceSettings.mScreenSize,mPaint,mFont)
            if(comment.isIntendedFor(mCustomCommentsUser))
                if (mStopAddingStartupItems)
                    mEvents.add(CommentEvent(mSongTime, comment))
                else
                    mStartScreenComments.add(comment)
        }

        val midiEventTags=tags.filterIsInstance<MIDIEventTag>()
        midiEventTags.forEach {
            if (mDisplayLineCounter < DEMO_LINE_COUNT || mRegistered)
            {
                val midiEvent=it.mEvent
                if (mStopAddingStartupItems)
                    mEvents.add(midiEvent)
                else
                {
                    mInitialMIDIMessages.addAll(midiEvent.mMessages)
                    if (midiEvent.mOffset != EventOffset.NoOffset)
                        mErrors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.midi_offset_before_first_line)))
                }
            }
        }

        val audioTagsThisLine=tags.filterIsInstance<AudioTag>()
        // If there are multiple {audio} tags on a line, and we've actually reached song content, add an error.
        // You should only be allowed to define multiple {audio} tags in the pre-song section.
        if(mStopAddingStartupItems) {
            if (audioTagsThisLine.size > 1) {
                mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.multiple_audio_tags_on_song_line)))
                mAudioTags.add(audioTagsThisLine.first())
            }
        }
        else
            mAudioTags.addAll(audioTagsThisLine)

        val createLine= (workLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || imageTag != null)
        // Contains only tags? Or contains nothing? Don't use it as a blank line.

        val pauseTag=tags.filterIsInstance<PauseTag>().firstOrNull()
        if (createLine || pauseTag!=null) {
            // We definitely have a line!
            // So now is when we want to create the count-in (if any)
            if(mCountIn>0) {
                val countInEvents=generateCountInEvents(mCountIn, mMetronomeContext === MetronomeContext.DuringCountIn || metronomeOn)
                mEvents.addAll(countInEvents.second)
                mSongTime=countInEvents.first
                mCountIn=0
            }

            val audioTag=
                    when {
                        mStopAddingStartupItems -> audioTagsThisLine.firstOrNull()
                        mSongLoadInfo.mTrack==null -> mAudioTags.firstOrNull()
                        else -> {
                            val matchedAudioTag= mAudioTags.firstOrNull { it.mFilename == mSongLoadInfo.mTrack!!.mNormalizedName }
                            if(matchedAudioTag==null)
                                mErrors.add(FileParseError(line.mLineNumber,BeatPrompterApplication.getResourceString(R.string.missing_audio_file_warning)))
                            matchedAudioTag
                        }
                    }
            mAudioTags.clear()

            if(audioTag!=null) {
                // Make sure file exists.
                val mappedTracks = SongList.mCachedCloudFiles.getMappedAudioFiles(audioTag.mFilename)
                if(mappedTracks.isEmpty())
                    mErrors.add(FileParseError(audioTag,BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, audioTag.mFilename)))
                else if(mappedTracks.size>1)
                    mErrors.add(FileParseError(audioTag,BeatPrompterApplication.getResourceString(R.string.multipleFilenameMatches, audioTag.mFilename)))
                else {
                    val audioFile = mappedTracks.first()
                    if (!audioFile.mFile.exists())
                        mErrors.add(FileParseError(audioTag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, audioTag.mFilename)))
                    else
                        mEvents.add(AudioEvent(mSongTime, audioFile, audioTag.mVolume, !mStopAddingStartupItems))
                }
            }

            // Any comments or MIDI events from here will be part of the song,
            // rather than startup events.
            mStopAddingStartupItems = true

            if (imageTag != null && (workLine.isNotBlank() || chordsFound))
                mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.text_found_with_image)))
            if (workLine.isBlank() && (!chordsFound || chordsFoundButNotShowingThem))
                workLine = "▼"

            // Generate pause events if required
            val pauseEvents=
                if(pauseTag!=null)
                    generatePauseEvents(mSongTime,pauseTag.mDuration)
                else
                    null

            if (createLine)
            {
                // Won't pay? We'll take it away!
                if (mDisplayLineCounter > DEMO_LINE_COUNT && !mRegistered) {
                    workLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                    imageTag = null
                }

                // First line should always have a time of zero, so that if the user scrolls
                // back to the start of the song, it still picks up any count-in beat events.
                val lineStartTime=if(mLines.isEmpty()) 0L else mSongTime

                // If the first line is a pause event, we need to adjust the total line time accordingly
                // to include any count-in
                val addToPause=if(mLines.isEmpty()) mSongTime else 0L

                // Generate beat events (may return null in smooth mode)
                val beatEvents=generateBeatEvents(mSongTime,metronomeOn)

                // Calculate how long this line will last for
                val lineDuration=calculateLineDuration(pauseTag,addToPause,lineStartTime,beatEvents)

                // Calculate the start and stop scroll times for this line
                val startAndStopScrollTimes=calculateStartAndStopScrollTimes(pauseTag,lineStartTime+addToPause,lineDuration,beatEvents)

                // Create the line
                var lineObj: Line?=null
                if (imageTag != null) {
                    val imageFiles = SongList.mCachedCloudFiles.getMappedImageFiles(imageTag.mFilename)
                    if (imageFiles.isNotEmpty())
                        try {
                            lineObj = ImageLine(imageFiles.first(), imageTag.mImageScalingMode,lineStartTime,lineDuration,mCurrentLineBeatInfo,mNativeDeviceSettings,mSongHeight,startAndStopScrollTimes)
                        }
                        catch(e:Exception)
                        {
                            mErrors.add(FileParseError(imageTag,BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file)))
                        }
                    else {
                        imageTag = null
                        workLine = BeatPrompterApplication.getResourceString(R.string.missing_image_file_warning)
                        mErrors.add(FileParseError(imageTag, workLine))
                    }
                }
                if(imageTag==null) {
                    lineObj = TextLine(workLine, tags, lineStartTime, lineDuration, mCurrentLineBeatInfo, mNativeDeviceSettings, mLines.filterIsInstance<TextLine>().lastOrNull()?.mTrailingHighlightColor, mSongHeight, startAndStopScrollTimes, mSongLoadCancelEvent)
                }

                if(lineObj!=null)
                {
                    mSongHeight+=lineObj.mMeasurements.mLineHeight
                    mLines.add(lineObj)
                    val lineEvent=LineEvent(lineObj.mLineTime,lineObj)
                    mEvents.add(lineEvent)

                    // If a pause is specified on a line, then discard any beats.
                    if (pauseEvents!=null && pauseTag!=null) {
                        mEvents.addAll(pauseEvents)
                        mSongTime += pauseTag.mDuration
                    }
                    // Otherwise, add any generated beats
                    else if(beatEvents!=null) {
                        mEvents.addAll(beatEvents.second)
                        mSongTime=beatEvents.first
                    }
                    // Otherwise, forget it, just bump up the song time
                    else
                        mSongTime += lineDuration
                }
            }
            // This is the case where the line is blank, but we want to generate a pause
            else if (pauseEvents!=null && pauseTag!=null) {
                mEvents.addAll(pauseEvents)
                mSongTime+=pauseTag.mDuration
            }
        }

        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, line.mLineNumber, mSongLoadInfo.mSongFile.mLines).sendToTarget()
    }

    override fun getResult(): Song {
        var smoothScrollOffset=0
        if (mSongLoadInfo.mSongLoadMode === SongLoadMode.Smooth)
            smoothScrollOffset = Math.min(mLines.map{it.mMeasurements.mLineHeight}.maxBy{it}?:0, (mNativeDeviceSettings.mScreenSize.height() / 3.0).toInt())
        else if (mSongLoadInfo.mSongLoadMode === SongLoadMode.Beat)
            mSongHeight -= mLines.reversed().map{it.mMeasurements.mLineHeight}.firstOrNull{it!=0}?:0

        val startScreenStrings=createStartScreenStrings()
        val totalStartScreenTextHeight = startScreenStrings.first.sumBy { it.mHeight }

        // Each line has a number of manual scroll positions. We can set them now.
        setManualScrollPositions()

        // Get all required audio info ...
        val audioEvents=mEvents.filterIsInstance<AudioEvent>()

        // Allocate graphics objects.
        val maxGraphicsRequired = getMaximumGraphicsRequired(mNativeDeviceSettings.mScreenSize.height())
        val lineGraphics = CircularGraphicsList()
        for (f in 0 until maxGraphicsRequired)
            lineGraphics.add(LineGraphic(getBiggestLineSize(f, maxGraphicsRequired)))

        // There may be no lines! So we have to check ...
        if(lineGraphics.isNotEmpty()) {
            var graphic: LineGraphic = lineGraphics.first()
            mLines.forEach {
                for (f in 0 until it.mMeasurements.mLines) {
                    it.allocateGraphic(graphic)
                    graphic = graphic.mNextGraphic
                }
            }
        }

        val beatCounterHeight=mBeatCounterRect.height()
        val maxSongTitleWidth = mNativeDeviceSettings.mScreenSize.width() * 0.9f
        val maxSongTitleHeight = beatCounterHeight * 0.9f
        val vMargin = (beatCounterHeight - maxSongTitleHeight) / 2.0f
        val songTitleHeader = ScreenString.create(mSongLoadInfo.mSongFile.mTitle, mPaint, maxSongTitleWidth.toInt(), maxSongTitleHeight.toInt(), Utils.makeHighlightColour(Color.BLACK, 0x80.toByte()), mFont, false)
        val extraMargin = (maxSongTitleHeight - songTitleHeader.mHeight) / 2.0f
        val x = ((mNativeDeviceSettings.mScreenSize.width() - songTitleHeader.mWidth) / 2.0).toFloat()
        val y = beatCounterHeight - (extraMargin + songTitleHeader.mDescenderOffset.toFloat() + vMargin)
        val songTitleHeaderLocation = PointF(x, y)

        val lineEvents=mEvents.filterIsInstance<LineEvent>()
        val firstEvent=mEvents.buildEventChain(mSongTime)
        offsetMIDIEvents(firstEvent)

        return Song(mSongLoadInfo.mSongFile,mNativeDeviceSettings,mSongHeight,firstEvent,mLines,lineEvents,audioEvents,mInitialMIDIMessages,mBeatBlocks,mSendMidiClock,startScreenStrings.first,startScreenStrings.second,totalStartScreenTextHeight,mSongLoadInfo.mStartedByBandLeader,mSongLoadInfo.mNextSong,smoothScrollOffset,mBeatCounterRect,songTitleHeader,songTitleHeaderLocation)
    }

    private fun getBiggestLineSize(index: Int, modulus: Int): Rect {
        var maxHeight = 0
        var maxWidth = 0
        var lineCount = 0
        mLines.forEach{
            for (lh in it.mMeasurements.mGraphicHeights) {
                if (lineCount % modulus == index) {
                    maxHeight = Math.max(maxHeight, lh)
                    maxWidth = Math.max(maxWidth, it.mMeasurements.mLineWidth)
                }
                ++lineCount
            }
        }
        return Rect(0, 0, maxWidth - 1, maxHeight - 1)
    }

    private fun getMaximumGraphicsRequired(screenHeight: Int): Int {
        var maxLines = 0
        for(start in 0 until mLines.size)
        {
            var heightCounter = 0
            var lineCounter = 0
            for(f in start until mLines.size)
            {
                if(heightCounter<screenHeight)
                {
                    // Assume height of first line to be 1 pixel
                    // This is the state of affairs when the top line is almost
                    // scrolled offscreen, but not quite.
                    var lineHeight = 1
                    if (lineCounter > 0)
                        lineHeight = mLines[f].mMeasurements.mLineHeight
                    heightCounter += lineHeight
                    lineCounter += mLines[f].mMeasurements.mLines
                }
            }
            maxLines = Math.max(maxLines, lineCounter)
        }
        return maxLines
    }

    private fun setManualScrollPositions() {
        val usableScreenHeight=mNativeDeviceSettings.mScreenSize.height()-mBeatCounterRect.height()
        val maxScrollPosition=Math.max(mSongHeight-usableScreenHeight,0)

        for(lineWeAreSettingTheValuesFor in mLines)
        {
            var previousLine=lineWeAreSettingTheValuesFor.mPrevLine
            var nextLine=lineWeAreSettingTheValuesFor.mNextLine
            var previousLinePosition=previousLine?.mSongPixelPosition?:0
            var nextLinePosition=nextLine?.mSongPixelPosition?:maxScrollPosition
            val lineUp=previousLinePosition
            val lineDown=Math.min(nextLinePosition,maxScrollPosition)

            var pageUp=lineWeAreSettingTheValuesFor.mSongPixelPosition
            val pageUpLimit=Math.max(pageUp-usableScreenHeight,0)
            if(pageUpLimit>0) {
                while (previousLinePosition >= pageUpLimit) {
                    pageUp = previousLinePosition
                    previousLine=previousLine?.mPrevLine
                    previousLinePosition=previousLine?.mSongPixelPosition?:-1
                }
            }
            else
                pageUp=0

            var pageDown=lineWeAreSettingTheValuesFor.mSongPixelPosition
            val pageDownLimit=Math.min(pageDown+usableScreenHeight,maxScrollPosition)
            if(pageDownLimit<maxScrollPosition) {
                while (nextLinePosition <= pageDownLimit) {
                    pageDown = nextLinePosition
                    nextLine=nextLine?.mNextLine
                    nextLinePosition=nextLine?.mSongPixelPosition?:maxScrollPosition+1
                }
            }
            else
                pageDown=maxScrollPosition

            lineWeAreSettingTheValuesFor.mManualScrollPositions=ManualScrollPositions(lineUp,lineDown,pageUp,pageDown)
        }
    }

    private fun generateBeatEvents(startTime:Long,click:Boolean):Pair<Long,List<BeatEvent>>?
    {
        if(mCurrentLineBeatInfo.mScrollMode===LineScrollingMode.Smooth)
            return null
        var eventTime=startTime
        val beatEvents= mutableListOf<BeatEvent>()
        var beatThatWeWillScrollOn = 0
        val currentTimePerBeat=Utils.nanosecondsPerBeat(mCurrentLineBeatInfo.mBPM)
        val rolloverBeatCount = mRolloverBeats.size
        val beatsToAdjustCount = mBeatsToAdjust
        // We have N beats to adjust.
        // For the previous N beatevents, set the BPB to the new BPB.
        if(mBeatsToAdjust>0)
            mEvents.filterIsInstance<BeatEvent>().takeLast(mBeatsToAdjust).forEach{
                it.mBPB=mCurrentLineBeatInfo.mBPB
            }
        mBeatsToAdjust = 0

        var currentBarBeat = 0
        while (currentBarBeat < mCurrentLineBeatInfo.mBeats) {
            val beatsRemaining = mCurrentLineBeatInfo.mBeats - currentBarBeat
            beatThatWeWillScrollOn = if (beatsRemaining > mCurrentLineBeatInfo.mBPB)
                -1
            else
                (mCurrentBeat + (beatsRemaining - 1)) % mCurrentLineBeatInfo.mBPB
            val beatEvent: BeatEvent
            var rolloverBPB = 0
            var rolloverBeatLength: Long = 0
            if (mRolloverBeats.isEmpty())
                beatEvent = BeatEvent(eventTime, mCurrentLineBeatInfo, mCurrentBeat, click, beatThatWeWillScrollOn)
            else {
                beatEvent = mRolloverBeats.removeAt(0)
                beatEvent.mWillScrollOnBeat = beatThatWeWillScrollOn
                rolloverBPB = beatEvent.mBPB
                rolloverBeatLength = Utils.nanosecondsPerBeat(beatEvent.mBPM)
            }
            beatEvents.add(beatEvent)
            val beatTimeLength = if (rolloverBeatLength == 0L) currentTimePerBeat else rolloverBeatLength
            val nanoPerBeat = beatTimeLength / 4.0
            // generate MIDI beats.
            if (mLastBeatBlock == null || nanoPerBeat != mLastBeatBlock!!.nanoPerBeat) {
                mLastBeatBlock = BeatBlock(beatEvent.mEventTime, mMIDIBeatCounter++, nanoPerBeat)
                mBeatBlocks.add(mLastBeatBlock!!)
            }

            eventTime += beatTimeLength
            mCurrentBeat++
            if (mCurrentBeat == (if (rolloverBPB > 0) rolloverBPB else mCurrentLineBeatInfo.mBPB))
                mCurrentBeat = 0
            ++currentBarBeat
        }

        val beatsThisLine =mCurrentLineBeatInfo.mBeats-rolloverBeatCount+beatsToAdjustCount
        val simpleBeatsThisLine=mCurrentLineBeatInfo.mBPB * mCurrentLineBeatInfo.mBPL
        if (beatsThisLine > simpleBeatsThisLine) {
            // We need to store some information so that the next line can adjust the rollover beats.
            mBeatsToAdjust = mCurrentLineBeatInfo.mBeats - simpleBeatsThisLine
        } else if (beatsThisLine < simpleBeatsThisLine) {
            // We need to generate a few beats to store for the next line to use.
            mRolloverBeats.clear()
            var rolloverCurrentBeat = mCurrentBeat
            var rolloverCurrentTime = eventTime
            for (f in beatsThisLine until simpleBeatsThisLine) {
                mRolloverBeats.add(BeatEvent(rolloverCurrentTime, mCurrentLineBeatInfo, rolloverCurrentBeat++, click, beatThatWeWillScrollOn))
                rolloverCurrentTime += currentTimePerBeat
                if (rolloverCurrentBeat == mCurrentLineBeatInfo.mBPB)
                    rolloverCurrentBeat = 0
            }
        }
        return Pair(eventTime,beatEvents)
    }

    private fun generatePauseEvents(startTime:Long, pauseTimeNano: Long):List<PauseEvent> {
        // pauseTime is in milliseconds.
        // We don't want to generate thousands of events, so let's say every 1/10th of a second.
        var eventTime=startTime
        val pauseEvents= mutableListOf<PauseEvent>()
        val deciSeconds = Math.ceil(Utils.nanoToMilli(pauseTimeNano).toDouble() / 100.0).toInt()
        val remainder = pauseTimeNano - Utils.milliToNano(deciSeconds * 100)
        val oneDeciSecondInNanoseconds = Utils.milliToNano(100)
        eventTime += remainder
        for (f in 0 until deciSeconds) {
            val pauseEvent = PauseEvent(eventTime, deciSeconds, f)
            pauseEvents.add(pauseEvent)
            eventTime += oneDeciSecondInNanoseconds
        }
        return pauseEvents
    }

    private fun generateCountInEvents(countBars:Int, click:Boolean):Pair<Long,List<BeatEvent>> {
        val countInEvents= mutableListOf<BeatEvent>()
        var startTime=0L
        if (countBars > 0) {
            if (mCurrentLineBeatInfo.mBPM > 0.0) {
                val countbpm = mCurrentLineBeatInfo.mBPM
                val countbpb = mCurrentLineBeatInfo.mBPB
                val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                for (f in 0 until countBars)
                    for (g in 0 until countbpb) {
                        countInEvents.add(BeatEvent(startTime, mCurrentLineBeatInfo, g, click, if (f == countBars - 1) countbpb - 1 else -1))
                        startTime += nanoPerBeat
                    }
            }
        }
        return Pair(startTime,countInEvents)
    }

    private fun offsetMIDIEvents(firstEvent: BaseEvent) {
        var event:BaseEvent? = firstEvent
        while (event != null) {
            if (event is MIDIEvent) {
                val midiEvent = event
                if (midiEvent.mOffset.mAmount != 0) {
                    // OK, this event needs moved.
                    var newTime: Long = -1
                    if (midiEvent.mOffset.mOffsetType === EventOffsetType.Milliseconds) {
                        val offset = Utils.milliToNano(midiEvent.mOffset.mAmount)
                        newTime = midiEvent.mEventTime + offset
                    } else {
                        // Offset by beat count.
                        var beatCount = midiEvent.mOffset.mAmount
                        var currentEvent: BaseEvent = midiEvent
                        while (beatCount != 0) {
                            val beatEvent: BeatEvent = (if (beatCount > 0)
                                currentEvent.nextBeatEvent
                            else if (currentEvent is BeatEvent && currentEvent.mPrevEvent != null)
                                currentEvent.mPrevEvent!!.mPrevBeatEvent
                            else
                                currentEvent.mPrevBeatEvent) ?: break
                            if (beatEvent.mEventTime != midiEvent.mEventTime) {
                                beatCount -= beatCount / Math.abs(beatCount)
                                newTime = beatEvent.mEventTime
                            }
                            currentEvent = beatEvent
                        }
                    }
                    if (newTime < 0) {
                        mErrors.add(FileParseError(midiEvent.mOffset.mSourceTag, BeatPrompterApplication.getResourceString(R.string.midi_offset_is_before_start_of_song)))
                        newTime = 0
                    }
                    val newMIDIEvent = MIDIEvent(newTime, midiEvent.mMessages)
                    midiEvent.insertEvent(newMIDIEvent)
                    event = midiEvent.mPrevEvent
                    midiEvent.remove()
                }
            }
            event = event!!.mNextEvent
        }
    }

    /**
     * Based on the difference in screen size/resolution/orientation, we will alter the min/max font size of our native settings.
     */
    private fun translateSourceDeviceSettingsToNative(sourceSettings:SongDisplaySettings,nativeSettings:SongDisplaySettings):SongDisplaySettings
    {
        val sourceScreenSize=sourceSettings.mScreenSize
        val sourceRatio = sourceScreenSize.width().toDouble() / sourceScreenSize.height().toDouble()
        val screenWillRotate = nativeSettings.mOrientation != sourceSettings.mOrientation
        val nativeScreenSize = if(screenWillRotate)
            Rect(0,0,nativeSettings.mScreenSize.height(),nativeSettings.mScreenSize.width())
        else
            nativeSettings.mScreenSize
        val nativeRatio = nativeScreenSize.width().toDouble() / nativeScreenSize.height().toDouble()
        val minRatio = Math.min(nativeRatio, sourceRatio)
        val maxRatio = Math.max(nativeRatio, sourceRatio)
        val ratioMultiplier = minRatio / maxRatio
        var minimumFontSize = sourceSettings.mMinFontSize
        var maximumFontSize = sourceSettings.mMaxFontSize
        minimumFontSize *= ratioMultiplier.toFloat()
        maximumFontSize *= ratioMultiplier.toFloat()
        if (minimumFontSize > maximumFontSize) {
            mErrors.add(FileParseError(0, BeatPrompterApplication.getResourceString(R.string.fontSizesAllMessedUp)))
            maximumFontSize = minimumFontSize
        }
        return SongDisplaySettings(sourceSettings.mOrientation,minimumFontSize,maximumFontSize,nativeScreenSize)
    }

    private fun createStartScreenStrings():Pair<List<ScreenString>,ScreenString?>
    {
        // As for the start screen display (title/artist/comments/"press go"),
        // the title should take up no more than 20% of the height, the artist
        // no more than 10%, also 10% for the "press go" message.
        // The rest of the space is allocated for the comments and error messages,
        // each line no more than 10% of the screen height.
        val startScreenStrings= mutableListOf<ScreenString>()
        var availableScreenHeight = mNativeDeviceSettings.mScreenSize.height()
        var nextSongString:ScreenString?=null
        val boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (mSongLoadInfo.mNextSong.isNotBlank()) {
            // OK, we have a next song title to display.
            // This should take up no more than 15% of the screen.
            // But that includes a border, so use 13 percent for the text.
            val eightPercent = (mNativeDeviceSettings.mScreenSize.height() * 0.13).toInt()
            val nextSong=mSongLoadInfo.mNextSong
            val fullString = ">>> $nextSong >>>"
            nextSongString=ScreenString.create(fullString, mPaint, mNativeDeviceSettings.mScreenSize.width(), eightPercent, Color.BLACK, boldFont, true)
            availableScreenHeight -= (mNativeDeviceSettings.mScreenSize.height() * 0.15f).toInt()
        }
        val tenPercent = (availableScreenHeight / 10.0).toInt()
        val twentyPercent = (availableScreenHeight / 5.0).toInt()
        startScreenStrings.add(ScreenString.create(mSongLoadInfo.mSongFile.mTitle, mPaint, mNativeDeviceSettings.mScreenSize.width(), twentyPercent, Color.YELLOW, boldFont, true))
        if (mSongLoadInfo.mSongFile.mArtist.isNotBlank())
            startScreenStrings.add(ScreenString.create(mSongLoadInfo.mSongFile.mArtist, mPaint, mNativeDeviceSettings.mScreenSize.width(), tenPercent, Color.YELLOW, boldFont, true))
        val commentLines = mutableListOf<String>()
        for (c in mStartScreenComments)
            commentLines.add(c.mText)
        val nonBlankCommentLines = mutableListOf<String>()
        for (commentLine in commentLines)
            if (commentLine.trim().isNotEmpty())
                nonBlankCommentLines.add(commentLine.trim())
        val uniqueErrors= mErrors.distinct()
        var errorCount = uniqueErrors.size
        var messages = Math.min(errorCount, 6) + nonBlankCommentLines.size
        val showBPM = mShowBPM!=ShowBPM.No
        if (showBPM)
            ++messages
        if (mShowKey)
            ++messages
        if (messages > 0) {
            val remainingScreenSpace = mNativeDeviceSettings.mScreenSize.height() - twentyPercent * 2
            var spacePerMessageLine = Math.floor((remainingScreenSpace / messages).toDouble()).toInt()
            spacePerMessageLine = Math.min(spacePerMessageLine, tenPercent)
            var errorCounter = 0
            for (error in uniqueErrors) {
                startScreenStrings.add(ScreenString.create(error.toString(), mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.RED, mFont, false))
                ++errorCounter
                --errorCount
                if (errorCounter == 5 && errorCount > 0) {
                    startScreenStrings.add(ScreenString.create(String.format(BeatPrompterApplication.getResourceString(R.string.otherErrorCount), errorCount), mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.RED, mFont, false))
                    break
                }
            }
            for (nonBlankComment in nonBlankCommentLines)
                startScreenStrings.add(ScreenString.create(nonBlankComment, mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.WHITE, mFont, false))
            if (mShowKey) {
                val keyString = BeatPrompterApplication.getResourceString(R.string.keyPrefix) + ": " + mSongLoadInfo.mSongFile.mKey
                startScreenStrings.add(ScreenString.create(keyString, mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.CYAN, mFont, false))
            }
            if (mShowBPM!=ShowBPM.No) {
                val rounded = mShowBPM==ShowBPM.Rounded || mSongLoadInfo.mSongFile.mBPM == mSongLoadInfo.mSongFile.mBPM.toInt().toDouble()
                var bpmString = BeatPrompterApplication.getResourceString(R.string.bpmPrefix) + ": "
                bpmString += if (rounded)
                    Math.round(mSongLoadInfo.mSongFile.mBPM).toInt()
                else
                    mSongLoadInfo.mSongFile.mBPM
                startScreenStrings.add(ScreenString.create(bpmString, mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.CYAN, mFont, false))
            }
        }
        if (mSongLoadInfo.mSongLoadMode !== SongLoadMode.Manual)
            startScreenStrings.add(ScreenString.create(BeatPrompterApplication.getResourceString(R.string.tapTwiceToStart), mPaint, mNativeDeviceSettings.mScreenSize.width(), tenPercent, Color.GREEN, boldFont, true))
        return Pair(startScreenStrings,nextSongString)
    }

    override fun createSongTag(name:String,lineNumber:Int,position:Int,value:String): Tag
    {
        when(name)
        {
            "image"->return ImageTag(name, lineNumber, position, value)
            "pause"->return PauseTag(name, lineNumber, position, value)
            "send_midi_clock"->return SendMIDIClockTag(name, lineNumber, position)
            "comment", "c", "comment_box", "cb", "comment_italic", "ci"->return CommentTag(name, lineNumber, position, value)
            "count","countin"->return CountTag(name,lineNumber,position,value)

            "soh"->return StartOfHighlightTag(name, lineNumber, position, value, mDefaultHighlightColor)
            "eoh"->return EndOfHighlightTag(name, lineNumber, position)

            // BeatPrompter tags that are not required here ...
            "midi_song_select_trigger", "midi_program_change_trigger", "title", "t", "artist", "a", "subtitle", "st", "key", "tag", "time",
            // Unused ChordPro tags
            "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype", "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> return UnusedTag(name,lineNumber,position)

            else->{
                if(COMMENT_AUDIENCE_STARTERS.any{name.startsWith(it)})
                    return CommentTag(name,lineNumber,position,value)
                return MIDIEventTag(name, lineNumber, position, value, mSongTime, mDefaultMIDIOutputChannel)
            }
        }
    }

    private fun calculateStartAndStopScrollTimes(pauseTag:PauseTag?,lineStartTime:Long,lineDuration:Long,currentBeatEvents:Pair<Long,List<BeatEvent>>?):Pair<Long,Long>
    {
        // Calculate when this line should start scrolling
        val startScrollTime=
                when(mCurrentLineBeatInfo.mScrollMode)
                {
                    // Smooth mode? Start scrolling instantly.
                    LineScrollingMode.Smooth->mSongTime
                    else->
                        // Pause line? Start scrolling after 95% of the pause has elapsed.
                        if(pauseTag!=null)
                            lineStartTime+(pauseTag.mDuration*0.95).toLong()
                        // Beat line? Start scrolling on the last beat.
                        else
                            currentBeatEvents!!.second.lastOrNull()?.mEventTime?:mSongTime
                    // (Manual mode ignores these scroll values)
                }
        // Calculate when the line should stop scrolling
        val stopScrollTime=
                when(mCurrentLineBeatInfo.mScrollMode)
                {
                    // Smooth mode? It should stop scrolling once the allocated time has elapsed.
                    LineScrollingMode.Smooth->mSongTime+lineDuration
                    else->
                        // Pause line? It should stop scrolling when the pause has ran out
                        if(pauseTag!=null)
                            lineStartTime+pauseTag.mDuration
                        // Beat line? It should stop scrolling after the final beat
                        else
                            currentBeatEvents!!.first
                    // (Manual mode ignores these values)
                }

        return Pair(startScrollTime,stopScrollTime)
    }

    private fun calculateLineDuration(pauseTag:PauseTag?,addToPause:Long,lineStartTime:Long,currentBeatEvents:Pair<Long,List<BeatEvent>>?):Long
    {
        // Calculate how long this line will last for.
        return when {
            // Pause line? Lasts as long as the pause!
            pauseTag !=null -> pauseTag.mDuration+addToPause
            // Smooth line? We've counted the bars, so do the sums.
            mCurrentLineBeatInfo.mScrollMode == LineScrollingMode.Smooth && mSmoothScrollTimings!=null -> mSmoothScrollTimings.mTimePerBar * mCurrentLineBeatInfo.mBPL
            // Beat line? The result of generateBeatEvents will contain the time
            // that the beats end, so subtract the start time from that to get our duration.
            else -> currentBeatEvents!!.first-lineStartTime
            // (Manual mode ignores these scroll values)
        }
    }

    companion object {
        private const val DEMO_LINE_COUNT = 15
        // Every beatstart/beatstop block has events that are offset by this amount (one year).
        // If you left the app running for a year, it would eventually progress. WHO WOULD DO SUCH A THING?
        private val BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS = Utils.milliToNano(1000 * 60 * 24 * 365)
        private val COMMENT_AUDIENCE_STARTERS=listOf("comment@", "c@", "comment_box@", "cb@", "comment_italic@", "ci@")
    }
}