package com.stevenfrew.beatprompter.cache.parse

import android.graphics.*
import android.os.Handler
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.songload.SongLoadInfo

class SongParser constructor(private val mSongLoadInfo: SongLoadInfo, private val mCancelEvent: CancelEvent, private val mSongLoadHandler: Handler, private val mRegistered:Boolean):SongFileParser<Song>(mSongLoadInfo.mSongFile) {
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
    private val mEvents=mutableListOf<BaseEvent>(StartEvent())
    private val mLines=mutableListOf<Pair<Line,BeatInfo>>()
    private val mRolloverBeats=mutableListOf<BeatEvent>()
    private val mBeatBlocks = mutableListOf<BeatBlock>()
    private val mPaint= Paint()
    private val mFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val mBeatCounterRect:Rect
    private val mBeatCounterHeight:Int
    private val mDefaultHighlightColor:Int

    private var mSongHeight=0
    private var mMIDIBeatCounter:Int=0
    private var mLastBeatBlock: BeatBlock? = null
    private var mBeatsToAdjust:Int=0
    private var mCurrentBeat:Int=0
    private var mDisplayLineCounter:Int=0
    private var mCurrentScrollMode=mSongLoadInfo.mScrollMode
    private var mBeatInfo:BeatInfo=BeatInfo()
    private var mCountIn:Int
    private var mSendMidiClock:Boolean=false
    private var mCurrentHighlightColor:Int?=null
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
        mBeatCounterHeight =
                // Top 5% of screen is used for beat counter
                if (mSongLoadInfo.mScrollMode !== ScrollingMode.Manual)
                    (mNativeDeviceSettings.mScreenSize.height() / 20.0).toInt()
                else
                    0
        mBeatCounterRect = Rect(0, 0, mNativeDeviceSettings.mScreenSize.width(), mBeatCounterHeight)

