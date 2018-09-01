package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongScrollingMode
import com.stevenfrew.beatprompter.Utils
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
    private var mTotalPause:Long=0L
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

        val titleTag = line.mTags.filterIsInstance<TitleTag>().firstOrNull()
        val artistTag = line.mTags.filterIsInstance<ArtistTag>().firstOrNull()
        val keyTag = line.mTags.filterIsInstance<KeyTag>().firstOrNull()
        val chordTag = line.mTags.filterIsInstance<ChordTag>().firstOrNull()
        val midiSongSelectTriggerTag = line.mTags.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()
        val midiProgramChangeTriggerTag = line.mTags.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()
        val bpmTag = line.mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val beatStartTag = line.mTags.filterIsInstance<BeatStartTag>().firstOrNull()
        val beatStopTag = line.mTags.filterIsInstance<BeatStopTag>().firstOrNull()
        val timeTag = line.mTags.filterIsInstance<TimeTag>().firstOrNull()
        val audioTags = line.mTags.filterIsInstance<AudioTag>()
        val imageTags = line.mTags.filterIsInstance<ImageTag>()
        val pauseTag = line.mTags.filterIsInstance<PauseTag>().firstOrNull()
        val tagTags = line.mTags.filterIsInstance<TagTag>()

        if (titleTag != null)
            mTitle = titleTag.mTitle

        if (artistTag != null)
            mArtist = artistTag.mArtist

        if (keyTag != null)
            mKey = keyTag.mKey

        if (chordTag != null)
            if (mFirstChord == null && chordTag.isValidChord())
                mFirstChord = chordTag.mName

        if (midiSongSelectTriggerTag != null)
            mMIDISongSelectTrigger = midiSongSelectTriggerTag.mTrigger

        if (midiProgramChangeTriggerTag != null)
            mMIDIProgramChangeTrigger = midiProgramChangeTriggerTag.mTrigger

        if (bpmTag != null)
            mBPM = bpmTag.mBPM

        if(pauseTag!=null)
            mTotalPause+=pauseTag.mDuration

        if (timeTag != null)
            mDuration = timeTag.mDuration

        if (beatStartTag != null || beatStopTag != null)
            mMixedMode = true

        if (!line.mTaglessLine.isBlank() || imageTags.isNotEmpty() || chordTag != null) {
            mBars += mCurrentLineBeatInfo.mBPL
            mBeats += mCurrentLineBeatInfo.mBeats
        }

        mAudioFiles.addAll(audioTags.map { it.mFilename })
        mImageFiles.addAll(imageTags.map { it.mFilename })
        mTags.addAll(tagTags.map { it.mTag })
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

        return SongFile(mCachedCloudFileDescriptor,mLines,mBars,mTitle!!,mArtist!!,key,mBPM,mDuration,mTotalPause,mAudioFiles,mImageFiles,mTags.toSet(),mMIDIProgramChangeTrigger?: SongTrigger.DEAD_TRIGGER,mMIDISongSelectTrigger?: SongTrigger.DEAD_TRIGGER,mErrors)
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
            "pause"-> PauseTag(name, lineNumber, position, value)
            "tag"-> TagTag(name, lineNumber, position, value)
            // Don't care about any other tags in this context, treat them as all irrelevant ChordPro tags
            else-> UnusedTag(name,lineNumber,position)
        }
    }
}
