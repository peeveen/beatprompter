package com.stevenfrew.beatprompter.cache

import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.util.Log
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import com.stevenfrew.beatprompter.event.*
import com.stevenfrew.beatprompter.midi.*
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.songload.SongLoadInfo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.util.*

class SongFile : CachedCloudFile {

    lateinit var mTitle: String
    private var mTimePerLine: Long = 0
    private var mTimePerBar: Long = 0
    var mBPM = 0.0
    var mKey: String = ""
    var mLines = 0
    var mMixedMode = false
    var mArtist: String? = null

    val sortableArtist
        get()= sortableString(mArtist)

    val sortableTitle
        get()= sortableString(mTitle)

    private var mSongSelectTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
    private var mProgramChangeTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER

    var mTags = HashSet<String>()
    var mAudioFiles = ArrayList<String>()
    var mImageFiles = ArrayList<String>()

    val isSmoothScrollable: Boolean
        get() = mTimePerLine > 0

    val isBeatScrollable: Boolean
        get() = mBPM > 0

    @Throws(InvalidBeatPrompterFileException::class)
    constructor(result: SuccessfulCloudDownloadResult) : super(result.mDownloadedFile, result.mCloudFileInfo) {
        parseSongFileInfo()
    }

    @Throws(IOException::class)
    constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String) : super(file, id, name, lastModified, subfolder) {
        parseSongFileInfo()
    }

    constructor(element: Element) : super(element) {
        mTitle = element.getAttribute(SONG_FILE_TITLE_ATTRIBUTE_NAME)
        mArtist = element.getAttribute(SONG_FILE_ARTIST_ATTRIBUTE_NAME)
        val bpmString = element.getAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME)
        val lineString = element.getAttribute(SONG_FILE_LINECOUNT_ATTRIBUTE_NAME)
        var mixedModeString: String? = element.getAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME)
        val keyString = element.getAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME)
        if (keyString != null)
            mKey = keyString
        if (mixedModeString == null)
            mixedModeString = "false"
        mLines = Integer.parseInt(lineString)
        mBPM = bpmString.toDouble()
        val timePerLineString = element.getAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME)
        mMixedMode = mixedModeString.toBoolean()
        mTimePerLine = timePerLineString.toLong()
        var timePerBarString: String? = element.getAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME)
        if (timePerBarString == null || timePerBarString.isEmpty())
            timePerBarString = "0"
        mTimePerBar = timePerBarString.toLong()
        val tagNodes = element.getElementsByTagName(TAG_ELEMENT_TAG_NAME)
        mTags = HashSet()
        for (f in 0 until tagNodes.length)
            mTags.add(tagNodes.item(f).textContent)
        val audioNodes = element.getElementsByTagName(AUDIO_FILE_ELEMENT_TAG_NAME)
        mAudioFiles = ArrayList()
        for (f in 0 until audioNodes.length)
            mAudioFiles.add(audioNodes.item(f).textContent)
        val imageNodes = element.getElementsByTagName(IMAGE_FILE_ELEMENT_TAG_NAME)
        mImageFiles = ArrayList()
        for (f in 0 until imageNodes.length)
            mImageFiles.add(imageNodes.item(f).textContent)
        val pcTriggerNodes = element.getElementsByTagName(PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME)
        mProgramChangeTrigger = SongTrigger.DEAD_TRIGGER
        for (f in 0 until pcTriggerNodes.length)
            mProgramChangeTrigger = SongTrigger.readFromXMLElement(pcTriggerNodes.item(f) as Element)
        val ssTriggerNodes = element.getElementsByTagName(SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME)
        mSongSelectTrigger = SongTrigger.DEAD_TRIGGER
        for (f in 0 until ssTriggerNodes.length)
            mSongSelectTrigger = SongTrigger.readFromXMLElement(ssTriggerNodes.item(f) as Element)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun parseSongFileInfo() {
        var br: BufferedReader? = null
        try {
            val (timePerLine, timePerBar) = getTimePerLineAndBar(null)
            var title:String?=null
            mTimePerLine = timePerLine
            mTimePerBar = timePerBar
            val parsingState= SongParsingState(ScrollingMode.Beat,this)
            br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
            var line: String?
            var lineNumber = 0
            do {
                line = br.readLine()
                if(line!=null)
                {
                    val fileLine= SongFileLine(line, ++lineNumber,parsingState)
                    if(title==null)
                        title = fileLine.getTitle()
                    val artist = fileLine.getArtist()
                    val key = fileLine.getKey()
                    val firstChord = fileLine.getFirstChord()
                    if ((mKey.isBlank()) && firstChord != null && firstChord.isNotEmpty())
                        mKey = firstChord
                    val msst = fileLine.getMIDISongSelectTrigger()
                    val mpct = fileLine.getMIDIProgramChangeTrigger()
                    if (msst != null)
                        mSongSelectTrigger = msst
                    if (mpct != null)
                        mProgramChangeTrigger = mpct
                    if (key != null)
                        mKey = key
                    if (artist != null)
                        mArtist = artist
                    val bpm = fileLine.getBPM()
                    if (bpm != null && mBPM == 0.0) {
                        try {
                            mBPM = bpm.toDouble()
                        } catch (e: Exception) {
                            Log.e(BeatPrompterApplication.TAG, "Failed to parse BPM value from song file.", e)
                        }

                    }

                    // TODO: better implementation of this.
                    //mMixedMode = mMixedMode or fileLine.containsToken("beatstart")
                    mMixedMode=false

                    val tags = fileLine.getTags()
                    mTags.addAll(tags)
                    val audios = fileLine.getAudioFiles()
                    mAudioFiles.addAll(audios.map{it.mName})
                    val images = fileLine.getImageFiles()
                    mImageFiles.addAll(images.map{it.mName})
                }
            } while(line!=null)
            mLines = lineNumber
            if (title == null || title.isEmpty())
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.noTitleFound, mName))
            mTitle=title
            if (mArtist == null)
                mArtist = ""
        } catch (ioe: IOException) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.file_io_read_error), ioe)
        } finally {
            try {
                br?.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close song file.", ioe)
            }
        }
    }

    override fun writeToXML(d: Document, element: Element) {
        val songElement = d.createElement(SONGFILE_ELEMENT_TAG_NAME)
        super.writeToXML(songElement)
        songElement.setAttribute(SONG_FILE_TITLE_ATTRIBUTE_NAME, mTitle)
        songElement.setAttribute(SONG_FILE_ARTIST_ATTRIBUTE_NAME, mArtist)
        songElement.setAttribute(SONG_FILE_LINECOUNT_ATTRIBUTE_NAME, Integer.toString(mLines))
        songElement.setAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME, mMixedMode.toString())
        songElement.setAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME, mBPM.toString())
        songElement.setAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME, mKey)
        songElement.setAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME, mTimePerLine.toString())
        songElement.setAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME, mTimePerBar.toString())
        for (tag in mTags) {
            val tagElement = d.createElement(TAG_ELEMENT_TAG_NAME)
            tagElement.textContent = tag
            songElement.appendChild(tagElement)
        }
        for (audioFile in mAudioFiles) {
            val audioFileElement = d.createElement(AUDIO_FILE_ELEMENT_TAG_NAME)
            audioFileElement.textContent = audioFile
            songElement.appendChild(audioFileElement)
        }
        for (imageFile in mImageFiles) {
            val imageFileElement = d.createElement(IMAGE_FILE_ELEMENT_TAG_NAME)
            imageFileElement.textContent = imageFile
            songElement.appendChild(imageFileElement)
        }
        mProgramChangeTrigger.writeToXML(d, songElement, PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME)
        mSongSelectTrigger.writeToXML(d, songElement, SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME)
        element.appendChild(songElement)
    }

    fun matchesTrigger(trigger: SongTrigger): Boolean {
        return mSongSelectTrigger == trigger || mProgramChangeTrigger == trigger
    }

    @Throws(IOException::class)
    fun getTimePerLineAndBar(chosenTrack: AudioFile?): SmoothScrollingTimings {
        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
        try {
            var songTime: Long = 0
            var songMilli = 0
            var pauseTime: Long = 0
            var realLineCount = 0
            var realBarCount = 0
            val parsingState=SongParsingState(ScrollingMode.Beat,this)
            var totalPauseTime: Long = 0//defaultPausePref*1000;
            var line: String?
            var lineImage: ImageFile? = null
            var lineNumber = 0
            val errors = ArrayList<FileParseError>()
            do {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= SongFileLine(line, ++lineNumber,parsingState)
                    // Ignore comments.
                    if (!fileLine.isComment) {
                        val strippedLine = fileLine.mTaglessLine
                        val chordsFound = fileLine.chordTags.isNotEmpty()
                        val bars=fileLine.mBeatInfo.mBPL
                        // Not bothered about chords at the moment.
                        fileLine.mTags.filterNot { it is ChordTag }.forEach{
                            if(it is TimeTag)
                                songTime=it.mDuration
                            else if(it is PauseTag)
                                pauseTime = it.mDuration
                            else if(it is ImageTag) {
                                if (lineImage != null)
                                    errors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                                else
                                    lineImage=it.mImageFile
                            }
                            else if(it is TrackTag) {
                                if (songMilli == 0 && (chosenTrack == null || it.mAudioFile == chosenTrack)) {
                                    try {
                                        val mmr = MediaMetadataRetriever()
                                        mmr.setDataSource(it.mAudioFile.mFile.absolutePath)
                                        val data = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                        if (data != null)
                                            songMilli = Integer.parseInt(data)
                                    } catch (e: Exception) {
                                        Log.e(BeatPrompterApplication.TAG, "Failed to extract duration metadata from media file.", e)
                                    }
                                }
                            }
                        }

                        // Contains only tags? Or contains nothing? Don't use it as a blank line.
                        if (strippedLine.trim().isNotEmpty() || chordsFound || pauseTime > 0 || lineImage != null) {
                            // removed lineImage=null from here
                            totalPauseTime += pauseTime
                            pauseTime = 0
                            // Line could potentially have been "{sometag} # comment"?
                            if (lineImage != null || chordsFound || !strippedLine.trim().startsWith("#")) {
                                realBarCount += bars
                                realLineCount++
                            }
                        }
                    }
                    ++lineNumber
                }
            } while(line!=null)

            if (songTime == Utils.TRACK_AUDIO_LENGTH_VALUE)
                songTime = songMilli.toLong()

            var negateResult = false
            if (totalPauseTime > songTime) {
                negateResult = true
                totalPauseTime = 0
            }
            var lineresult = (Utils.milliToNano(songTime - totalPauseTime).toDouble() / realLineCount.toDouble()).toLong()
            var barresult = (Utils.milliToNano(songTime - totalPauseTime).toDouble() / realBarCount.toDouble()).toLong()
            val trackresult = Utils.milliToNano(songMilli)
            if (negateResult) {
                lineresult = -lineresult
                barresult = -barresult
            }
            return SmoothScrollingTimings(lineresult, barresult, trackresult)
        } finally {
            try {
                br.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close song file.", ioe)
            }

        }
    }

    @Throws(IOException::class)
    fun parse(loadingSongFile: SongLoadInfo, cancelEvent: CancelEvent, songLoadHandler: Handler, registered: Boolean): Song {
        val parsingState= SongParsingState(loadingSongFile.scrollMode,this)
        val initialMIDIMessages = mutableListOf<OutgoingMessage>()
        var stopAddingStartupItems = false

        val chosenTrack = loadingSongFile.track
        val sst = getTimePerLineAndBar(chosenTrack)
        val timePerLine = sst.timePerLine
        val timePerBar = sst.timePerBar

        if (timePerLine < 0 || timePerBar < 0) {
            parsingState.mErrors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.pauseLongerThanSong)))
            sst.timePerLine = -timePerLine
            sst.timePerBar = -timePerBar
        }

        val sharedPrefs=BeatPrompterApplication.preferences
        val ignoreColorInfo = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue).toBoolean())
        var sendMidiClock = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        val countInPref = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        val metronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPrefs)
        var backgroundColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
        var pulseColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_default)))
        var beatCounterColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)))
        val scrollMarkerColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_default)))
        var lyricColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_default)))
        var chordColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_default)))
        val annotationColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_default)))
        val customCommentsUser = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))
        val showChords = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        val triggerContext = TriggerOutputContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        val defaultMIDIOutputChannelPrefValue = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        val defaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)

        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
        try {
            var line: String?=""

            val tagsSet = HashSet<String>()

            // ONE SHOT
            var chosenAudioFile: AudioFile? = null
            var chosenAudioVolume = 100
            var count = countInPref
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

            var metronomeOn = metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.On
            if (metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.OnWhenNoTrack && chosenTrack!=null)
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
            songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mLines).sendToTarget()
            while (line!=null && !cancelEvent.isCancelled) {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= SongFileLine(line, ++lineCounter, parsingState)
                    pauseTime = 0
                    // Ignore comments.
                    if(fileLine.isComment)
                        continue

                    fileLine.mTags.filterNot{it is ChordTag }.forEach {
                        if (it.isColorTag && !ignoreColorInfo)
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
                            sendMidiClock=true
                        else if(it is CountTag)
                            count=it.mCount
                        else if(it is BackgroundColorTag)
                            backgroundColour=it.mColor
                        else if(it is PulseColorTag)
                            pulseColour=it.mColor
                        else if(it is LyricsColorTag)
                            lyricColour=it.mColor
                        else if(it is ChordsColorTag)
                            chordColour=it.mColor
                        else if(it is BeatCounterColorTag)
                            beatCounterColour=it.mColor
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
                                if (comment.isIntendedFor(customCommentsUser))
                                    comments.add(comment)
                        }
                        else if(it is PauseTag)
                            pauseTime=it.mDuration
                        else if(it is MIDIEventTag)
                        {
                            if (displayLineCounter < DEMO_LINE_COUNT || registered)
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
                    val chordsFound = showChords && !chordTags.isEmpty()
                    val chordsFoundButNotShowingThem=!showChords && chordsFound
                    val tagsToProcess=if (chordsFoundButNotShowingThem) nonChordTags else fileLine.mTags
                    val createLine= (fileLine.mTaglessLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || lineImage != null)

                    // Contains only tags? Or contains nothing? Don't use it as a blank line.
                    if (createLine || pauseTime > 0) {
                        // We definitely have a line event!
                        // Deal with style/time/comment events now.
                        if (createColorEvent) {
                            val styleEvent = ColorEvent(currentTime, backgroundColour, pulseColour, lyricColour, chordColour, annotationColour, beatCounterColour, scrollMarkerColour)
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
                            if (displayLineCounter > DEMO_LINE_COUNT && !registered) {
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
                            if (totalLineTime == 0L || parsingState.mBeatInfo.mScrollingMode === ScrollingMode.Smooth)
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
                            } else if (bpmThisLine > 0 && parsingState.mBeatInfo.mScrollingMode !== com.stevenfrew.beatprompter.ScrollingMode.Smooth) {
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
                                        lineEvent.mLine.mYStartScrollTime = if (parsingState.mBeatInfo.mScrollingMode === ScrollingMode.Smooth) lineEvent.mEventTime else currentTime
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
                songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_READ, lineCounter, mLines).sendToTarget()
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
                            val countEvent = BeatEvent(countTime, countbpm, countbpb, countbpl, g, metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.DuringCountIn || metronomeOn, if (f == count - 1) countbpb - 1 else -1)
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
                firstEvent = ColorEvent(currentTime, backgroundColour, pulseColour, lyricColour, chordColour, annotationColour, beatCounterColour, scrollMarkerColour)

            val reallyTheLastEvent = firstEvent!!.lastEvent
            // In beat mode, or in any other mode where we're using a backing track, let's have an end event.
            if (trackEvent != null || parsingState.mBeatInfo.mScrollingMode !== com.stevenfrew.beatprompter.ScrollingMode.Manual) {
                var trackEndTime: Long = 0
                if (trackEvent != null)
                    trackEndTime = trackEvent.mEventTime + sst.trackLength
                // The end event will be where the final beat occurs.
                // But there is a chance that the audio track is longer than that.
                val endEvent = EndEvent(Math.max(currentTime, trackEndTime))
                reallyTheLastEvent.add(endEvent)
            }

            if (triggerContext === com.stevenfrew.beatprompter.midi.TriggerOutputContext.Always || triggerContext === com.stevenfrew.beatprompter.midi.TriggerOutputContext.ManualStartOnly && !loadingSongFile.startedByMIDITrigger) {
                if (mProgramChangeTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mProgramChangeTrigger.getMIDIMessages(defaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }

                if (mSongSelectTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mSongSelectTrigger.getMIDIMessages(defaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }
            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(firstEvent, parsingState.mErrors)

            val song = Song(this, chosenAudioFile, chosenAudioVolume, comments, firstEvent!!, firstLine!!, parsingState.mErrors, loadingSongFile.scrollMode, sendMidiClock, loadingSongFile.startedByBandLeader, loadingSongFile.nextSong, loadingSongFile.sourceDisplaySettings.mOrientation, initialMIDIMessages, beatBlocks, firstLine.mBeatInfo.mBPB, count)
            song.doMeasurements(Paint(), cancelEvent, songLoadHandler, loadingSongFile.nativeDisplaySettings, loadingSongFile.sourceDisplaySettings)
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

        private var thePrefix=BeatPrompterApplication.getResourceString(R.string.lowerCaseThe)+" "

        private const val SONG_FILE_TITLE_ATTRIBUTE_NAME = "title"
        private const val SONG_FILE_ARTIST_ATTRIBUTE_NAME = "artist"
        private const val SONG_FILE_LINECOUNT_ATTRIBUTE_NAME = "lines"
        private const val SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME = "mixedMode"
        private const val SONG_FILE_BPM_ATTRIBUTE_NAME = "bpm"
        private const val SONG_FILE_KEY_ATTRIBUTE_NAME = "key"
        private const val SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME = "timePerLine"
        private const val SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME = "timePerBar"
        private const val TAG_ELEMENT_TAG_NAME = "tag"

        const val SONGFILE_ELEMENT_TAG_NAME = "song"
        private const val AUDIO_FILE_ELEMENT_TAG_NAME = "audio"
        private const val IMAGE_FILE_ELEMENT_TAG_NAME = "image"

        private const val PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME = "programChangeTrigger"
        private const val SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME = "songSelectTrigger"

        fun sortableString(inStr:String?):String
        {
                val str=inStr?.toLowerCase()
                if(str!=null)
                {
                    return if(str.startsWith(thePrefix))
                        str.substring(thePrefix.length)
                    else
                        str
                }
                return ""
        }
    }
}