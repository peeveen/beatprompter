package com.stevenfrew.beatprompter.cache.parse

import android.graphics.*
import android.os.Handler
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.song.event.*
import com.stevenfrew.beatprompter.comm.midi.message.Message
import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
import com.stevenfrew.beatprompter.graphics.ScreenString
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.LineGraphic
import com.stevenfrew.beatprompter.midi.BeatBlock
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.song.*
import com.stevenfrew.beatprompter.song.line.ImageLine
import com.stevenfrew.beatprompter.song.line.Line
import com.stevenfrew.beatprompter.song.line.TextLine
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.song.load.SongLoadCancelEvent
import com.stevenfrew.beatprompter.song.load.SongLoadCancelledException
import com.stevenfrew.beatprompter.song.load.SongLoadInfo
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.util.Utils
import kotlin.math.absoluteValue

@ParseTags(ImageTag::class, PauseTag::class, SendMIDIClockTag::class, CommentTag::class, CountTag::class,
        StartOfHighlightTag::class, EndOfHighlightTag::class,
        BarMarkerTag::class, BarsTag::class, BeatsPerMinuteTag::class, BeatsPerBarTag::class, BarsPerLineTag::class,
        ScrollBeatModifierTag::class, ScrollBeatTag::class, BeatStartTag::class, BeatStopTag::class, AudioTag::class,
        MIDIEventTag::class, ChordTag::class)
@IgnoreTags(LegacyTag::class, TimeTag::class, MIDISongSelectTriggerTag::class, MIDIProgramChangeTriggerTag::class,
        TitleTag::class, ArtistTag::class, KeyTag::class, TagTag::class, FilterOnlyTag::class)
/**
 * Song file parser. This returns the full information for playing the song.
 */
