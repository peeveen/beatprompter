package com.stevenfrew.beatprompter

import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.util.Log
import com.stevenfrew.beatprompter.cache.*
import com.stevenfrew.beatprompter.cache.parse.FileLine
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.SongParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.songload.SongLoadInfo
import java.io.*
import java.util.HashSet

/**
 * Takes a SongFile and parses it into a Song.
 */
class SongParser(private val mLoadingSongFile: SongLoadInfo, private val mCancelEvent: CancelEvent, private val mSongLoadHandler: Handler, private val mRegistered: Boolean) {
    private val mSongFile: SongFile = mLoadingSongFile.songFile
    private val mCountInMin: Int
    private val mCountInMax: Int
    private val mCountInDefault: Int
    private val mBPMMin: Int
    private val mBPMMax: Int
    private val mBPMDefault: Int
    private val mBPLMin: Int
    private val mBPLMax: Int
    private val mBPLDefault: Int
    private val mBPBMin: Int
    private val mBPBMax: Int
    private val mBPBDefault: Int
    private val mUserChosenScrollMode: ScrollingMode = mLoadingSongFile.scrollMode
    private var mCurrentScrollMode: ScrollingMode? = null
    private val mTriggerContext: TriggerOutputContext
    private var mCountInPref: Int = 0
    private var mDefaultTrackVolume: Int = 0
    private val mDefaultMIDIOutputChannel: Byte
    private val mShowChords: Boolean
    private var mSendMidiClock: Boolean = false
    private var mBackgroundColour: Int = 0
    private var mPulseColour: Int = 0
    private var mBeatCounterColour: Int = 0
    private val mScrollMarkerColour: Int
    private var mLyricColour: Int = 0
    private var mChordColour: Int = 0
    private var mAnnotationColour: Int = 0
    private val mCustomCommentsUser: String?
    private val mIgnoreColorInfo: Boolean
    private var mMetronomeContext: MetronomeContext? = null

