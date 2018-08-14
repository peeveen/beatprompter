package com.stevenfrew.beatprompter

import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.util.Log
import com.stevenfrew.beatprompter.cache.*
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.*
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
        mTriggerContext = TriggerOutputContext.valueOf(sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue)))
        mCountInPref = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        mCountInPref += Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_offset))
        /*            int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
            defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        mDefaultTrackVolume = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_default)))
        mDefaultTrackVolume += Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_offset))
        val defaultMIDIOutputChannelPrefValue = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
        mShowChords = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue)))
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
        mIgnoreColorInfo = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), java.lang.Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue)))
        mMetronomeContext = try {
            MetronomeContext.valueOf(sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_metronome_key), BeatPrompterApplication.getResourceString(R.string.pref_metronome_defaultValue)))
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            MetronomeContext.Off
        }

    }

    @Throws(IOException::class)
    fun parse(): Song {
        val scrollBeatMin = 1
        var scrollBeatDefault = 4
        val initialMIDIMessages = mutableListOf<OutgoingMessage>()
        val errors = mutableListOf<FileParseError>()
        var stopAddingStartupItems = false

        val chosenTrack = mLoadingSongFile.track
        val sst = mSongFile.getTimePerLineAndBar(chosenTrack)
        val timePerLine = sst.timePerLine
        val timePerBar = sst.timePerBar

        if (timePerLine < 0 || timePerBar < 0) {
            errors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.pauseLongerThanSong)))
            sst.timePerLine = -timePerLine
            sst.timePerBar = -timePerBar
        }

        val br = BufferedReader(InputStreamReader(FileInputStream(mSongFile.mFile)))
        try {
            var line: String?=""

            var tagsOut = mutableListOf<Tag>()
            val tagsSet = HashSet<String>()

            // ONE SHOT
            var chosenAudioFile: AudioFile? = null
            var chosenAudioVolume = 100
            var count = mCountInPref
            var trackOffset: Long = 0
            // TIME
            var bpm = if (mSongFile.mBPM == 0.0) 120.0 else mSongFile.mBPM
            var bpb = 4
            var initialBPB = 4
            var initialBPBSet = false
            var bpl = 1
            var beatsToAdjust = 0
            val rolloverBeats = mutableListOf<BeatEvent>()
            var pauseTime: Int
            var scrollBeat = bpb
            var lastBeatBlock: BeatBlock? = null
            // COMMENT
            val comments = mutableListOf<Comment>()
            val beatBlocks = mutableListOf<BeatBlock>()
            var lineImage: ImageFile? = null

            var metronomeOn = mMetronomeContext === MetronomeContext.On
            if (mMetronomeContext === MetronomeContext.OnWhenNoTrack && chosenTrack.isEmpty())
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
                    var beatStartOrStopFoundOnThisLine = false
                    line = line.trim()
                    pauseTime = 0
                    lineCounter++
                    // Ignore comments.
                    if (!line.startsWith("#")) {
                        if (line.length > MAX_LINE_LENGTH) {
                            line = line.substring(0, MAX_LINE_LENGTH)
                            errors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.lineTooLong, lineCounter, MAX_LINE_LENGTH)))
                        }
                        tagsOut.clear()
                        var strippedLine = Tag.extractTags(line, lineCounter, tagsOut)
                        // Replace stupid unicode BOM character
                        strippedLine = strippedLine.replace("\uFEFF", "")
                        var chordsFound = false
                        var scrollbeatOffset = 0
                        for (tag in tagsOut) {
                            // Not bothered about chords at the moment.
                            if (tag.mChordTag) {
                                chordsFound = true
                                continue
                            }

                            if (Tag.colorTags.contains(tag.mName) && !mIgnoreColorInfo)
                                createColorEvent = true
                            if (Tag.oneShotTags.contains(tag.mName) && tagsSet.contains(tag.mName))
                                errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.oneShotTagDefinedTwice, tag.mName)))

                            val commentAudience=tag.parsePotentialCommentTag()

                            when (tag.mName) {
                                "image" -> {
                                    if (lineImage != null) {
                                        errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                                    }
                                    else {
                                        var imageName = tag.mValue
                                        val colonindex = imageName.indexOf(":")
                                        imageScalingMode = ImageScalingMode.Stretch
                                        if (colonindex != -1 && colonindex < imageName.length - 1) {
                                            val strScalingMode = imageName.substring(colonindex + 1)
                                            imageName = imageName.substring(0, colonindex)
                                            when {
                                                strScalingMode.equals("stretch", ignoreCase = true) -> imageScalingMode = ImageScalingMode.Stretch
                                                strScalingMode.equals("original", ignoreCase = true) -> imageScalingMode = ImageScalingMode.Original
                                                else -> errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.unknown_image_scaling_mode)))
                                            }
                                        }
                                        val image = File(imageName).name
                                        val imageFile: File
                                        val mappedImage = SongList.mCachedCloudFiles.getMappedImageFilename(image)
                                        if (mappedImage == null)
                                            errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image)))
                                        else {
                                            imageFile = File(mSongFile.mFile.parent, mappedImage.mFile.name)
                                            if (!imageFile.exists())
                                                errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image)))
                                        }
                                        lineImage = mappedImage
                                    }
                                }
                                "track", "audio", "musicpath" -> {
                                    var trackName = tag.mValue
                                    var volume = mDefaultTrackVolume
                                    val trackcolonindex = trackName.indexOf(":")
                                    if (trackcolonindex != -1 && trackcolonindex < trackName.length - 1) {
                                        val strVolume = trackName.substring(trackcolonindex + 1)
                                        trackName = trackName.substring(0, trackcolonindex)
                                        try {
                                            val tryvolume = Integer.parseInt(strVolume)
                                            if (tryvolume < 0 || tryvolume > 100)
                                                errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.badAudioVolume)))
                                            else
                                                volume = (volume.toDouble() * (tryvolume.toDouble() / 100.0)).toInt()
                                        } catch (nfe: NumberFormatException) {
                                            errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.badAudioVolume)))
                                        }
                                    }
                                    val track = File(trackName).name
                                    var trackFile: File? = null
                                    val mappedTrack = SongList.mCachedCloudFiles.getMappedAudioFilename(track)
                                    if (mappedTrack == null)
                                        errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track)))
                                    else {
                                        trackFile = File(mSongFile.mFile.parent, mappedTrack.mFile.name)
                                        if (!trackFile.exists()) {
                                            errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track)))
                                            trackFile = null
                                        }
                                    }
                                    if (trackFile != null && track.equals(chosenTrack, ignoreCase = true)) {
                                        chosenAudioFile = mappedTrack
                                        chosenAudioVolume = volume
                                    }
                                }
                                "send_midi_clock" -> mSendMidiClock = true
                                "count", "countin" -> count = Tag.getIntegerValueFromTag(tag, mCountInMin, mCountInMax, mCountInDefault, errors)
                                //                            case "trackoffset":
                                //                                trackOffset=Tag.getLongValueFromTag(tag, trackOffsetMin, trackOffsetMax, trackOffsetDefault, errors);
                                //                                break;
                                "backgroundcolour", "backgroundcolor", "bgcolour", "bgcolor" -> mBackgroundColour = Tag.getColourValueFromTag(tag, mBackgroundColour, errors)
                                "pulsecolour", "pulsecolor", "beatcolour", "beatcolor" -> mPulseColour = Tag.getColourValueFromTag(tag, mPulseColour, errors)
                                "lyriccolour", "lyriccolor", "lyricscolour", "lyricscolor" -> mLyricColour = Tag.getColourValueFromTag(tag, mLyricColour, errors)
                                "chordcolour", "chordcolor" -> mChordColour = Tag.getColourValueFromTag(tag, mChordColour, errors)
                                "beatcountercolour", "beatcountercolor" -> mBeatCounterColour = Tag.getColourValueFromTag(tag, mBeatCounterColour, errors)
                                "bpm", "metronome", "beatsperminute" -> bpm = Tag.getDoubleValueFromTag(tag, mBPMMin.toDouble(), mBPMMax.toDouble(), mBPMDefault.toDouble(), errors)
                                "bpb", "beatsperbar" -> {
                                    val prevScrollBeatDiff = bpb - scrollBeat
                                    bpb = Tag.getIntegerValueFromTag(tag, mBPBMin, mBPBMax, mBPBDefault, errors)
                                    if (!initialBPBSet) {
                                        initialBPB = bpb
                                        initialBPBSet = true
                                    }
                                    scrollBeatDefault = bpb
                                    if (bpb - prevScrollBeatDiff > 0)
                                        scrollBeat = bpb - prevScrollBeatDiff
                                    if (scrollBeat > bpb)
                                        scrollBeat = bpb
                                }
                                "bpl", "barsperline" -> bpl = Tag.getIntegerValueFromTag(tag, mBPLMin, mBPLMax, mBPLDefault, errors)
                                "scrollbeat", "sb" -> {
                                    scrollBeat = Tag.getIntegerValueFromTag(tag, scrollBeatMin, bpb, scrollBeatDefault, errors)
                                    if (scrollBeat > bpb)
                                        scrollBeat = bpb
                                }
                                "comment" -> {
                                    val comment = Comment(tag.mValue,commentAudience)
                                    if (stopAddingStartupItems) {
                                        val ce = CommentEvent(currentTime, comment)
                                        if (firstEvent == null)
                                            firstEvent = ce
                                        else
                                            lastEvent!!.add(ce)
                                        lastEvent = ce
                                    } else {
                                        if (comment.isIntendedFor(mCustomCommentsUser))
                                            comments.add(comment)
                                    }
                                }
                                "pause" -> pauseTime = Tag.getDurationValueFromTag(tag, 1000, 60 * 60 * 1000, 0, false, errors)
                                "midi_song_select_trigger", "midi_program_change_trigger" ->
                                    // Don't need the value after the song is loaded, we're just showing informational
                                    // errors about bad formatting.
                                    Tag.verifySongTriggerFromTag(tag, errors)
                                "time", "tag", "bars", "soh", "eoh", "b", "title", "t", "artist", "a", "subtitle", "st", "key" -> {
                                }
                                "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype" -> {
                                }
                                "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> {
                                }
                                "beatstart" -> {
                                    if (beatStartOrStopFoundOnThisLine) {
                                        errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_beatstart_beatstop_same_line)))
                                    }
                                    else {
                                        beatStartOrStopFoundOnThisLine = true
                                        if (mCurrentScrollMode === ScrollingMode.Manual)
                                            mCurrentScrollMode = ScrollingMode.Beat
                                        else if (mSongFile.mBPM == 0.0)
                                            errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                                    }
                                }
                                "beatstop" -> {
                                    if (beatStartOrStopFoundOnThisLine) {
                                        errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_beatstart_beatstop_same_line)))
                                    }
                                    else {
                                        beatStartOrStopFoundOnThisLine = true
                                        if (mCurrentScrollMode === ScrollingMode.Beat) {
                                            mCurrentScrollMode = ScrollingMode.Manual
                                            currentTime += BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS
                                        }
                                    }
                                }
                                else -> try {
                                    if (displayLineCounter < DEMO_LINE_COUNT || mRegistered) {
                                        val me = Tag.getMIDIEventFromTag(currentTime, tag, SongList.midiAliases, mDefaultMIDIOutputChannel, errors)
                                        if (me != null) {
                                            if (stopAddingStartupItems) {
                                                if (firstEvent == null)
                                                    firstEvent = me
                                                else
                                                    lastEvent!!.add(me)
                                                lastEvent = me
                                            } else {
                                                initialMIDIMessages.addAll(me.mMessages)
                                                if (me.mOffset != null)
                                                    errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.midi_offset_before_first_line)))
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errors.add(FileParseError(tag, e.message))
                                }

                            }// Dealt with in pre-parsing.
                            // ChordPro stuff we're not supporting.
                            // SongBook stuff we're not supporting.
                            tagsSet.add(tag.mName)
                        }
                        var createLine = false
                        var allowBlankLine = false
                        if (!mShowChords && chordsFound) {
                            chordsFound = false
                            allowBlankLine = true
                            val noChordsTags = mutableListOf<Tag>()
                            for (tag in tagsOut)
                                if (!tag.mChordTag)
                                    noChordsTags.add(tag)
                            tagsOut = noChordsTags
                        }
                        if (strippedLine.trim().isNotEmpty() || allowBlankLine || chordsFound || lineImage != null)
                            createLine = true

                        // Contains only tags? Or contains nothing? Don't use it as a blank line.
                        if (createLine || pauseTime > 0) {
                            // We definitely have a line event!
                            // Deal with style/time/comment events now.
                            if (createColorEvent) {
                                val styleEvent = ColorEvent(currentTime, mBackgroundColour, mPulseColour, mLyricColour, mChordColour, mAnnotationColour, mBeatCounterColour, mScrollMarkerColour)
                                if (currentTime == 0L && firstEvent != null) {
                                    // First event should ALWAYS be a color event.
                                    val oldFirstEvent = firstEvent
                                    firstEvent = styleEvent
                                    firstEvent.add(oldFirstEvent)
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

                            var bars = 0
                            var commasFound = false
                            while (strippedLine.startsWith(",")) {
                                commasFound = true
                                strippedLine = strippedLine.substring(1)
                                for (tag in tagsOut)
                                    if (tag.mPosition > 0)
                                        tag.mPosition--
                                bars++
                            }
                            bars = Math.max(1, bars)

                            val bpbThisLine = bpb
                            while (strippedLine.endsWith(">") || strippedLine.endsWith("<")/*||(strippedLine.endsWith("+"))||(strippedLine.endsWith("_"))*/) {
                                if (strippedLine.endsWith(">"))
                                    scrollbeatOffset++
                                else if (strippedLine.endsWith("<"))
                                    scrollbeatOffset--
                                /*                            else if(strippedLine.endsWith("+")) {
                                scrollbeatOffset++;
                                bpbThisLine++;
                            }
                            else if(strippedLine.endsWith("_")) {
                                scrollbeatOffset--;
                                bpbThisLine--;
                            }*/
                                strippedLine = strippedLine.substring(0, strippedLine.length - 1)
                                for (tag in tagsOut)
                                    if (tag.mPosition > strippedLine.length)
                                        tag.mPosition--
                            }

                            if (scrollbeatOffset < -bpbThisLine || scrollbeatOffset >= bpbThisLine) {
                                errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
                                scrollbeatOffset = 0
                            }
                            if (!commasFound)
                                bars = bpl

                            if (lineImage != null && (strippedLine.trim().isNotEmpty() || chordsFound))
                                errors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.text_found_with_image)))

                            if (strippedLine.trim().isEmpty() && !chordsFound)
                                strippedLine = "▼"

                            //                        if((firstLine==null)&&(smoothScrolling))
                            //                            pauseTime+=defaultPausePref*1000;

                            if (createLine) {
                                displayLineCounter++
                                if (displayLineCounter > DEMO_LINE_COUNT && !mRegistered) {
                                    tagsOut = mutableListOf()
                                    strippedLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                                    lineImage = null
                                }
                                var lastLine: Line? = null
                                if (lastEvent.mPrevLineEvent != null)
                                    lastLine = lastEvent.mPrevLineEvent!!.mLine
                                var lastScrollbeatOffset = 0
                                var lastBPB = bpbThisLine
                                var lastScrollbeat = scrollBeatDefault
                                if (lastLine != null) {
                                    lastBPB = lastLine.mBPB
                                    lastScrollbeatOffset = lastLine.mScrollbeatOffset
                                    lastScrollbeat = lastLine.mScrollbeat
                                }
                                val scrollbeatDifference = scrollBeat - bpbThisLine - (lastScrollbeat - lastBPB)

                                for (tag in tagsOut)
                                    if (!tag.mChordTag)
                                        if (tag.mName == "b" || tag.mName == "bars")
                                            bars = Tag.getIntegerValueFromTag(tag, 1, 128, 1, errors)
                                bars = Math.max(1, bars)

                                var beatsForThisLine = bpbThisLine * bars
                                val simpleBeatsForThisLine = beatsForThisLine
                                beatsForThisLine += scrollbeatOffset
                                beatsForThisLine += scrollbeatDifference
                                beatsForThisLine -= lastScrollbeatOffset

                                nanosecondsPerBeat = if (bpm > 0)
                                    Utils.nanosecondsPerBeat(bpm)
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
                                    lineObj = ImageLine(currentTime, totalLineTime, lineImage, imageScalingMode, bars, lastEvent.mPrevColorEvent!!, bpbThisLine, scrollBeat, scrollbeatOffset, mCurrentScrollMode!!)
                                    lineImage = null
                                } else
                                    lineObj = TextLine(currentTime, totalLineTime, strippedLine, tagsOut, bars, lastEvent.mPrevColorEvent!!, bpbThisLine, scrollBeat, scrollbeatOffset, mCurrentScrollMode!!)

                                lastLine?.insertAfter(lineObj)
                                val lineEvent = lineObj.mLineEvent
                                if (firstLine == null)
                                    firstLine = lineObj
                                lastEvent.insertEvent(lineEvent)

                                // generate beats ...

                                // if a pause is specified on a line, it replaces the actual beats for that line.
                                if (pauseTime > 0) {
                                    currentTime = generatePause(pauseTime.toLong(), lastEvent, currentTime)
                                    lastEvent = lastEvent.lastEvent
                                    lineEvent.mLine.mYStartScrollTime = currentTime - nanosecondsPerBeat
                                    lineEvent.mLine.mYStopScrollTime = currentTime
                                } else if (bpm > 0 && mCurrentScrollMode !== ScrollingMode.Smooth) {
                                    var finished = false
                                    var beatThatWeWillScrollOn = 0
                                    val rolloverBeatCount = rolloverBeats.size
                                    val beatsToAdjustCount = beatsToAdjust
                                    if (beatsToAdjust > 0) {
                                        // We have N beats to adjust.
                                        // For the previous N beatevents, set the BPB to the new BPB.
                                        var lastBeatEvent = lastEvent.mPrevBeatEvent
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
                                            beatEvent = BeatEvent(currentTime, bpm, bpbThisLine, bpl, currentBeat, metronomeOn, beatThatWeWillScrollOn)
                                        else {
                                            beatEvent = rolloverBeats[0]
                                            beatEvent.mWillScrollOnBeat = beatThatWeWillScrollOn
                                            rolloverBPB = beatEvent.mBPB
                                            rolloverBeatLength = Utils.nanosecondsPerBeat(beatEvent.mBPM)
                                            rolloverBeats.removeAt(0)
                                        }
                                        lastEvent.insertEvent(beatEvent)
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
                                            rolloverBeats.add(BeatEvent(rolloverCurrentTime, bpm, bpbThisLine, bpl, rolloverCurrentBeat++, metronomeOn, beatThatWeWillScrollOn))
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
                                currentTime = generatePause(pauseTime.toLong(), lastEvent, currentTime)

                            lastEvent = lastEvent.lastEvent
                        }
                    }
                }
                mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_READ, lineCounter, mSongFile.mLines).sendToTarget()
            }

            var countTime: Long = 0
            // Create count events
            if (bpm > 0 && mCurrentScrollMode !== ScrollingMode.Manual) {
                val firstBeatEvent = firstEvent!!.firstBeatEvent
                val countbpm = firstBeatEvent?.mBPM ?: 120.0
                val countbpb = firstBeatEvent?.mBPB ?: 4
                val countbpl = firstBeatEvent?.mBPL ?: 1
                var insertAfterEvent = firstEvent
                if (count > 0) {
                    val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                    for (f in 0 until count)
                        for (g in 0 until countbpb) {
                            val countEvent = BeatEvent(countTime, countbpm, countbpb, countbpl, g, mMetronomeContext === MetronomeContext.DuringCountIn || metronomeOn, if (f == count - 1) bpb - 1 else -1)
                            insertAfterEvent!!.insertAfter(countEvent)
                            insertAfterEvent = countEvent
                            countTime += nanoPerBeat
                        }
                    insertAfterEvent!!.offsetLaterEvents(countTime)
                } else {
                    val baseBeatEvent = BeatEvent(0, countbpm, countbpb, countbpl, countbpb, false, -1)
                    firstEvent.insertAfter(baseBeatEvent)
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
            if (bpm > 0 && mCurrentScrollMode === ScrollingMode.Beat) {
                // Last Y scroll should never happen. No point scrolling last line offscreen.
                val mLastLine = firstLine!!.lastLine
                mLastLine.mYStopScrollTime = java.lang.Long.MAX_VALUE
                mLastLine.mYStartScrollTime = mLastLine.mYStopScrollTime
            }

            // Nothing at all in the song file? We at least want the colors set right.
            if (firstEvent == null)
                firstEvent = ColorEvent(currentTime, mBackgroundColour, mPulseColour, mLyricColour, mChordColour, mAnnotationColour, mBeatCounterColour, mScrollMarkerColour)

            val reallyTheLastEvent = firstEvent.lastEvent
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
                if (mSongFile.mProgramChangeTrigger != null)
                    if (mSongFile.mProgramChangeTrigger!!.isSendable())
                        try {
                            initialMIDIMessages.addAll(mSongFile.mProgramChangeTrigger!!.getMIDIMessages(mDefaultMIDIOutputChannel))
                        } catch (re: ResolutionException) {
                            errors.add(FileParseError(lineCounter, re.message))
                        }

                if (mSongFile.mSongSelectTrigger != null)
                    if (mSongFile.mSongSelectTrigger!!.isSendable())
                        try {
                            initialMIDIMessages.addAll(mSongFile.mSongSelectTrigger!!.getMIDIMessages(mDefaultMIDIOutputChannel))
                        } catch (re: ResolutionException) {
                            errors.add(FileParseError(lineCounter, re.message))
                        }

            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(firstEvent, errors)

            val song = Song(mSongFile, chosenAudioFile, chosenAudioVolume, comments, firstEvent, firstLine!!, errors, mUserChosenScrollMode, mSendMidiClock, mLoadingSongFile.startedByBandLeader, mLoadingSongFile.nextSong, mLoadingSongFile.sourceDisplaySettings.mOrientation, initialMIDIMessages, beatBlocks, initialBPB, count)
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
        private const val MAX_LINE_LENGTH = 256
        private const val DEMO_LINE_COUNT = 15
        // Every beatstart/beatstop block has events that are offset by this amount (one year).
        // If you left the app running for a year, it would eventually progress. WHO WOULD DO SUCH A THING?
        private val BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS = Utils.milliToNano(1000 * 60 * 24 * 365)

        private fun offsetMIDIEvents(firstEvent: BaseEvent?, errors: MutableList<FileParseError>) {
            var event = firstEvent
            while (event != null) {
                if (event is MIDIEvent) {
                    val midiEvent = event as MIDIEvent?
                    if (midiEvent!!.mOffset != null && midiEvent.mOffset!!.mAmount != 0) {
                        // OK, this event needs moved.
                        var newTime: Long = -1
                        if (midiEvent.mOffset!!.mOffsetType === EventOffsetType.Milliseconds) {
                            val offset = Utils.milliToNano(midiEvent.mOffset!!.mAmount)
                            newTime = midiEvent.mEventTime + offset
                        } else {
                            // Offset by beat count.
                            var beatCount = midiEvent.mOffset!!.mAmount
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
                            errors.add(FileParseError(midiEvent.mOffset!!.mSourceTag, BeatPrompterApplication.getResourceString(R.string.midi_offset_is_before_start_of_song)))
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
