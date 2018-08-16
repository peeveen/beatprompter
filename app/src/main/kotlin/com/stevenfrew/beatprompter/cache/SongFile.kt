package com.stevenfrew.beatprompter.cache

import android.media.MediaMetadataRetriever
import android.util.Log
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.FileLine
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cache.parse.SongParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
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
    var mKey: String = ""
    var mLines = 0
    var mMixedMode = false
    var mArtist: String? = null

    val sortableArtist
        get()= sortableString(mArtist)

    val sortableTitle
        get()= sortableString(mTitle)

    var mSongSelectTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
    var mProgramChangeTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER

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
    private fun parseSongFileInfo(tempAudioFileCollection: ArrayList<AudioFile>, tempImageFileCollection: ArrayList<ImageFile>) {
        var br: BufferedReader? = null
        try {
            val (timePerLine, timePerBar) = getTimePerLineAndBar(null, tempAudioFileCollection, tempImageFileCollection)
            var title:String?=null
            mTimePerLine = timePerLine
            mTimePerBar = timePerBar
            val parsingState= SongParsingState()
            br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
            var line: String?
            var lineNumber = 0
            do {
                line = br.readLine()
                if(line!=null)
                {
                    val fileLine= FileLine(line, ++lineNumber,mFile,parsingState)
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
                    mAudioFiles.addAll(audios)
                    val images = fileLine.getImageFiles()
                    mImageFiles.addAll(images)
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
    fun getTimePerLineAndBar(chosenTrack: AudioFile?, tempAudioFileCollection: MutableList<AudioFile> =mutableListOf(), tempImageFileCollection: MutableList<ImageFile> =mutableListOf()): SmoothScrollingTimings {
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
            val parsingState=SongParsingState()
            var totalPauseTime: Long = 0//defaultPausePref*1000;
            var line: String?
            var lineImage: ImageFile? = null
            var lineNumber = 0
            val errors = ArrayList<FileParseError>()
            do {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= FileLine(line, ++lineNumber,mFile,parsingState)
                    // Ignore comments.
                    if (!fileLine.isComment) {
                        val strippedLine = fileLine.mTaglessLine
                        val chordsFound = fileLine.chordTags.isNotEmpty()
                        val bars=fileLine.mBars
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
                                if (songMilli == 0 && (chosenTrack == null || it.mAudioFile.equals(chosenTrack))) {
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