    init {

        val countInOffset = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_offset))
        mCountInMin = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_min)) + countInOffset
        mCountInMax = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_max)) + countInOffset
        mCountInDefault = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)) + countInOffset

        val bpmOffset = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_offset))
        mBPMMin = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_min)) + bpmOffset
        mBPMMax = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_max)) + bpmOffset
        mBPMDefault = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_default)) + bpmOffset

        val bplOffset = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_offset))
        mBPLMin = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_min)) + bplOffset
        mBPLMax = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_max)) + bplOffset
        mBPLDefault = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_default)) + bplOffset

        val bpbOffset = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_offset))
        mBPBMin = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_min)) + bpbOffset
        mBPBMax = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_max)) + bpbOffset
        mBPBDefault = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_default)) + bpbOffset

        // OK, the "scrollMode" param is passed in here.
        // This might be what the user has explicitly chosen, i.e.
        // smooth mode or manual mode, chosen via the long-press play dialog.
        mCurrentScrollMode = mUserChosenScrollMode
        // BUT, if the mode that has come in is "beat mode", and this is a mixed mode
        // song, we should be switching when we encounter beatstart/beatstop tags.
        if (mSongFile.mMixedMode && mCurrentScrollMode === ScrollingMode.Beat)
        // And if we ARE in mixed mode with switching allowed, we start in manual.
            mCurrentScrollMode = ScrollingMode.Manual

        val sharedPref = BeatPrompterApplication.preferences
        mTriggerContext = TriggerOutputContext.valueOf(sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        mCountInPref = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        mCountInPref += Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_offset))
        /*            int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
            defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        mDefaultTrackVolume = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_default)))
        mDefaultTrackVolume += Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_offset))
        val defaultMIDIOutputChannelPrefValue = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
        mShowChords = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        mSendMidiClock = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        mBackgroundColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
        mPulseColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_default)))
        mBeatCounterColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)))
        mScrollMarkerColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_default)))
        mLyricColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_default)))
        mChordColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_default)))
        mAnnotationColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_default)))
        mCustomCommentsUser = sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))
        mBackgroundColour = mBackgroundColour or -0x1000000
        mAnnotationColour = mAnnotationColour or -0x1000000
        mPulseColour = mPulseColour or -0x1000000
        mBeatCounterColour = mBeatCounterColour or -0x1000000
        mLyricColour = mLyricColour or -0x1000000
        mChordColour = mChordColour or -0x1000000
        mIgnoreColorInfo = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue).toBoolean())
        mMetronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPref)
    }

    @Throws(IOException::class)
    fun parse(): Song {
        val parsingState= SongParsingState()
        val initialMIDIMessages = mutableListOf<OutgoingMessage>()
        var stopAddingStartupItems = false

        val chosenTrack = mLoadingSongFile.track
        val sst = mSongFile.getTimePerLineAndBar(chosenTrack)
        val timePerLine = sst.timePerLine
        val timePerBar = sst.timePerBar

        if (timePerLine < 0 || timePerBar < 0) {
            parsingState.mErrors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.pauseLongerThanSong)))
            sst.timePerLine = -timePerLine
            sst.timePerBar = -timePerBar
        }

        val br = BufferedReader(InputStreamReader(FileInputStream(mSongFile.mFile)))
        try {
            var line: String?=""

            val tagsSet = HashSet<String>()

            // ONE SHOT
            var chosenAudioFile: AudioFile? = null
            var chosenAudioVolume = 100
            var count = mCountInPref
            var trackOffset: Long = 0
            // TIME
            var beatsToAdjust = 0
            val rolloverBeats = mutableListOf<BeatEvent>()
            var pauseTime: Long
            var lastBeatBlock: BeatBlock? = null
            // COMMENT
            val comments = mutableListOf<Comment>()
            val beatBlocks = mutableListOf<BeatBlock>()
            var lineImage: ImageFile? = null

            var metronomeOn = mMetronomeContext === MetronomeContext.On
            if (mMetronomeContext === MetronomeContext.OnWhenNoTrack && chosenTrack!=null)
                metronomeOn = true

            var currentBeat = 0
            var nanosecondsPerBeat: Long

            var imageScalingMode = ImageScalingMode.Stretch
            var currentTime: Long = 0
            var midiBeatCounter = 0
            var firstEvent: BaseEvent? = null
            var lastEvent: BaseEvent? = null
            var firstLine: Line? = null

            // There must ALWAYS be a style and time event at the start, before any line events.
            // Create them from defaults even if there are no relevant tags in the file.
            var createColorEvent = true
            var lineCounter = 0
            var displayLineCounter = 0
            mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mSongFile.mLines).sendToTarget()
            while (line!=null && !mCancelEvent.isCancelled) {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= FileLine(line, ++lineCounter, mSongFile.mFile,parsingState)
                    pauseTime = 0
                    // Ignore comments.
                    if(fileLine.isComment)
                        continue

                    fileLine.mTags.filterNot{it is ChordTag }.forEach {
                        if (it.isColorTag && !mIgnoreColorInfo)
                            createColorEvent = true
                        if (it.isOneShotTag && tagsSet.contains(it.mName))
                            parsingState.mErrors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.oneShotTagDefinedTwice, it.mName)))

                        if(it is ImageTag)
                        {
                            if (lineImage != null)
                                parsingState.mErrors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                            else {
                                lineImage = it.mImageFile
                                imageScalingMode=it.mImageScalingMode
                            }
                        }
                        else if(it is TrackTag)
                        {
                            chosenAudioFile = it.mAudioFile
                            chosenAudioVolume = it.mVolume
                        }
                        else if(it is SendMIDIClockTag)
                            mSendMidiClock=true
                        else if(it is CountTag)
                            count=it.mCount
                        else if(it is BackgroundColorTag)
                            mBackgroundColour=it.mColor
                        else if(it is PulseColorTag)
                            mPulseColour=it.mColor
                        else if(it is LyricsColorTag)
                            mLyricColour=it.mColor
                        else if(it is ChordsColorTag)
                            mChordColour=it.mColor
                        else if(it is BeatCounterColorTag)
                            mBeatCounterColour=it.mColor
                        else if(it is CommentTag)
                        {
                            val comment = Comment(it.mComment,it.mAudience)
                            if (stopAddingStartupItems) {
                                val ce = CommentEvent(currentTime, comment)
                                if (firstEvent == null)
                                    firstEvent = ce
                                else
                                    lastEvent!!.add(ce)
                                lastEvent = ce
                            } else
                                if (comment.isIntendedFor(mCustomCommentsUser))
                                    comments.add(comment)
                        }
                        else if(it is PauseTag)
                            pauseTime=it.mDuration
                        else if(it is MIDIEventTag)
                        {
                            if (displayLineCounter < DEMO_LINE_COUNT || mRegistered)
                            {
                                val midiEvent=it.mEvent
                                if (stopAddingStartupItems) {
                                    if (firstEvent == null)
                                        firstEvent = midiEvent
                                    else
                                        lastEvent!!.add(midiEvent)
                                    lastEvent = midiEvent
                                } else {
                                    initialMIDIMessages.addAll(midiEvent.mMessages)
                                    if (midiEvent.mOffset != EventOffset.NoOffset)
                                        parsingState.mErrors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.midi_offset_before_first_line)))
                                }
                            }
                        }
                        tagsSet.add(it.mName)
                    }
                    val chordTags=fileLine.chordTags
                    val nonChordTags=fileLine.nonChordTags
                    val chordsFound = mShowChords && !chordTags.isEmpty()
                    val chordsFoundButNotShowingThem=!mShowChords && chordsFound
                    val tagsToProcess=if (chordsFoundButNotShowingThem) nonChordTags else fileLine.mTags
                    val createLine= (fileLine.mTaglessLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || lineImage != null)

                    // Contains only tags? Or contains nothing? Don't use it as a blank line.
                    if (createLine || pauseTime > 0) {
                        // We definitely have a line event!
                        // Deal with style/time/comment events now.
                        if (createColorEvent) {
                            val styleEvent = ColorEvent(currentTime, mBackgroundColour, mPulseColour, mLyricColour, mChordColour, mAnnotationColour, mBeatCounterColour, mScrollMarkerColour)
                            if (currentTime == 0L && firstEvent != null) {
                                // First event should ALWAYS be a color event.
                                val oldFirstEvent = firstEvent!!
                                styleEvent.add(oldFirstEvent)
                                firstEvent = styleEvent
                            } else {
                                if (firstEvent == null)
                                    firstEvent = styleEvent
                                else
                                    lastEvent!!.add(styleEvent)
                                lastEvent = styleEvent
                            }
                            createColorEvent = false
                        }

                        if (lastEvent!!.mPrevLineEvent == null) {
                            // There haven't been any line events yet.
                            // So the comments that have been gathered up until now
                            // can just be shown on the song startup screen.
                            stopAddingStartupItems = true
                        }

                        val bpbThisLine = fileLine.mBeatInfo.mBPB
                        val bpmThisLine = fileLine.mBeatInfo.mBPM
                        val scrollBeatThisLine = fileLine.mBeatInfo.mScrollBeat
                        val scrollBeatOffsetThisLine = fileLine.mBeatInfo.mScrollBeatOffset
                        val bars = fileLine.mBeatInfo.mBPL

                        var displayLine=fileLine.mTaglessLine
                        if (lineImage != null && (displayLine.isNotEmpty() || chordsFound))
                            parsingState.mErrors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.text_found_with_image)))
                        if (displayLine.isEmpty() && !chordsFound)
                            displayLine = "▼"

                        if (createLine) {
                            displayLineCounter++

                            // Won't pay? We'll take it away!
                            if (displayLineCounter > DEMO_LINE_COUNT && !mRegistered) {
                                displayLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                                lineImage = null
                            }

                            var lastLine: Line? = null
                            if (lastEvent!!.mPrevLineEvent != null)
                                lastLine = lastEvent!!.mPrevLineEvent!!.mLine
                            var lastScrollBeatOffset = 0
                            var lastBPB = bpbThisLine
                            var lastScrollBeat = scrollBeatThisLine
                            if (lastLine != null) {
                                lastBPB = lastLine.mBeatInfo.mBPB
                                lastScrollBeatOffset = lastLine.mBeatInfo.mScrollBeatOffset
                                lastScrollBeat = lastLine.mBeatInfo.mScrollBeat
                            }
                            val scrollBeatDifference = scrollBeatThisLine - bpbThisLine - (lastScrollBeat - lastBPB)

                            var beatsForThisLine = bpbThisLine * bars
                            val simpleBeatsForThisLine = beatsForThisLine
                            beatsForThisLine += scrollBeatOffsetThisLine
                            beatsForThisLine += scrollBeatDifference
                            beatsForThisLine -= lastScrollBeatOffset

                            nanosecondsPerBeat = if (bpmThisLine > 0)
                                Utils.nanosecondsPerBeat(bpmThisLine)
                            else
                                0

                            var totalLineTime: Long
                            totalLineTime = if (pauseTime > 0)
                                Utils.milliToNano(pauseTime)
                            else
                                beatsForThisLine * nanosecondsPerBeat
                            if (totalLineTime == 0L || mCurrentScrollMode === ScrollingMode.Smooth)
                                totalLineTime = timePerBar * bars

                            val lineObj: Line
                            if (lineImage != null) {
                                lineObj = ImageLine(currentTime, totalLineTime, lineImage!!, imageScalingMode, lastEvent!!.mPrevColorEvent!!,fileLine.mBeatInfo)
                                lineImage = null
                            } else
                                lineObj = TextLine(currentTime, totalLineTime, displayLine, tagsToProcess, lastEvent!!.mPrevColorEvent!!, fileLine.mBeatInfo)

                            lastLine?.insertAfter(lineObj)
                            val lineEvent = lineObj.mLineEvent
                            if (firstLine == null)
                                firstLine = lineObj
                            lastEvent!!.insertEvent(lineEvent)

                            // generate beats ...

                            // if a pause is specified on a line, it replaces the actual beats for that line.
                            if (pauseTime > 0) {
                                currentTime = generatePause(pauseTime, lastEvent, currentTime)
                                lastEvent = lastEvent!!.lastEvent
                                lineEvent.mLine.mYStartScrollTime = currentTime - nanosecondsPerBeat
                                lineEvent.mLine.mYStopScrollTime = currentTime
                            } else if (bpmThisLine > 0 && mCurrentScrollMode !== ScrollingMode.Smooth) {
                                var finished = false
                                var beatThatWeWillScrollOn = 0
                                val rolloverBeatCount = rolloverBeats.size
                                val beatsToAdjustCount = beatsToAdjust
                                if (beatsToAdjust > 0) {
                                    // We have N beats to adjust.
                                    // For the previous N beatevents, set the BPB to the new BPB.
                                    var lastBeatEvent = lastEvent!!.mPrevBeatEvent
                                    while (lastBeatEvent != null && beatsToAdjust > 0) {
                                        lastBeatEvent.mBPB = bpbThisLine
                                        beatsToAdjust--
                                        lastBeatEvent = if (lastBeatEvent.mPrevEvent != null)
                                            lastBeatEvent.mPrevEvent!!.mPrevBeatEvent
                                        else
                                            null
                                    }
                                    beatsToAdjust = 0
                                }

                                var currentBarBeat = 0
                                while (!finished && currentBarBeat < beatsForThisLine) {
                                    val beatsRemaining = beatsForThisLine - currentBarBeat
                                    beatThatWeWillScrollOn = if (beatsRemaining > bpbThisLine)
                                        -1
                                    else
                                        (currentBeat + (beatsRemaining - 1)) % bpbThisLine
                                    val beatEvent: BeatEvent
                                    var rolloverBPB = 0
                                    var rolloverBeatLength: Long = 0
                                    if (rolloverBeats.isEmpty())
                                        beatEvent = BeatEvent(currentTime, bpmThisLine, bpbThisLine, bars, currentBeat, metronomeOn, beatThatWeWillScrollOn)
                                    else {
                                        beatEvent = rolloverBeats[0]
                                        beatEvent.mWillScrollOnBeat = beatThatWeWillScrollOn
                                        rolloverBPB = beatEvent.mBPB
                                        rolloverBeatLength = Utils.nanosecondsPerBeat(beatEvent.mBPM)
                                        rolloverBeats.removeAt(0)
                                    }
                                    lastEvent!!.insertEvent(beatEvent)
                                    val beatTimeLength = if (rolloverBeatLength == 0L) nanosecondsPerBeat else rolloverBeatLength
                                    val nanoPerBeat = beatTimeLength / 4.0
                                    // generate MIDI beats.
                                    if (lastBeatBlock == null || nanoPerBeat != lastBeatBlock.nanoPerBeat) {
                                        lastBeatBlock = BeatBlock(beatEvent.mEventTime, midiBeatCounter++, nanoPerBeat)
                                        val beatBlock = lastBeatBlock
                                        beatBlocks.add(beatBlock)
                                    }

                                    if (currentBarBeat == beatsForThisLine - 1) {
                                        lineEvent.mLine.mYStartScrollTime = if (mCurrentScrollMode === ScrollingMode.Smooth) lineEvent.mEventTime else currentTime
                                        lineEvent.mLine.mYStopScrollTime = currentTime + nanosecondsPerBeat
                                        finished = true
                                    }
                                    currentTime += beatTimeLength
                                    currentBeat++
                                    if (currentBeat == (if (rolloverBPB > 0) rolloverBPB else bpbThisLine))
                                        currentBeat = 0
                                    ++currentBarBeat
                                }

                                beatsForThisLine -= rolloverBeatCount
                                beatsForThisLine += beatsToAdjustCount
                                if (beatsForThisLine > simpleBeatsForThisLine) {
                                    // We need to store some information so that the next line can adjust the rollover beats.
                                    beatsToAdjust = beatsForThisLine - simpleBeatsForThisLine
                                } else if (beatsForThisLine < simpleBeatsForThisLine) {
                                    // We need to generate a few beats to store for the next line to use.
                                    rolloverBeats.clear()
                                    var rolloverCurrentBeat = currentBeat
                                    var rolloverCurrentTime = currentTime
                                    for (f in beatsForThisLine until simpleBeatsForThisLine) {
                                        rolloverBeats.add(BeatEvent(rolloverCurrentTime, bpmThisLine, bpbThisLine, bars, rolloverCurrentBeat++, metronomeOn, beatThatWeWillScrollOn))
                                        rolloverCurrentTime += nanosecondsPerBeat
                                        if (rolloverCurrentBeat == bpbThisLine)
                                            rolloverCurrentBeat = 0
                                    }
                                }
                            } else {
                                lineEvent.mLine.mYStartScrollTime = currentTime
                                currentTime += totalLineTime
                                lineEvent.mLine.mYStopScrollTime = currentTime
                            }
                        } else if (pauseTime > 0)
                            currentTime = generatePause(pauseTime, lastEvent, currentTime)

                        lastEvent = lastEvent!!.lastEvent
                    }
                }
                mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_READ, lineCounter, mSongFile.mLines).sendToTarget()
            }

            var countTime: Long = 0
            // Create count events
            if (firstLine?.mBeatInfo?.mScrollingMode==ScrollingMode.Beat) {
                val firstBeatEvent = firstEvent!!.firstBeatEvent
                val countbpm = firstBeatEvent?.mBPM ?: 120.0
                val countbpb = firstBeatEvent?.mBPB ?: 4
                val countbpl = firstBeatEvent?.mBPL ?: 1
                var insertAfterEvent = firstEvent
                if (count > 0) {
                    val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                    for (f in 0 until count)
                        for (g in 0 until countbpb) {
                            val countEvent = BeatEvent(countTime, countbpm, countbpb, countbpl, g, mMetronomeContext === MetronomeContext.DuringCountIn || metronomeOn, if (f == count - 1) countbpb - 1 else -1)
                            insertAfterEvent!!.insertAfter(countEvent)
                            insertAfterEvent = countEvent
                            countTime += nanoPerBeat
                        }
                    insertAfterEvent!!.offsetLaterEvents(countTime)
                } else {
                    val baseBeatEvent = BeatEvent(0, countbpm, countbpb, countbpl, countbpb, false, -1)
                    firstEvent!!.insertAfter(baseBeatEvent)
                }
            }
            var trackEvent: TrackEvent? = null
            if (chosenAudioFile != null) {
                trackOffset = Utils.milliToNano(trackOffset.toInt()) // milli to nano
                trackOffset += countTime
                val eventBefore = firstEvent!!.findEventOnOrBefore(trackOffset)
                trackEvent = TrackEvent(if (trackOffset < 0) 0 else trackOffset)
                eventBefore!!.insertAfter(trackEvent)
                if (trackOffset < 0)
                    trackEvent.offsetLaterEvents(Math.abs(trackOffset))
            }
            if (firstLine?.lastLine!!.mBeatInfo.mScrollingMode === ScrollingMode.Beat) {
                // Last Y scroll should never happen. No point scrolling last line offscreen.
                val mLastLine = firstLine!!.lastLine
                mLastLine.mYStopScrollTime = Long.MAX_VALUE
                mLastLine.mYStartScrollTime = mLastLine.mYStopScrollTime
            }

            // Nothing at all in the song file? We at least want the colors set right.
            if (firstEvent == null)
                firstEvent = ColorEvent(currentTime, mBackgroundColour, mPulseColour, mLyricColour, mChordColour, mAnnotationColour, mBeatCounterColour, mScrollMarkerColour)

            val reallyTheLastEvent = firstEvent!!.lastEvent
            // In beat mode, or in any other mode where we're using a backing track, let's have an end event.
            if (trackEvent != null || mCurrentScrollMode !== ScrollingMode.Manual) {
                var trackEndTime: Long = 0
                if (trackEvent != null)
                    trackEndTime = trackEvent.mEventTime + sst.trackLength
                // The end event will be where the final beat occurs.
                // But there is a chance that the audio track is longer than that.
                val endEvent = EndEvent(Math.max(currentTime, trackEndTime))
                reallyTheLastEvent.add(endEvent)
            }

            if (mTriggerContext === TriggerOutputContext.Always || mTriggerContext === TriggerOutputContext.ManualStartOnly && !mLoadingSongFile.startedByMIDITrigger) {
                if (mSongFile.mProgramChangeTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mSongFile.mProgramChangeTrigger.getMIDIMessages(mDefaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }

                if (mSongFile.mSongSelectTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mSongFile.mSongSelectTrigger.getMIDIMessages(mDefaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }
            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(firstEvent, parsingState.mErrors)

            val song = Song(mSongFile, chosenAudioFile, chosenAudioVolume, comments, firstEvent!!, firstLine!!, parsingState.mErrors, mUserChosenScrollMode, mSendMidiClock, mLoadingSongFile.startedByBandLeader, mLoadingSongFile.nextSong, mLoadingSongFile.sourceDisplaySettings.mOrientation, initialMIDIMessages, beatBlocks, firstLine.mBeatInfo.mBPB, count)
            song.doMeasurements(Paint(), mCancelEvent, mSongLoadHandler, mLoadingSongFile.nativeDisplaySettings, mLoadingSongFile.sourceDisplaySettings)
            return song
        } finally {
            try {
                br.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close song file.", ioe)
            }
        }
    }

    companion object {
        private const val DEMO_LINE_COUNT = 15
        // Every beatstart/beatstop block has events that are offset by this amount (one year).
        // If you left the app running for a year, it would eventually progress. WHO WOULD DO SUCH A THING?
        private val BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS = Utils.milliToNano(1000 * 60 * 24 * 365)

        private fun offsetMIDIEvents(firstEvent: BaseEvent?, errors: MutableList<FileParseError>) {
            var event = firstEvent
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
                            errors.add(FileParseError(midiEvent.mOffset.mSourceTag, BeatPrompterApplication.getResourceString(R.string.midi_offset_is_before_start_of_song)))
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

        private fun generatePause(pauseTime: Long, lastEvent: BaseEvent?, currentTime: Long): Long {
            var vLastEvent = lastEvent
            var vCurrentTime = currentTime
            // pauseTime is in milliseconds.
            // We don't want to generate thousands of events, so let's say every 1/10th of a second.
            val deciSeconds = Math.ceil(pauseTime.toDouble() / 100.0).toInt()
            val remainder = Utils.milliToNano(pauseTime) - Utils.milliToNano(deciSeconds * 100)
            val oneDeciSecondInNanoseconds = Utils.milliToNano(100)
            vCurrentTime += remainder
            for (f in 0 until deciSeconds) {
                val pauseEvent = PauseEvent(vCurrentTime, deciSeconds, f)
                vLastEvent!!.insertEvent(pauseEvent)
                vLastEvent = vLastEvent.lastEvent
                vCurrentTime += oneDeciSecondInNanoseconds
            }
            return vCurrentTime
        }
    }
}
