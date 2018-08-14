package com.stevenfrew.beatprompter.cache

import android.media.MediaMetadataRetriever
import android.util.Log
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import com.stevenfrew.beatprompter.midi.SongTrigger
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.util.*

class SongFile : CachedCloudFile {

    lateinit var mTitle: String
    private var mTimePerLine: Long = 0
    private var mTimePerBar: Long = 0
    var mBPM = 0.0
    var mKey: String? = ""
    var mLines = 0
    var mMixedMode = false
    var mArtist: String? = null

    val sortableArtist
        get()= sortableString(mArtist)

    val sortableTitle
        get()= sortableString(mTitle)

    var mSongSelectTrigger: SongTrigger? = SongTrigger.DEAD_TRIGGER
    var mProgramChangeTrigger: SongTrigger? = SongTrigger.DEAD_TRIGGER

    var mTags = HashSet<String>()
    var mAudioFiles = ArrayList<String>()
    var mImageFiles = ArrayList<String>()

    val isSmoothScrollable: Boolean
        get() = mTimePerLine > 0

    val isBeatScrollable: Boolean
        get() = mBPM > 0

    @Throws(InvalidBeatPrompterFileException::class)
    constructor(result: SuccessfulCloudDownloadResult) : super(result.mDownloadedFile, result.mCloudFileInfo) {
        parseSongFileInfo(ArrayList(), ArrayList())
    }