        // Start the progress message dialog
        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mSongLoadInfo.mSongFile.mLines).sendToTarget()
    }

    override fun parseLine(line: TextFileLine<Song>) {
        // TODO: Deep clone of tags
        val chordTags=line.mTags.filterIsInstance<ChordTag>()
        val nonChordTags=line.mTags.filter { it !is ChordTag }
        val chordsFound = mShowChords && !chordTags.isEmpty()
        val chordsFoundButNotShowingThem=!mShowChords && chordsFound
        val tags=if(mShowChords)line.mTags.toList() else nonChordTags

        // Bars can be defined by commas ....
        var commaBars:Int?=null
        var workLine=line.mTaglessLine
        while (workLine.startsWith(",")) {
            if(commaBars==null)
                commaBars=0
            workLine = workLine.substring(1)
            tags.forEach{it.retreatFrom(0)}
            commaBars++
        }

        var scrollBeatOffset=0
        while (workLine.endsWith(">") || workLine.endsWith("<")) {
            if (workLine.endsWith(">"))
                scrollBeatOffset++
            else if (workLine.endsWith("<"))
                scrollBeatOffset--
            workLine = workLine.substring(0, workLine.length - 1)
            tags.forEach{it.retreatFrom(workLine.length)}
        }

        // TODO: dynamic BPB changing

        // ... or by a tag (which overrides commas)
        val barsTag=tags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=tags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=tags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=tags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=tags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val beatStartTags=tags.filterIsInstance<BeatStartTag>().toMutableList()
        val beatStopTags=tags.filterIsInstance<BeatStopTag>().toMutableList()
        val beatModeTags=listOf(beatStartTags,beatStopTags).flatMap { it }.toMutableList()

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?:commaBars?:mBeatInfo.mBPL
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:mBeatInfo.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:mBeatInfo.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:mBeatInfo.mScrollBeat

        val previousBeatsPerBar=mBeatInfo.mBPB
        val previousScrollBeat=mBeatInfo.mScrollBeat
        // If the beats-per-bar have changed, and there is no indication of what the new scrollbeat should be,
        // set the new scrollbeat to have the same "difference" as before. For example, if the old BPB was 4,
        // and the scrollbeat was 3 (one less than BPB), a new BPB of 6 should have a scrollbeat of 5 (one
        // less than BPB)
        if((beatsPerBarInThisLine!=previousBeatsPerBar)&&(scrollBeatTag==null))
        {
            val prevScrollBeatDiff = previousBeatsPerBar - previousScrollBeat
            if(beatsPerBarInThisLine-prevScrollBeatDiff>0)
                scrollBeatInThisLine=beatsPerBarInThisLine-prevScrollBeatDiff
        }
        if(scrollBeatInThisLine>beatsPerBarInThisLine)
            scrollBeatInThisLine=beatsPerBarInThisLine

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        if(beatModeTags.size==1)
            if(beatStartTags.isNotEmpty())
                if(beatsPerMinuteInThisLine==0.0)
                    mErrors.add(FileParseError(beatStartTags.first(), BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                else
                    mCurrentScrollMode= ScrollingMode.Beat
            else
                mCurrentScrollMode = ScrollingMode.Manual


        if ((beatsPerBarInThisLine!=0)&&(scrollBeatOffset < -beatsPerBarInThisLine || scrollBeatOffset >= beatsPerBarInThisLine)) {
            mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
            scrollBeatOffset = 0
        }

        mBeatInfo= BeatInfo(barsPerLineTag?.mBPL?:mBeatInfo.mBPL,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,mCurrentScrollMode)
        val currentLineBeatInfo= BeatInfo(barsInThisLine,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,mCurrentScrollMode)

        val metronomeOn = mMetronomeContext === MetronomeContext.On || (mMetronomeContext === MetronomeContext.OnWhenNoTrack && mSongLoadInfo.mTrack!=null)

        var imageTag=tags.filterIsInstance<ImageTag>().firstOrNull()
        val audioTags=tags.filterIsInstance<AudioTag>()
        val audioTag=
                if(mSongLoadInfo.mTrack==null)
                    audioTags.firstOrNull()
                else {
                    val matchedAudioFile= audioTags.firstOrNull { it.mFilename == mSongLoadInfo.mTrack!!.mName }
                    if(matchedAudioFile==null)
                        mErrors.add(FileParseError(line.mLineNumber,BeatPrompterApplication.getResourceString(R.string.missing_audio_file_warning)))
                    matchedAudioFile
                }

        if(!mSendMidiClock)
            mSendMidiClock=tags.any{it is SendMIDIClockTag}

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

        val pauseTag=tags.filterIsInstance<PauseTag>().firstOrNull()
        if(pauseTag!=null)
        {
            // TODO: generate pause here
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

        val createLine= (workLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || imageTag != null)

        // Contains only tags? Or contains nothing? Don't use it as a blank line.
        if (createLine || pauseTag!=null) {
            // We definitely have a line!
            // So now is when we want to create the count-in (if any)
            generateCountInEvents(mCountIn,currentLineBeatInfo,mMetronomeContext === MetronomeContext.DuringCountIn || metronomeOn)

            if(audioTag!=null)
                mEvents.add(AudioEvent(mSongTime,audioTag.mFilename,audioTag.mVolume))

            // Any comments or MIDI events from here will be part of the song,
            // rather than startup events.
            mStopAddingStartupItems = true

            if (imageTag != null && (workLine.isNotBlank() || chordsFound))
                mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.text_found_with_image)))
            if (workLine.isBlank() && (!chordsFound || chordsFoundButNotShowingThem))
                workLine = "▼"

            if (createLine)
            {
                // Won't pay? We'll take it away!
                if (mDisplayLineCounter > DEMO_LINE_COUNT && !mRegistered) {
                    workLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                    imageTag = null
                }

                val lastBeatInfo= if(mLines.isEmpty())currentLineBeatInfo else mLines.last().second
                val lastScrollBeatOffset = lastBeatInfo.mScrollBeatOffset
                val lastBPB = lastBeatInfo.mBPB
                val lastScrollBeat = lastBeatInfo.mScrollBeat
                val scrollBeatDifference =currentLineBeatInfo.mScrollBeat - currentLineBeatInfo.mBPB - (lastScrollBeat - lastBPB)

                var beatsForThisLine = currentLineBeatInfo.mBPB * currentLineBeatInfo.mBPL
                beatsForThisLine += currentLineBeatInfo.mScrollBeatOffset
                beatsForThisLine += scrollBeatDifference
                beatsForThisLine -= lastScrollBeatOffset

                val nanosecondsPerBeat = if (currentLineBeatInfo.mBPM > 0.0)
                    Utils.nanosecondsPerBeat(currentLineBeatInfo.mBPM)
                else
                    0

                // TODO: CALCULATE TIME PER BAR FOR SMOOTH MODE
                val timePerBar=0L
                val totalLineTime = when {
                    pauseTag !=null -> Utils.milliToNano(pauseTag.mDuration)
                    currentLineBeatInfo.mScrollingMode == ScrollingMode.Smooth -> timePerBar * currentLineBeatInfo.mBPL
                    else -> beatsForThisLine * nanosecondsPerBeat
                }

                var lineObj: Line?=null
                if (imageTag != null) {
                    val imageFile = SongList.mCachedCloudFiles.getMappedImageFile(imageTag.mFilename)
                    if (imageFile != null)
                        try {
                            lineObj = ImageLine(imageFile, imageTag.mImageScalingMode,mSongTime,totalLineTime,mCurrentScrollMode,mNativeDeviceSettings,mSongHeight)
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
                if(imageTag==null)
                    lineObj = TextLine(workLine, tags,mSongTime,totalLineTime,mCurrentScrollMode,mNativeDeviceSettings,mCurrentHighlightColor,mSongHeight,mCancelEvent)

                if(lineObj!=null)
                {
                    val previousLine=mLines.lastOrNull()?.first
                    if(previousLine!=null)
                    {
                        previousLine.mNextLine=lineObj
                        lineObj.mPrevLine=previousLine
                    }

                    mSongHeight+=lineObj.mMeasurements.mLineHeight
                    mLines.add(Pair(lineObj,currentLineBeatInfo))
                    val lineEvent=LineEvent(mSongTime,lineObj)
                    mEvents.add(lineEvent)

                    // Generate beat events ...

                    // if a pause is specified on a line, it replaces the actual beats for that line.
                    if (pauseTag!=null) {
                        generatePauseEvents(pauseTag.mDuration)
                        // Line still has to scroll somewhere!
                        lineObj.mYStartScrollTime = mSongTime - nanosecondsPerBeat
                        lineObj.mYStopScrollTime = mSongTime
                    } else if (currentLineBeatInfo.mBPM > 0.0 && currentLineBeatInfo.mScrollingMode == ScrollingMode.Beat) {
                        generateBeatEvents(currentLineBeatInfo,nanosecondsPerBeat,beatsForThisLine,metronomeOn,lineObj)
                    } else {
                        lineEvent.mLine.mYStartScrollTime = mSongTime
                        mSongTime += totalLineTime
                        lineEvent.mLine.mYStopScrollTime = mSongTime
                    }
                }
            } else if (pauseTag!=null)
                generatePauseEvents(pauseTag.mDuration)
        }

        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, line.mLineNumber, mSongLoadInfo.mSongFile.mLines).sendToTarget()
    }

    override fun getResult(): Song {
        var smoothScrollOffset=0
        if (mSongLoadInfo.mScrollMode === ScrollingMode.Smooth)
            smoothScrollOffset = Math.min(mLines.map{it.first.mMeasurements.mLineHeight}.maxBy{it}?:0, (mNativeDeviceSettings.mScreenSize.height() / 3.0).toInt())
        else if (mSongLoadInfo.mScrollMode === ScrollingMode.Beat)
            mSongHeight -= mLines.reversed().map{it.first.mMeasurements.mLineHeight}.firstOrNull{it!=0}?:0

        val startScreenStrings=createStartScreenStrings()
        val totalStartScreenTextHeight = startScreenStrings.first.sumBy { it.mHeight }

        // Allocate graphics objects.
        val maxGraphicsRequired = getMaximumGraphicsRequired(mNativeDeviceSettings.mScreenSize.height())
        val lineGraphics = CircularGraphicsList()
        for (f in 0 until maxGraphicsRequired)
            lineGraphics.add(LineGraphic(getBiggestLineSize(f, maxGraphicsRequired)))

        var graphic: LineGraphic = lineGraphics.first()
        mLines.forEach {
            for (f in 0 until it.first.mMeasurements.mLines) {
                it.first.allocateGraphic(graphic)
                graphic = graphic.mNextGraphic
            }
        }

        // In smooth scrolling mode, the last screenful of text should never leave the screen.
        if (mSongLoadInfo.mScrollMode === ScrollingMode.Smooth) {
            var total = mNativeDeviceSettings.mScreenSize.height() - smoothScrollOffset - mBeatCounterHeight
            var nextLineEvent:LineEvent?=null
            for(endLineEvent in mEvents.filterIsInstance<LineEvent>().reversed()) {
                total -= endLineEvent.mLine.mMeasurements.mLineHeight
                if (total <= 0) {
                    if (nextLineEvent != null)
                        nextLineEvent.mLine.mYStopScrollTime = Long.MAX_VALUE
                    break
                }
                mEvents.remove(endLineEvent)
                nextLineEvent=endLineEvent
            }
        }

        val maxSongTitleWidth = mNativeDeviceSettings.mScreenSize.width() * 0.9f
        val maxSongTitleHeight = mBeatCounterHeight * 0.9f
        val vMargin = (mBeatCounterHeight - maxSongTitleHeight) / 2.0f
        val songTitleHeader = ScreenString.create(mSongLoadInfo.mSongFile.mTitle, mPaint, maxSongTitleWidth.toInt(), maxSongTitleHeight.toInt(), Utils.makeHighlightColour(Color.BLACK, 0x80.toByte()), mFont, false)
        val extraMargin = (maxSongTitleHeight - songTitleHeader.mHeight) / 2.0f
        val x = ((mNativeDeviceSettings.mScreenSize.width() - songTitleHeader.mWidth) / 2.0).toFloat()
        val y = mBeatCounterHeight - (extraMargin + songTitleHeader.mDescenderOffset.toFloat() + vMargin)
        val songTitleHeaderLocation = PointF(x, y)

        val firstEvent=buildEventList()
        offsetMIDIEvents(firstEvent)
        return Song(mSongLoadInfo.mSongFile,mSongLoadInfo.mScrollMode,mNativeDeviceSettings,mSongHeight,firstEvent,mLines.map{it.first},mInitialMIDIMessages,mBeatBlocks,mSendMidiClock,startScreenStrings.first,startScreenStrings.second,totalStartScreenTextHeight,mSongLoadInfo.mStartedByBandLeader,mSongLoadInfo.mNextSong,smoothScrollOffset,mBeatCounterRect,songTitleHeader,songTitleHeaderLocation)
    }

    private fun buildEventList():BaseEvent
    {
        val firstEvent=mEvents.removeAt(0)
        mEvents.forEach {
            firstEvent.add(it)
        }
        return firstEvent
    }

    private fun getBiggestLineSize(index: Int, modulus: Int): Rect {
        var maxHeight = 0
        var maxWidth = 0
        var lineCount = 0
        mLines.forEach{
            for (lh in it.first.mMeasurements.mGraphicHeights) {
                if (lineCount % modulus == index) {
                    maxHeight = Math.max(maxHeight, lh)
                    maxWidth = Math.max(maxWidth, it.first.mMeasurements.mLineWidth)
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
                        lineHeight = mLines[f].first.mMeasurements.mLineHeight
                    heightCounter += lineHeight
                    lineCounter += mLines[f].first.mMeasurements.mLines
                }
            }
            maxLines = Math.max(maxLines, lineCounter)
        }
        return maxLines
    }

    override fun createSongTag(name:String,lineNumber:Int,position:Int,value:String): Tag
    {
        when(name)
        {
            "bpm", "metronome", "beatsperminute"->return BeatsPerMinuteTag(name, lineNumber, position, value)
            "b","bars"->return BarsTag(name, lineNumber, position, value)
            "bpb", "beatsperbar"->return BeatsPerBarTag(name, lineNumber, position, value)
            "bpl", "barsperline"->return BarsPerLineTag(name, lineNumber, position, value)
            "scrollbeat", "sb"->return ScrollBeatTag(name, lineNumber, position, value)
            "beatstart"->return BeatStartTag(name, lineNumber, position)
            "beatstop"->return BeatStopTag(name, lineNumber, position)

            "image"->return ImageTag(name, lineNumber, position, value)
            "pause"->return PauseTag(name, lineNumber, position, value)
            "send_midi_clock"->return SendMIDIClockTag(name, lineNumber, position)
            "comment", "c", "comment_box", "cb", "comment_italic", "ci"->return CommentTag(name, lineNumber, position, value)
            "count","countin"->return CountTag(name,lineNumber,position,value)

            "soh"->return StartOfHighlightTag(name, lineNumber, position, value, mCurrentHighlightColor?:mDefaultHighlightColor)
            "eoh"->return EndOfHighlightTag(name, lineNumber, position)

            // BeatPrompter tags that are not required here ...
            "midi_song_select_trigger", "midi_program_change_trigger", "title", "t", "artist", "a", "subtitle", "st", "key", "tag", "track", "audio", "musicpath", "time",
            // Unused ChordPro tags
            "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype", "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> return UnusedTag(name,lineNumber,position)

            else->return MIDIEventTag(name, lineNumber, position, value, mSongTime, mDefaultMIDIOutputChannel)
        }
    }

    private fun generateBeatEvents(currentLineBeatInfo:BeatInfo,nanosecondsPerBeat:Long,beatsForThisLine:Int,click:Boolean,lineObj:Line)
    {
        if (currentLineBeatInfo.mScrollingMode == ScrollingMode.Smooth)
            lineObj.mYStartScrollTime = mSongTime
        var finished = false
        var beatThatWeWillScrollOn = 0
        val rolloverBeatCount = mRolloverBeats.size
        val beatsToAdjustCount = mBeatsToAdjust
        // We have N beats to adjust.
        // For the previous N beatevents, set the BPB to the new BPB.
        mEvents.filterIsInstance<BeatEvent>().takeLast(mBeatsToAdjust).forEach{
            it.mBPB=currentLineBeatInfo.mBPB
        }
        mBeatsToAdjust = 0

        var currentBarBeat = 0
        while (!finished && currentBarBeat < beatsForThisLine) {
            val beatsRemaining = beatsForThisLine - currentBarBeat
            beatThatWeWillScrollOn = if (beatsRemaining > currentLineBeatInfo.mBPB)
                -1
            else
                (mCurrentBeat + (beatsRemaining - 1)) % currentLineBeatInfo.mBPB
            val beatEvent: BeatEvent
            var rolloverBPB = 0
            var rolloverBeatLength: Long = 0
            if (mRolloverBeats.isEmpty())
                beatEvent = BeatEvent(mSongTime, currentLineBeatInfo, mCurrentBeat, click, beatThatWeWillScrollOn)
            else {
                beatEvent = mRolloverBeats.removeAt(0)
                beatEvent.mWillScrollOnBeat = beatThatWeWillScrollOn
                rolloverBPB = beatEvent.mBPB
                rolloverBeatLength = Utils.nanosecondsPerBeat(beatEvent.mBPM)
            }
            mEvents.add(beatEvent)
            val beatTimeLength = if (rolloverBeatLength == 0L) nanosecondsPerBeat else rolloverBeatLength
            val nanoPerBeat = beatTimeLength / 4.0
            // generate MIDI beats.
            if (mLastBeatBlock == null || nanoPerBeat != mLastBeatBlock!!.nanoPerBeat) {
                mLastBeatBlock = BeatBlock(beatEvent.mEventTime, mMIDIBeatCounter++, nanoPerBeat)
                mBeatBlocks.add(mLastBeatBlock!!)
            }

            if (currentBarBeat == beatsForThisLine - 1) {
                if (currentLineBeatInfo.mScrollingMode == ScrollingMode.Beat) {
                    lineObj.mYStartScrollTime = mSongTime
                    lineObj.mYStopScrollTime = mSongTime + nanosecondsPerBeat
                }
                finished = true
            }
            mSongTime += beatTimeLength
            mCurrentBeat++
            if (mCurrentBeat == (if (rolloverBPB > 0) rolloverBPB else currentLineBeatInfo.mBPB))
                mCurrentBeat = 0
            ++currentBarBeat
        }

        val beatsThisLine =beatsForThisLine-rolloverBeatCount+beatsToAdjustCount
        val simpleBeatsThisLine=currentLineBeatInfo.mBPB * currentLineBeatInfo.mBPL
        if (beatsThisLine > simpleBeatsThisLine) {
            // We need to store some information so that the next line can adjust the rollover beats.
            mBeatsToAdjust = beatsForThisLine - simpleBeatsThisLine
        } else if (beatsThisLine < simpleBeatsThisLine) {
            // We need to generate a few beats to store for the next line to use.
            mRolloverBeats.clear()
            var rolloverCurrentBeat = mCurrentBeat
            var rolloverCurrentTime = mSongTime
            for (f in beatsThisLine until simpleBeatsThisLine) {
                mRolloverBeats.add(BeatEvent(rolloverCurrentTime, currentLineBeatInfo, rolloverCurrentBeat++, click, beatThatWeWillScrollOn))
                rolloverCurrentTime += nanosecondsPerBeat
                if (rolloverCurrentBeat == currentLineBeatInfo.mBPB)
                    rolloverCurrentBeat = 0
            }
        }
        if (currentLineBeatInfo.mScrollingMode == ScrollingMode.Smooth)
            lineObj.mYStopScrollTime = mSongTime + nanosecondsPerBeat
    }

    private fun generatePauseEvents(pauseTime: Long) {
        // pauseTime is in milliseconds.
        // We don't want to generate thousands of events, so let's say every 1/10th of a second.
        val deciSeconds = Math.ceil(pauseTime.toDouble() / 100.0).toInt()
        val remainder = Utils.milliToNano(pauseTime) - Utils.milliToNano(deciSeconds * 100)
        val oneDeciSecondInNanoseconds = Utils.milliToNano(100)
        mSongTime += remainder
        for (f in 0 until deciSeconds) {
            val pauseEvent = PauseEvent(mSongTime, deciSeconds, f)
            mEvents.add(pauseEvent)
            mSongTime += oneDeciSecondInNanoseconds
        }
    }

    private fun generateCountInEvents(countBars:Int,currentLineBeatInfo: BeatInfo,click:Boolean) {
        if (countBars > 0) {
            if (currentLineBeatInfo.mBPM > 0.0) {
                val countbpm = currentLineBeatInfo.mBPM
                val countbpb = currentLineBeatInfo.mBPB
                val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                for (f in 0 until countBars)
                    for (g in 0 until countbpb) {
                        mEvents.add(BeatEvent(mSongTime, currentLineBeatInfo, g, click, if (f == countBars - 1) countbpb - 1 else -1))
                        mSongTime += nanoPerBeat
                    }
            }
        }
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
        var errorCount = mErrors.size
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
            for (error in mErrors) {
                startScreenStrings.add(ScreenString.create(error.errorMessage, mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.RED, mFont, false))
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
        if (mSongLoadInfo.mScrollMode !== ScrollingMode.Manual)
            startScreenStrings.add(ScreenString.create(BeatPrompterApplication.getResourceString(R.string.tapTwiceToStart), mPaint, mNativeDeviceSettings.mScreenSize.width(), tenPercent, Color.GREEN, boldFont, true))
        return Pair(startScreenStrings,nextSongString)
    }

    companion object {
        private const val DEMO_LINE_COUNT = 15
        // Every beatstart/beatstop block has events that are offset by this amount (one year).
        // If you left the app running for a year, it would eventually progress. WHO WOULD DO SUCH A THING?
        private val BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS = Utils.milliToNano(1000 * 60 * 24 * 365)
    }
}