class SongParser constructor(private val mSongLoadInfo: SongLoadInfo,
                             private val mSongLoadCancelEvent: SongLoadCancelEvent,
                             private val mSongLoadHandler: Handler,
                             private val mRegistered: Boolean)
    : SongFileParser<Song>(mSongLoadInfo.mSongFile,
        mSongLoadInfo.initialScrollMode,
        mSongLoadInfo.mixedModeActive,
        true) {
    private val mMetronomeContext: MetronomeContext
    private val mCustomCommentsUser: String
    private val mShowChords: Boolean
    private val mShowKey: Boolean
    private val mShowBPM: ShowBPMContext
    private val mTriggerContext: TriggerOutputContext
    private val mNativeDeviceSettings: DisplaySettings
    private val mInitialMIDIMessages = mutableListOf<OutgoingMessage>()
    private var mStopAddingStartupItems = false
    private val mStartScreenComments = mutableListOf<Song.Comment>()
    private val mEvents = mutableListOf<BaseEvent>()
    private val mLines = LineList()
    private val mRolloverBeats = mutableListOf<BeatEvent>()
    private val mBeatBlocks = mutableListOf<BeatBlock>()
    private val mPaint = Paint()
    private val mFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val mDefaultHighlightColor: Int
    private val mAudioTags = mutableListOf<AudioTag>()
    private val mTimePerBar: Long

    private var mSongHeight = 0
    private var mMIDIBeatCounter: Int = 0
    private var mLastBeatBlock: BeatBlock? = null
    private var mBeatsToAdjust: Int = 0
    private var mCurrentBeat: Int = 0
    private var mDisplayLineCounter: Int = 0
    private var mCountIn: Int
    private var mSendMidiClock: Boolean = false
    private var mSongTime: Long = 0
    private var mDefaultMIDIOutputChannel: Byte

    init {
        // All songFile info parsing errors count as our errors too.
        mErrors.addAll(mSongLoadInfo.mSongFile.mErrors)

        mSendMidiClock = BeatPrompterPreferences.sendMIDIClock
        mCountIn = BeatPrompterPreferences.defaultCountIn
        mMetronomeContext = BeatPrompterPreferences.metronomeContext
        mDefaultHighlightColor = BeatPrompterPreferences.defaultHighlightColor
        mCustomCommentsUser = BeatPrompterPreferences.customCommentsUser
        mShowChords = BeatPrompterPreferences.showChords
        mTriggerContext = BeatPrompterPreferences.sendMIDITriggerOnStart
        val defaultMIDIOutputChannelPrefValue = BeatPrompterPreferences.defaultMIDIOutputChannel
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
        mShowKey = BeatPrompterPreferences.showKey && mSongLoadInfo.mSongFile.mKey.isNotBlank()
        mShowBPM = if (mSongLoadInfo.mSongFile.mBPM > 0.0) BeatPrompterPreferences.showBPMContext else ShowBPMContext.No

        // Figure out the screen size
        mNativeDeviceSettings = translateSourceDeviceSettingsToNative(mSongLoadInfo.mSourceDisplaySettings, mSongLoadInfo.mNativeDisplaySettings)

        // Start the progress message dialog
        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mSongLoadInfo.mSongFile.mLines).sendToTarget()

        val lengthOfBackingTrack = mSongLoadInfo.mTrack?.mDuration ?: 0L
        var songTime = if (mSongLoadInfo.mSongFile.mDuration == Utils.TRACK_AUDIO_LENGTH_VALUE) lengthOfBackingTrack else mSongLoadInfo.mSongFile.mDuration
        if (songTime > 0 && mSongLoadInfo.mSongFile.mTotalPauses > songTime) {
            mErrors.add(FileParseError(R.string.pauseLongerThanSong))
            mOngoingBeatInfo = SongBeatInfo(mScrollMode = ScrollingMode.Manual)
            mCurrentLineBeatInfo = LineBeatInfo(mOngoingBeatInfo)
            songTime = 0
        }

        mTimePerBar = if (songTime > 0L) (songTime.toDouble() / mSongLoadInfo.mSongFile.mBars).toLong() else 0
    }

    override fun parseLine(line: TextFileLine<Song>) {
        if (mSongLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()
        super.parseLine(line)

        val chordTags = line.mTags.filterIsInstance<ChordTag>()
        val nonChordTags = line.mTags.filter { it !is ChordTag }
        val chordsFound = mShowChords && !chordTags.isEmpty()
        val chordsFoundButNotShowingThem = !mShowChords && chordsFound
        val tags = if (mShowChords) line.mTags.toList() else nonChordTags

        var workLine = line.mLineWithNoTags

        // Generate clicking beats if the metronome is on.
        // The "on when no track" logic will be performed during song playback.
        val metronomeOn = mMetronomeContext === MetronomeContext.On || mMetronomeContext === MetronomeContext.OnWhenNoTrack

        var imageTag = tags.asSequence().filterIsInstance<ImageTag>().firstOrNull()

        if (!mSendMidiClock)
            mSendMidiClock = tags.any { it is SendMIDIClockTag }

        if (!mStopAddingStartupItems)
            mCountIn = tags.filterIsInstance<CountTag>().firstOrNull()?.mCount ?: mCountIn

        val commentTags = tags.filterIsInstance<CommentTag>()
        commentTags.forEach {
            val comment = Song.Comment(it.mComment, it.mAudience, mNativeDeviceSettings.mScreenSize, mPaint, mFont)
            if (comment.isIntendedFor(mCustomCommentsUser))
                if (mStopAddingStartupItems)
                    mEvents.add(CommentEvent(mSongTime, comment))
                else
                    mStartScreenComments.add(comment)
        }

        val midiEventTags = tags.filterIsInstance<MIDIEventTag>()
        midiEventTags.forEach {
            if (mDisplayLineCounter < DEMO_LINE_COUNT || mRegistered) {
                if (mStopAddingStartupItems)
                    mEvents.add(it.toMIDIEvent(mSongTime))
                else {
                    mInitialMIDIMessages.addAll(it.mMessages)
                    if (it.mOffset != null)
                        mErrors.add(FileParseError(it, R.string.midi_offset_before_first_line))
                }
            }
        }

        val audioTagsThisLine = tags.filterIsInstance<AudioTag>()
        // If there are multiple {audio} tags on a line, and we've actually reached song content, add an error.
        // You should only be allowed to define multiple {audio} tags in the pre-song section.
        if (mStopAddingStartupItems) {
            if (audioTagsThisLine.size > 1) {
                mErrors.add(FileParseError(line.mLineNumber, R.string.multiple_audio_tags_on_song_line))
                mAudioTags.add(audioTagsThisLine.first())
            }
        } else
            mAudioTags.addAll(audioTagsThisLine)

        val createLine = (workLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || imageTag != null)
        // Contains only tags? Or contains nothing? Don't use it as a blank line.

        val pauseTag = tags.asSequence().filterIsInstance<PauseTag>().firstOrNull()
        if (createLine || pauseTag != null) {
            // We definitely have a line!
            // So now is when we want to create the count-in (if any)
            if (mCountIn > 0) {
                val countInEvents = generateCountInEvents(mCountIn, mMetronomeContext === MetronomeContext.DuringCountIn || metronomeOn)
                mEvents.addAll(countInEvents.mEvents)
                mSongTime = countInEvents.mBlockEndTime
                mCountIn = 0
            }

            val audioTag =
                    when {
                        mStopAddingStartupItems -> audioTagsThisLine.firstOrNull()
                        mSongLoadInfo.mTrack == null -> mAudioTags.firstOrNull()
                        else -> mAudioTags.firstOrNull { it.mFilename == mSongLoadInfo.mTrack.mNormalizedName }
                    }
            mAudioTags.clear()

            // No audio WHATSOEVER in manual mode
            if (audioTag != null && !mSongLoadInfo.mNoAudio) {
                // Make sure file exists.
                val mappedTracks = SongListActivity.mCachedCloudFiles.getMappedAudioFiles(audioTag.mFilename)
                if (mappedTracks.isEmpty())
                    mErrors.add(FileParseError(audioTag, R.string.cannotFindAudioFile, audioTag.mFilename))
                else if (mappedTracks.size > 1)
                    mErrors.add(FileParseError(audioTag, R.string.multipleFilenameMatches, audioTag.mFilename))
                else {
                    val audioFile = mappedTracks.first()
                    if (!audioFile.mFile.exists())
                        mErrors.add(FileParseError(audioTag, R.string.cannotFindAudioFile, audioTag.mFilename))
                    else
                        mEvents.add(AudioEvent(mSongTime, audioFile, audioTag.mVolume, !mStopAddingStartupItems))
                }
            }

            // Any comments or MIDI events from here will be part of the song,
            // rather than startup events.
            mStopAddingStartupItems = true

            if (imageTag != null && (workLine.isNotBlank() || chordsFound))
                mErrors.add(FileParseError(line.mLineNumber, R.string.text_found_with_image))
            if (workLine.isBlank() && (!chordsFound || chordsFoundButNotShowingThem))
                workLine = "▼"

            // Generate pause events if required (may return null)
            val pauseEvents = generatePauseEvents(mSongTime, pauseTag)

            if (createLine) {
                // Won't pay? We'll take it away!
                if (mDisplayLineCounter > DEMO_LINE_COUNT && !mRegistered) {
                    workLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                    imageTag = null
                }

                // First line should always have a time of zero, so that if the user scrolls
                // back to the start of the song, it still picks up any count-in beat events.
                val lineStartTime = if (mLines.isEmpty()) 0L else mSongTime

                // If the first line is a pause event, we need to adjust the total line time accordingly
                // to include any count-in
                val addToPause = if (mLines.isEmpty()) mSongTime else 0L

                // Generate beat events (may return null in smooth mode)
                val beatEvents = generateBeatEvents(mSongTime, metronomeOn)

                // Calculate how long this line will last for
                val lineDuration = calculateLineDuration(pauseTag, addToPause, lineStartTime, beatEvents)

                // Calculate the start and stop scroll times for this line
                val startAndStopScrollTimes = calculateStartAndStopScrollTimes(pauseTag, lineStartTime + addToPause, lineDuration, beatEvents)

                // Create the line
                var lineObj: Line? = null
                if (imageTag != null) {
                    val imageFiles = SongListActivity.mCachedCloudFiles.getMappedImageFiles(imageTag.mFilename)
                    if (imageFiles.isNotEmpty())
                        try {
                            lineObj = ImageLine(imageFiles.first(), imageTag.mImageScalingMode, lineStartTime, lineDuration, mCurrentLineBeatInfo.mScrollMode, mNativeDeviceSettings, mSongHeight, startAndStopScrollTimes)
                        } catch (t: Throwable) {
                            // Bitmap loading could cause error here. Even OutOfMemory!
                            mErrors.add(FileParseError(imageTag, t))
                        }
                    else {
                        workLine = BeatPrompterApplication.getResourceString(R.string.missing_image_file_warning)
                        mErrors.add(FileParseError(imageTag, R.string.missing_image_file_warning))
                        imageTag = null
                    }
                }
                if (imageTag == null)
                    lineObj = TextLine(workLine, tags, lineStartTime, lineDuration, mCurrentLineBeatInfo.mScrollMode, mNativeDeviceSettings, mLines.filterIsInstance<TextLine>().lastOrNull()?.mTrailingHighlightColor, mSongHeight, startAndStopScrollTimes, mSongLoadCancelEvent)

                if (lineObj != null) {
                    mLines.add(lineObj)
                    mEvents.add(LineEvent(lineObj.mLineTime, lineObj))

                    mSongHeight += lineObj.mMeasurements.mLineHeight

                    // If a pause is going to be generated, then we don't need beats.
                    if (pauseEvents == null) {
                        // Otherwise, add any generated beats
                        if (beatEvents != null) {
                            mEvents.addAll(beatEvents.mEvents)
                            mSongTime = beatEvents.mBlockEndTime
                        }
                        // Otherwise, forget it, just bump up the song time
                        else
                            mSongTime += lineDuration
                    }
                }
            }
            // Now add the pause events to the song (if required).
            if (pauseEvents != null && pauseTag != null) {
                mEvents.addAll(pauseEvents)
                mSongTime += pauseTag.mDuration
            }
        } else
        // If there is no actual line data, then the scroll beat offset never took effect.
        // Clear it so that the next line (which MIGHT be a proper line) doesn't take it into account.
            mCurrentLineBeatInfo = LineBeatInfo(mCurrentLineBeatInfo.mBeats, mCurrentLineBeatInfo.mBPL, mCurrentLineBeatInfo.mBPB, mCurrentLineBeatInfo.mBPM, mCurrentLineBeatInfo.mScrollBeat, 0, mCurrentLineBeatInfo.mScrollMode)

        mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, line.mLineNumber, mSongLoadInfo.mSongFile.mLines).sendToTarget()
    }

    override fun getResult(): Song {
        // Song has no lines? Make a dummy line so we don't have to check for null everywhere in the code.
        if (mLines.isEmpty())
            throw InvalidBeatPrompterFileException(R.string.no_lines_in_song_file)

        val smoothMode = mLines.asSequence().filter { it.mScrollMode == ScrollingMode.Smooth }.any()

        val startScreenStrings = createStartScreenStrings()
        val totalStartScreenTextHeight = startScreenStrings.first.sumBy { it.mHeight }

        // In smooth scrolling mode, the display will start scrolling immediately.
        // This is an essential feature of smooth scrolling mode, yet causes a problem: the first line
        // will almost immediately become obscured, just as you are performing it.
        // To combat this, there will an initial blank "buffer zone", created by offsetting the graphical
        // display by a number of pixels.
        val smoothScrollOffset =
                if (smoothMode)
                // Obviously this will only be required if the song cannot fit entirely onscreen.
                    if (mSongHeight > mNativeDeviceSettings.mUsableScreenHeight)
                        Math.min(mLines.asSequence().map { it.mMeasurements.mLineHeight }.maxBy { it }
                                ?: 0, (mNativeDeviceSettings.mScreenSize.height() / 3.0).toInt())
                    else
                        0
                else
                    0

        // Get all required audio info ...
        val audioEvents = mEvents.filterIsInstance<AudioEvent>()

        // Allocate graphics objects.
        val maxGraphicsRequired = getMaximumGraphicsRequired(mNativeDeviceSettings.mScreenSize.height())
        val lineGraphics = CircularGraphicsList()
        repeat(maxGraphicsRequired) {
            lineGraphics.add(LineGraphic(getBiggestLineSize(it, maxGraphicsRequired)))
        }

        // There may be no lines! So we have to check ...
        if (lineGraphics.isNotEmpty()) {
            var graphic: LineGraphic = lineGraphics.first()
            mLines.forEach { line ->
                repeat(line.mMeasurements.mLines) {
                    line.allocateGraphic(graphic)
                    graphic = graphic.mNextGraphic
                }
            }
        }

        val beatCounterHeight = mNativeDeviceSettings.mBeatCounterRect.height()
        val maxSongTitleWidth = mNativeDeviceSettings.mScreenSize.width() * 0.9f
        val maxSongTitleHeight = beatCounterHeight * 0.9f
        val vMargin = (beatCounterHeight - maxSongTitleHeight) / 2.0f
        val songTitleHeader = ScreenString.create(mSongLoadInfo.mSongFile.mTitle, mPaint, maxSongTitleWidth.toInt(), maxSongTitleHeight.toInt(), Utils.makeHighlightColour(Color.BLACK, 0x80.toByte()), mFont, false)
        val extraMargin = (maxSongTitleHeight - songTitleHeader.mHeight) / 2.0f
        val x = ((mNativeDeviceSettings.mScreenSize.width() - songTitleHeader.mWidth) / 2.0).toFloat()
        val y = beatCounterHeight - (extraMargin + songTitleHeader.mDescenderOffset.toFloat() + vMargin)
        val songTitleHeaderLocation = PointF(x, y)

        // First of all, offset any MIDI events that have an offset.
        val offsetEventList = offsetMIDIEvents()

        // OK, now sort all events by time, and type within time
        val sortedEventList = sortEvents(offsetEventList).toMutableList()

        // Now we need to figure out which lines should NOT scroll offscreen.
        val noScrollLines = mutableListOf<Line>()
        val lastLineIsBeat = mLines.lastOrNull()?.mScrollMode == ScrollingMode.Beat
        if (lastLineIsBeat) {
            noScrollLines.add(mLines.last())
            sortedEventList.removeAt(mEvents.indexOfLast { it is LineEvent })
        } else if (smoothMode) {
            var availableScreenHeight = mNativeDeviceSettings.mUsableScreenHeight - smoothScrollOffset
            val lineEvents = mEvents.filterIsInstance<LineEvent>()
            for (lineEvent in lineEvents.reversed()) {
                availableScreenHeight -= lineEvent.mLine.mMeasurements.mLineHeight
                if (availableScreenHeight >= 0) {
                    noScrollLines.add(lineEvent.mLine)
                    sortedEventList.remove(lineEvent)
                } else
                    break
            }
        }

        // To generate the EndEvent, we need to know the time that the
        // song ends. This could be the time of the final generated event,
        // but there might still be an audio file playing, so find out
        // when the last track ends ...
        val lastAudioEndTime = sortedEventList.asSequence().filterIsInstance<AudioEvent>().map { it.mAudioFile.mDuration + it.mEventTime }.max()
        sortedEventList.add(EndEvent(Math.max(lastAudioEndTime ?: 0L, mSongTime)))

        // Now build the final event list.
        val firstEvent = buildLinkedEventList(sortedEventList)

        // Calculate the last position that we can scroll to.
        val scrollEndPixel = calculateScrollEndPixel(smoothMode, smoothScrollOffset)

        if ((mTriggerContext == TriggerOutputContext.Always) || (mTriggerContext == TriggerOutputContext.ManualStartOnly && !mSongLoadInfo.mStartedByMIDITrigger)) {
            mInitialMIDIMessages.addAll(mSongLoadInfo.mSongFile.mProgramChangeTrigger.getMIDIMessages(mDefaultMIDIOutputChannel))
            mInitialMIDIMessages.addAll(mSongLoadInfo.mSongFile.mSongSelectTrigger.getMIDIMessages(mDefaultMIDIOutputChannel))
        }

        return Song(mSongLoadInfo.mSongFile, mNativeDeviceSettings, firstEvent, mLines, audioEvents,
                mInitialMIDIMessages, mBeatBlocks, mSendMidiClock, startScreenStrings.first, startScreenStrings.second,
                totalStartScreenTextHeight, mSongLoadInfo.mStartedByBandLeader, mSongLoadInfo.mNextSong,
                smoothScrollOffset, mSongHeight, scrollEndPixel, noScrollLines, mNativeDeviceSettings.mBeatCounterRect, songTitleHeader,
                songTitleHeaderLocation, mSongLoadInfo.mLoadID)
    }

    private fun calculateScrollEndPixel(smoothMode: Boolean, smoothScrollOffset: Int): Int {
        val manualDisplayEnd = Math.max(0, mSongHeight - mNativeDeviceSettings.mUsableScreenHeight)
        val beatDisplayEnd = mLines.lastOrNull { it.mScrollMode === ScrollingMode.Beat }?.mSongPixelPosition
        return if (smoothMode)
            manualDisplayEnd + smoothScrollOffset//+smoothScrollEndOffset
        else if (beatDisplayEnd != null)
            if (beatDisplayEnd + mNativeDeviceSettings.mUsableScreenHeight > mSongHeight)
                beatDisplayEnd
            else
                manualDisplayEnd
        else
            manualDisplayEnd
    }

    private fun getBiggestLineSize(index: Int, modulus: Int): Rect {
        var maxHeight = 0
        var maxWidth = 0
        var lineCount = 0
        mLines.forEach {
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
        repeat(mLines.size) { start ->
            var heightCounter = 0
            var lineCounter = 0
            for (f in start until mLines.size) {
                if (heightCounter < screenHeight) {
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

    private fun generateBeatEvents(startTime: Long, click: Boolean): EventBlock? {
        if (mCurrentLineBeatInfo.mScrollMode === ScrollingMode.Smooth)
            return null
        var eventTime = startTime
        val beatEvents = mutableListOf<BeatEvent>()
        var beatThatWeWillScrollOn = 0
        val currentTimePerBeat = Utils.nanosecondsPerBeat(mCurrentLineBeatInfo.mBPM)
        val rolloverBeatCount = mRolloverBeats.size
        val beatsToAdjustCount = mBeatsToAdjust
        // We have N beats to adjust.
        // For the previous N beatevents, set the BPB to the new BPB.
        if (mBeatsToAdjust > 0)
            mEvents.filterIsInstance<BeatEvent>().takeLast(mBeatsToAdjust).forEach {
                it.mBPB = mCurrentLineBeatInfo.mBPB
            }
        mBeatsToAdjust = 0

        var currentLineBeat = 0
        while (currentLineBeat < mCurrentLineBeatInfo.mBeats) {
            val beatsRemaining = mCurrentLineBeatInfo.mBeats - currentLineBeat
            beatThatWeWillScrollOn = if (beatsRemaining > mCurrentLineBeatInfo.mBPB)
                -1
            else
                (mCurrentBeat + (beatsRemaining - 1)) % mCurrentLineBeatInfo.mBPB
            val beatEvent: BeatEvent
            var rolloverBPB = 0
            var rolloverBeatLength: Long = 0
            if (mRolloverBeats.isEmpty())
                beatEvent = BeatEvent(eventTime, mCurrentLineBeatInfo.mBPM, mCurrentLineBeatInfo.mBPB, mCurrentBeat, click, beatThatWeWillScrollOn)
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
            ++currentLineBeat
        }

        val beatsThisLine = mCurrentLineBeatInfo.mBeats - rolloverBeatCount + beatsToAdjustCount
        val simpleBeatsThisLine = mCurrentLineBeatInfo.mBPB * mCurrentLineBeatInfo.mBPL
        if (beatsThisLine > simpleBeatsThisLine) {
            // We need to store some information so that the next line can adjust the rollover beats.
            mBeatsToAdjust = mCurrentLineBeatInfo.mBeats - simpleBeatsThisLine
        } else if (beatsThisLine < simpleBeatsThisLine) {
            // We need to generate a few beats to store for the next line to use.
            mRolloverBeats.clear()
            var rolloverCurrentBeat = mCurrentBeat
            var rolloverCurrentTime = eventTime
            for (f in beatsThisLine until simpleBeatsThisLine) {
                mRolloverBeats.add(BeatEvent(rolloverCurrentTime, mCurrentLineBeatInfo.mBPM, mCurrentLineBeatInfo.mBPB, rolloverCurrentBeat++, click, beatThatWeWillScrollOn))
                rolloverCurrentTime += currentTimePerBeat
                if (rolloverCurrentBeat == mCurrentLineBeatInfo.mBPB)
                    rolloverCurrentBeat = 0
            }
        }
        return EventBlock(beatEvents, eventTime)
    }

    private fun generatePauseEvents(startTime: Long, pauseTag: PauseTag?): List<PauseEvent>? {
        if (pauseTag == null)
            return null
        // pauseTime is in milliseconds.
        // We don't want to generate thousands of events, so let's say every 1/10th of a second.
        var eventTime = startTime
        val pauseEvents = mutableListOf<PauseEvent>()
        val deciSeconds = Math.ceil(Utils.nanoToMilli(pauseTag.mDuration).toDouble() / 100.0).toInt()
        val remainder = pauseTag.mDuration - Utils.milliToNano(deciSeconds * 100)
        val oneDeciSecondInNanoseconds = Utils.milliToNano(100)
        eventTime += remainder
        repeat(deciSeconds) {
            val pauseEvent = PauseEvent(eventTime, deciSeconds, it)
            pauseEvents.add(pauseEvent)
            eventTime += oneDeciSecondInNanoseconds
        }
        return pauseEvents
    }

    private fun generateCountInEvents(countBars: Int, click: Boolean): EventBlock {
        val countInEvents = mutableListOf<BeatEvent>()
        var startTime = 0L
        if (countBars > 0) {
            if (mCurrentLineBeatInfo.mBPM > 0.0) {
                val countbpm = mCurrentLineBeatInfo.mBPM
                val countbpb = mCurrentLineBeatInfo.mBPB
                val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                repeat(countBars) { bar ->
                    repeat(countbpb) { beat ->
                        countInEvents.add(BeatEvent(startTime, mCurrentLineBeatInfo.mBPM, mCurrentLineBeatInfo.mBPB, beat, click, if (bar == countBars - 1) countbpb - 1 else -1))
                        startTime += nanoPerBeat
                    }
                }
            }
        }
        return EventBlock(countInEvents, startTime)
    }

    /**
     * Each MIDIEvent might have an offset. Process that here.
     */
    private fun offsetMIDIEvents(): List<BaseEvent> {
        val beatEvents = mEvents.asSequence().filterIsInstance<BeatEvent>().sortedBy { it.mEventTime }.toList()
        return mEvents.map {
            if (it is MIDIEvent)
                offsetMIDIEvent(it, beatEvents)
            else
                it
        }
    }

    /**
     * Each MIDIEvent might have an offset. Process that here.
     */
    private fun offsetMIDIEvent(midiEvent: MIDIEvent, beatEvents: List<BeatEvent>): MIDIEvent {
        if (midiEvent.mOffset != null)
            if (midiEvent.mOffset.mAmount != 0) {
                // OK, this event needs moved.
                var newTime: Long = -1
                if (midiEvent.mOffset.mOffsetType === EventOffsetType.Milliseconds) {
                    val offset = Utils.milliToNano(midiEvent.mOffset.mAmount)
                    newTime = midiEvent.mEventTime + offset
                } else {
                    // Offset by beat count.
                    val beatCount = midiEvent.mOffset.mAmount
                    val beatsBeforeOrAfterThisMIDIEvent = beatEvents.filter {
                        if (beatCount >= 0)
                            it.mEventTime > midiEvent.mEventTime
                        else
                            it.mEventTime < midiEvent.mEventTime
                    }
                    val beatsInOrder =
                            if (beatCount < 0)
                                beatsBeforeOrAfterThisMIDIEvent.reversed()
                            else
                                beatsBeforeOrAfterThisMIDIEvent
                    val beatWeWant = beatsInOrder.asSequence().take(beatCount.absoluteValue).lastOrNull()
                    if (beatWeWant != null)
                        newTime = beatWeWant.mEventTime
                }
                if (newTime < 0) {
                    mErrors.add(FileParseError(midiEvent.mOffset.mSourceFileLineNumber, R.string.midi_offset_is_before_start_of_song))
                    newTime = 0
                }
                return MIDIEvent(newTime, midiEvent.mMessages)
            }
        return midiEvent
    }

    /**
     * Based on the difference in screen size/resolution/orientation, we will alter the min/max font size of our native settings.
     */
    private fun translateSourceDeviceSettingsToNative(sourceSettings: DisplaySettings, nativeSettings: DisplaySettings): DisplaySettings {
        val sourceScreenSize = sourceSettings.mScreenSize
        val sourceRatio = sourceScreenSize.width().toDouble() / sourceScreenSize.height().toDouble()
        val screenWillRotate = nativeSettings.mOrientation != sourceSettings.mOrientation
        val nativeScreenSize = if (screenWillRotate)
            Rect(0, 0, nativeSettings.mScreenSize.height(), nativeSettings.mScreenSize.width())
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
            mErrors.add(FileParseError(0, R.string.fontSizesAllMessedUp))
            maximumFontSize = minimumFontSize
        }
        return DisplaySettings(sourceSettings.mOrientation, minimumFontSize, maximumFontSize, nativeScreenSize, sourceSettings.mShowBeatCounter)
    }

    private fun createStartScreenStrings(): Pair<List<ScreenString>, ScreenString?> {
        // As for the start screen display (title/artist/comments/"press go"),
        // the title should take up no more than 20% of the height, the artist
        // no more than 10%, also 10% for the "press go" message.
        // The rest of the space is allocated for the comments and error messages,
        // each line no more than 10% of the screen height.
        val startScreenStrings = mutableListOf<ScreenString>()
        var availableScreenHeight = mNativeDeviceSettings.mScreenSize.height()
        var nextSongString: ScreenString? = null
        val boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (mSongLoadInfo.mNextSong.isNotBlank()) {
            // OK, we have a next song title to display.
            // This should take up no more than 15% of the screen.
            // But that includes a border, so use 13 percent for the text.
            val eightPercent = (mNativeDeviceSettings.mScreenSize.height() * 0.13).toInt()
            val nextSong = mSongLoadInfo.mNextSong
            val fullString = ">>> $nextSong >>>"
            nextSongString = ScreenString.create(fullString, mPaint, mNativeDeviceSettings.mScreenSize.width(), eightPercent, Color.BLACK, boldFont, true)
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
        val uniqueErrors = mErrors.asSequence().distinct().sortedBy { it.mLineNumber }.toList()
        var errorCount = uniqueErrors.size
        var messages = Math.min(errorCount, 6) + nonBlankCommentLines.size
        val showBPM = mShowBPM != ShowBPMContext.No
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
            if (mShowBPM != ShowBPMContext.No) {
                val rounded = mShowBPM == ShowBPMContext.Rounded || mSongLoadInfo.mSongFile.mBPM == mSongLoadInfo.mSongFile.mBPM.toInt().toDouble()
                var bpmString = BeatPrompterApplication.getResourceString(R.string.bpmPrefix) + ": "
                bpmString += if (rounded)
                    Math.round(mSongLoadInfo.mSongFile.mBPM).toInt()
                else
                    mSongLoadInfo.mSongFile.mBPM
                startScreenStrings.add(ScreenString.create(bpmString, mPaint, mNativeDeviceSettings.mScreenSize.width(), spacePerMessageLine, Color.CYAN, mFont, false))
            }
        }
        if (mSongLoadInfo.mSongLoadMode !== ScrollingMode.Manual)
            startScreenStrings.add(ScreenString.create(BeatPrompterApplication.getResourceString(R.string.tapTwiceToStart), mPaint, mNativeDeviceSettings.mScreenSize.width(), tenPercent, Color.GREEN, boldFont, true))
        return startScreenStrings to nextSongString
    }

    private fun calculateStartAndStopScrollTimes(pauseTag: PauseTag?, lineStartTime: Long, lineDuration: Long, currentBeatEvents: EventBlock?): Pair<Long, Long> {
        // Calculate when this line should start scrolling
        val startScrollTime =
                when (mCurrentLineBeatInfo.mScrollMode) {
                    // Smooth mode? Start scrolling instantly.
                    ScrollingMode.Smooth -> mSongTime
                    else ->
                        // Pause line? Start scrolling after 95% of the pause has elapsed.
                        if (pauseTag != null)
                            lineStartTime + (pauseTag.mDuration * 0.95).toLong()
                        // Beat line? Start scrolling on the last beat.
                        else
                            currentBeatEvents!!.mEvents.lastOrNull()?.mEventTime ?: mSongTime
                    // (Manual mode ignores these scroll values)
                }
        // Calculate when the line should stop scrolling
        val stopScrollTime =
                when (mCurrentLineBeatInfo.mScrollMode) {
                    // Smooth mode? It should stop scrolling once the allocated time has elapsed.
                    ScrollingMode.Smooth -> mSongTime + lineDuration
                    else ->
                        // Pause line? It should stop scrolling when the pause has ran out
                        if (pauseTag != null)
                            lineStartTime + pauseTag.mDuration
                        // Beat line? It should stop scrolling after the final beat
                        else
                            currentBeatEvents!!.mBlockEndTime
                    // (Manual mode ignores these values)
                }

        return Pair(startScrollTime, stopScrollTime)
    }

    private fun calculateLineDuration(pauseTag: PauseTag?, addToPause: Long, lineStartTime: Long, currentBeatEvents: EventBlock?): Long {
        // Calculate how long this line will last for.
        return when {
            // Pause line? Lasts as long as the pause!
            pauseTag != null -> pauseTag.mDuration + addToPause
            // Smooth line? We've counted the bars, so do the sums.
            mCurrentLineBeatInfo.mScrollMode == ScrollingMode.Smooth && mTimePerBar > 0 -> mTimePerBar * mCurrentLineBeatInfo.mBPL
            // Beat line? The result of generateBeatEvents will contain the time
            // that the beats end, so subtract the start time from that to get our duration.
            else -> currentBeatEvents!!.mBlockEndTime - lineStartTime
            // (Manual mode ignores these scroll values)
        }
    }

    private fun sortEvents(eventList: List<BaseEvent>): List<BaseEvent> {
        // Sort all events by time, and by type within that.
        return eventList.sortedWith(Comparator { e1, e2 ->
            when {
                e1.mEventTime > e2.mEventTime -> 1
                e1.mEventTime < e2.mEventTime -> -1
                else -> {
                    // MIDI events are most important. We want to
                    // these first at any given time for maximum MIDI
                    // responsiveness
                    if (e1 is MIDIEvent && e2 is MIDIEvent)
                        0
                    else if (e1 is MIDIEvent)
                        -1
                    else if (e2 is MIDIEvent)
                        1
                    // AudioEvents are next-most important. We want to process
                    // these first at any given time for maximum audio
                    // responsiveness
                    else if (e1 is AudioEvent && e2 is AudioEvent)
                        0
                    else if (e1 is AudioEvent)
                        -1
                    else if (e2 is AudioEvent)
                        1
                    // Now LineEvents for maximum visual responsiveness
                    else if (e1 is LineEvent && e2 is LineEvent)
                        0
                    else if (e1 is LineEvent)
                        -1
                    else if (e2 is LineEvent)
                        1
                    // Remaining order doesn't really matter
                    else
                        0
                }
            }
        })
    }

    /**
     * Constructs a linked-list of events from the unlinked event list.
     */
    private fun buildLinkedEventList(eventList: List<BaseEvent>): LinkedEvent {
        // Now build the linked list.
        var prevEvent: LinkedEvent? = null
        val linkedEvents = eventList.map {
            val newEvent = LinkedEvent(it, prevEvent)
            prevEvent = newEvent
            newEvent
        }

        // Since we can't see the future, we now have to traverse the list backwards
        // setting the mNext... fields.
        setNextEvents(linkedEvents.last())

        return linkedEvents.first()
    }

    private fun setNextEvents(finalEvent: LinkedEvent) {
        var nextEvent: LinkedEvent? = null
        var nextBeatEvent: BeatEvent? = null
        var lastEvent: LinkedEvent? = finalEvent
        while (lastEvent != null) {
            lastEvent.mNextEvent = nextEvent
            lastEvent.mNextBeatEvent = nextBeatEvent
            if (lastEvent.mEvent is BeatEvent)
                nextBeatEvent = lastEvent.mEvent as BeatEvent
            nextEvent = lastEvent
            lastEvent = lastEvent.mPrevEvent
        }
    }

    /**
     * An "event block" is simply a list of events, in chronological order, and a time that marks the point
     * at which the block ends. Note that the end time is not necessarily the same as the time of the last
     * event. For example, a block of five beat events (where each beat last n nanoseconds) will contain
     * five events with the times of n*0, n*1, n*2, n*3, n*4, and the end time will be n*5, as a "beat event"
     * actually covers the duration of the beat.
     */
    data class EventBlock(val mEvents: List<BaseEvent>, val mBlockEndTime: Long)

    internal class LineList : ArrayList<Line>() {
        override fun add(element: Line): Boolean {
            val lastOrNull = lastOrNull()
            lastOrNull?.mNextLine = element
            element.mPrevLine = lastOrNull
            return super.add(element)
        }
    }

    internal class CircularGraphicsList : ArrayList<LineGraphic>() {
        override fun add(element: LineGraphic): Boolean {
            lastOrNull()?.mNextGraphic = element
            val result = super.add(element)
            last().mNextGraphic = first()
            return result
        }
    }

    companion object {
        private const val DEMO_LINE_COUNT = 15
    }
}