    @Throws(IOException::class)
    constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String, tempAudioFileCollection: ArrayList<AudioFile>, tempImageFileCollection: ArrayList<ImageFile>) : super(file, id, name, lastModified, subfolder) {
        parseSongFileInfo(tempAudioFileCollection, tempImageFileCollection)
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
        mBPM = java.lang.Double.parseDouble(bpmString)
        val timePerLineString = element.getAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME)
        mMixedMode = java.lang.Boolean.parseBoolean(mixedModeString)
        mTimePerLine = java.lang.Long.parseLong(timePerLineString)
        var timePerBarString: String? = element.getAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME)
        if (timePerBarString == null || timePerBarString.isEmpty())
            timePerBarString = "0"
        mTimePerBar = java.lang.Long.parseLong(timePerBarString)
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

    private fun getTitleFromLine(line: String, lineNumber: Int): String? {
        return getTokenValue(line, lineNumber, "title", "t")
    }

    private fun getKeyFromLine(line: String, lineNumber: Int): String? {
        return getTokenValue(line, lineNumber, "key")
    }

    private fun getFirstChordFromLine(line: String, lineNumber: Int): String? {
        val tagsOut = ArrayList<Tag>()
        Tag.extractTags(line, lineNumber, tagsOut)
        for (t in tagsOut) {
            if (t.mChordTag)
                if (Utils.isChord(t.mName.trim { it <= ' ' }))
                    return t.mName.trim { it <= ' ' }
        }
        return null
    }

    private fun getArtistFromLine(line: String, lineNumber: Int): String? {
        return getTokenValue(line, lineNumber, "artist", "a", "subtitle", "st")
    }

    private fun getBPMFromLine(line: String, lineNumber: Int): String? {
        return getTokenValue(line, lineNumber, "bpm", "beatsperminute", "metronome")
    }

    private fun getTagsFromLine(line: String, lineNumber: Int): ArrayList<String> {
        return getTokenValues(line, lineNumber, "tag")
    }

    private fun getMIDISongSelectTriggerFromLine(line: String, lineNumber: Int): SongTrigger? {
        return getMIDITriggerFromLine(line, lineNumber, true)
    }

    private fun getMIDIProgramChangeTriggerFromLine(line: String, lineNumber: Int): SongTrigger? {
        return getMIDITriggerFromLine(line, lineNumber, false)
    }

    private fun getMIDITriggerFromLine(line: String, lineNumber: Int, songSelectTrigger: Boolean): SongTrigger? {
        val `val` = getTokenValue(line, lineNumber, if (songSelectTrigger) "midi_song_select_trigger" else "midi_program_change_trigger")
        if (`val` != null)
            try {
                return SongTrigger.parse(`val`, songSelectTrigger, lineNumber, ArrayList())
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Failed to parse MIDI song trigger from song file.", e)
            }

        return null
    }

    private fun getAudioFilesFromLine(line: String, lineNumber: Int): List<String> {
        val audio = ArrayList<String>()
        audio.addAll(getTokenValues(line, lineNumber, "audio"))
        audio.addAll(getTokenValues(line, lineNumber, "track"))
        audio.addAll(getTokenValues(line, lineNumber, "musicpath"))
        val realAudio = ArrayList<String>()
        for (str in audio) {
            var audioString=str
            val index = audioString.indexOf(":")
            if (index != -1 && index < audioString.length - 1)
                audioString = audioString.substring(0, index)
            realAudio.add(audioString)
        }
        return realAudio
    }

    private fun getImageFilesFromLine(line: String, lineNumber: Int): ArrayList<String> {
        val image = ArrayList(getTokenValues(line, lineNumber, "image"))
        val realimage = ArrayList<String>()
        for (str in image) {
            var imageString=str
            val index = imageString.indexOf(":")
            if (index != -1 && index < imageString.length - 1)
                imageString = imageString.substring(0, index)
            realimage.add(imageString)
        }
        return realimage
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun parseSongFileInfo(tempAudioFileCollection: ArrayList<AudioFile>, tempImageFileCollection: ArrayList<ImageFile>) {
        var br: BufferedReader? = null
        try {
            val (timePerLine, timePerBar) = getTimePerLineAndBar(null, tempAudioFileCollection, tempImageFileCollection)
            var title:String?=null
            mTimePerLine = timePerLine
            mTimePerBar = timePerBar
            br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
            var line: String?
            var lineNumber = 0
            do {
                line = br.readLine()
                if(line!=null) {
                    if(title!=null)
                        title = getTitleFromLine(line, lineNumber)
                    val artist = getArtistFromLine(line, lineNumber)
                    val key = getKeyFromLine(line, lineNumber)
                    val firstChord = getFirstChordFromLine(line, lineNumber)
                    if ((mKey == null || mKey!!.isEmpty()) && firstChord != null && firstChord.isNotEmpty())
                        mKey = firstChord
                    val msst = getMIDISongSelectTriggerFromLine(line, lineNumber)
                    val mpct = getMIDIProgramChangeTriggerFromLine(line, lineNumber)
                    if (msst != null)
                        mSongSelectTrigger = msst
                    if (mpct != null)
                        mProgramChangeTrigger = mpct
                    val bpm = getBPMFromLine(line, lineNumber)
                    if (key != null)
                        mKey = key
                    if (artist != null)
                        mArtist = artist
                    if (bpm != null && mBPM == 0.0) {
                        try {
                            mBPM = java.lang.Double.parseDouble(bpm)
                        } catch (e: Exception) {
                            Log.e(BeatPrompterApplication.TAG, "Failed to parse BPM value from song file.", e)
                        }

                    }
                    mMixedMode = mMixedMode or containsToken(line, lineNumber, "beatstart")
                    val tags = getTagsFromLine(line, lineNumber)
                    mTags.addAll(tags)
                    val audio = getAudioFilesFromLine(line, lineNumber)
                    mAudioFiles.addAll(audio)
                    val image = getImageFilesFromLine(line, lineNumber)
                    mImageFiles.addAll(image)
                    ++lineNumber
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
        songElement.setAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME, java.lang.Boolean.toString(mMixedMode))
        songElement.setAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME, java.lang.Double.toString(mBPM))
        songElement.setAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME, mKey)
        songElement.setAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME, java.lang.Long.toString(mTimePerLine))
        songElement.setAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME, java.lang.Long.toString(mTimePerBar))
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
        mProgramChangeTrigger!!.writeToXML(d, songElement, PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME)
        mSongSelectTrigger!!.writeToXML(d, songElement, SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME)
        element.appendChild(songElement)
    }

    fun matchesTrigger(trigger: SongTrigger): Boolean {
        return mSongSelectTrigger != null && mSongSelectTrigger == trigger || mProgramChangeTrigger != null && mProgramChangeTrigger == trigger
    }

    @Throws(IOException::class)
    fun getTimePerLineAndBar(chosenTrack: String?, tempAudioFileCollection: ArrayList<AudioFile>, tempImageFileCollection: ArrayList<ImageFile>): SmoothScrollingTimings {
        val bplOffset = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_offset))
        val bplMin = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_min)) + bplOffset
        val bplMax = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_max)) + bplOffset
        val bplDefault = Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_default)) + bplOffset

        //        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        /*        int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
        defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))

        try {
            var songTime: Long = 0
            var songMilli = 0
            var pauseTime: Long = 0
            var realLineCount = 0
            var realBarCount = 0
            var barsPerLine = bplDefault
            var totalPauseTime: Long = 0//defaultPausePref*1000;
            var line: String?
            var lineImage: ImageFile? = null
            var lineNumber = 0
            val errors = ArrayList<FileParseError>()
            val tagsOut = ArrayList<Tag>()
            do {
                line = br.readLine()
                if(line!=null) {
                    line = line.trim { it <= ' ' }
                    lineNumber++
                    // Ignore comments.
                    if (!line.startsWith("#")) {
                        tagsOut.clear()
                        var strippedLine = Tag.extractTags(line, lineNumber, tagsOut)
                        // Replace stupid unicode BOM character
                        strippedLine = strippedLine.replace("\uFEFF", "")
                        var chordsFound = false
                        var barsTag = 0
                        for (tag in tagsOut) {
                            // Not bothered about chords at the moment.
                            if (tag.mChordTag) {
                                chordsFound = true
                                continue
                            }

                            when (tag.mName) {
                                "time" -> songTime = Tag.getDurationValueFromTag(tag, 1000, 60 * 60 * 1000, 0, true, errors).toLong()
                                "pause" -> pauseTime = Tag.getDurationValueFromTag(tag, 1000, 60 * 60 * 1000, 0, false, errors).toLong()
                                "bars", "b" -> barsTag = Tag.getIntegerValueFromTag(tag, 1, 128, 1, errors)
                                "bpl", "barsperline" -> barsPerLine = Tag.getIntegerValueFromTag(tag, bplMin, bplMax, bplDefault, errors)
                                "image" -> {
                                    if (lineImage != null) {
                                        errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                                    }
                                    else {
                                        var imageName = tag.mValue
                                        val colonindex = imageName.indexOf(":")
                                        if (colonindex != -1 && colonindex < imageName.length - 1)
                                            imageName = imageName.substring(0, colonindex)
                                        val image = File(imageName).name
                                        val imageFile: File
                                        var mappedImage = SongList.mCachedCloudFiles.getMappedImageFilename(image, tempImageFileCollection)
                                        if (mappedImage == null)
                                            errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image)))
                                        else {
                                            imageFile = File(mFile.parent, mappedImage.mFile.name)
                                            if (!imageFile.exists()) {
                                                errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image)))
                                                mappedImage = null
                                            }
                                        }
                                        lineImage = mappedImage
                                    }
                                }
                                "track", "audio", "musicpath" -> {
                                    var trackName = tag.mValue
                                    val trackColonindex = trackName.indexOf(":")
                                    // volume?
                                    if (trackColonindex != -1 && trackColonindex < trackName.length - 1)
                                        trackName = trackName.substring(0, trackColonindex)
                                    val track = File(trackName).name
                                    var trackFile: File? = null
                                    val mappedTrack = SongList.mCachedCloudFiles.getMappedAudioFilename(track, tempAudioFileCollection)
                                    if (mappedTrack == null) {
                                        errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track)))
                                    } else {
                                        trackFile = File(mFile.parent, mappedTrack.mFile.name)
                                        if (!trackFile.exists()) {
                                            errors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track)))
                                            trackFile = null
                                        }
                                    }
                                    if (songMilli == 0 && trackFile != null && (chosenTrack == null || track.equals(chosenTrack, ignoreCase = true))) {
                                        try {
                                            val mmr = MediaMetadataRetriever()
                                            mmr.setDataSource(trackFile.absolutePath)
                                            val data = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                            if (data != null)
                                                songMilli = Integer.parseInt(data)
                                        } catch (e: Exception) {
                                            Log.e(BeatPrompterApplication.TAG, "Failed to extract duration metadata from media file.", e)
                                        }

                                    }
                                }
                                else -> {
                                }
                            }
                        }

                        var bars = barsTag
                        if (bars == 0) {
                            var commasFound = false
                            while (strippedLine.startsWith(",")) {
                                commasFound = true
                                strippedLine = strippedLine.substring(1)
                                bars++
                            }
                            bars = Math.max(1, bars)
                            if (!commasFound)
                                bars = barsPerLine
                        }

                        // Contains only tags? Or contains nothing? Don't use it as a blank line.
                        if (strippedLine.trim { it <= ' ' }.isNotEmpty() || chordsFound || pauseTime > 0 || lineImage != null) {
                            // removed lineImage=null from here
                            totalPauseTime += pauseTime
                            pauseTime = 0
                            // Line could potentially have been "{sometag} # comment"?
                            if (lineImage != null || chordsFound || !strippedLine.trim { it <= ' ' }.startsWith("#")) {
                                realBarCount += bars
                                realLineCount++
                            }
                        }
                    }
                    ++lineNumber
                }
            } while(line!=null)

            if (songTime == Utils.TRACK_AUDIO_LENGTH_VALUE.toLong())
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

    companion object {
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