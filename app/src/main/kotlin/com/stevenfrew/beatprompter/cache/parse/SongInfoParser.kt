package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongScrollingMode
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.midi.SongTrigger

class SongInfoParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):SongFileParser<SongFile>(cachedCloudFileDescriptor, SongScrollingMode.Beat) {
    private var mTitle:String?=null
    private var mArtist:String?=null
    private var mKey:String?=null
    private var mFirstChord:String?=null
    private var mBPM:Double=0.0
    private var mBars:Int=0
    private var mBeats:Int=0
    private var mDuration:Long=0L
    private val mAudioFiles=mutableListOf<String>()
    private val mImageFiles=mutableListOf<String>()
    private val mTags=mutableListOf<String>()
    private var mMIDIProgramChangeTrigger:SongTrigger?=null
    private var mMIDISongSelectTrigger:SongTrigger?=null
    private var mMixedMode:Boolean=false
    private var mLines=0

    override fun parseLine(line: TextFileLine<SongFile>)
    {
        super.parseLine(line)
        ++mLines

        val titleTag=line.mTags.filterIsInstance<TitleTag>().firstOrNull()
        val artistTag=line.mTags.filterIsInstance<ArtistTag>().firstOrNull()
        val keyTag=line.mTags.filterIsInstance<KeyTag>().firstOrNull()
        val chordTag=line.mTags.filterIsInstance<ChordTag>().firstOrNull()
        val midiSongSelectTriggerTag=line.mTags.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()
        val midiProgramChangeTriggerTag=line.mTags.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()
        val bpmTag=line.mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val beatStartTag=line.mTags.filterIsInstance<BeatStartTag>().firstOrNull()
        val beatStopTag=line.mTags.filterIsInstance<BeatStopTag>().firstOrNull()
        val timeTag=line.mTags.filterIsInstance<TimeTag>().firstOrNull()
        val audioTags=line.mTags.filterIsInstance<AudioTag>()
        val imageTags=line.mTags.filterIsInstance<ImageTag>()
        val tagTags=line.mTags.filterIsInstance<TagTag>()

        if(titleTag!=null)
            mTitle=titleTag.mTitle

        if(artistTag!=null)
            mArtist=artistTag.mArtist

        if(keyTag!=null)
            mKey=keyTag.mKey

        if(chordTag!=null)
            if(mFirstChord==null && chordTag.isValidChord())
                mFirstChord=chordTag.mName

        if(midiSongSelectTriggerTag!=null)
            mMIDISongSelectTrigger=midiSongSelectTriggerTag.mTrigger

        if(midiProgramChangeTriggerTag!=null)
            mMIDIProgramChangeTrigger=midiProgramChangeTriggerTag.mTrigger

        if(bpmTag!=null)
            mBPM=bpmTag.mBPM

        if(timeTag!=null)
            mDuration=timeTag.mDuration

        if(beatStartTag!=null || beatStopTag!=null)
            mMixedMode=true

        if(!line.mTaglessLine.isBlank() || imageTags.isNotEmpty() || chordTag!=null) {
            mBars += mCurrentLineBeatInfo.mBPL
            mBeats += mCurrentLineBeatInfo.mBeats
        }

        mAudioFiles.addAll(audioTags.map{it.mFilename })
        mImageFiles.addAll(imageTags.map{it.mFilename })
        mTags.addAll(tagTags.map{it.mTag })
    }

    override fun getResult(): SongFile {
        if (mTitle.isNullOrBlank())
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.noTitleFound, mCachedCloudFileDescriptor.mName))
        if(mArtist.isNullOrBlank())
            mArtist=""
        val key=
                if (mKey.isNullOrBlank())
                    if(mFirstChord.isNullOrBlank())
                        ""
                    else
                        mFirstChord!!
                else
                    mKey!!

        return SongFile(mCachedCloudFileDescriptor,mLines,mBars,mTitle!!,mArtist!!,key,mBPM,mDuration,mAudioFiles,mImageFiles,mTags.toSet(),mMIDIProgramChangeTrigger?: SongTrigger.DEAD_TRIGGER,mMIDISongSelectTrigger?: SongTrigger.DEAD_TRIGGER,mErrors)
    }

    override fun createSongTag(name:String,lineNumber:Int,position:Int,value:String): Tag
    {
        return when(name)
        {
            "time" -> TimeTag(name, lineNumber, position, value)
            "image"-> ImageTag(name, lineNumber, position, value)
            "midi_song_select_trigger"-> MIDISongSelectTriggerTag(name, lineNumber, position, value)
            "midi_program_change_trigger"-> MIDIProgramChangeTriggerTag(name, lineNumber, position, value)
            "title", "t" -> TitleTag(name, lineNumber, position, value)
            "artist", "a", "subtitle", "st"-> ArtistTag(name, lineNumber, position, value)
            "key"-> KeyTag(name, lineNumber, position, value)
            "tag"-> TagTag(name, lineNumber, position, value)
            // Don't care about any other tags in this context, treat them as all irrelevant ChordPro tags
            else-> UnusedTag(name,lineNumber,position)
        }
    }

    // TODO: parse error if pauses exceed defined length
    /*
      @Throws(IOException::class)
    fun getTimePerLineAndBar(chosenTrack: AudioFile?): SmoothScrollingTimings {
        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
        try {
            var songTime: Long = 0
            var songMilli = 0
            var pauseTime: Long = 0
            var realLineCount = 0
            var realBarCount = 0
            val parsingState=SongParsingState(SongScrollingMode.Beat,this)
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
                            else if(it is AudioTag) {
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
*/
}
