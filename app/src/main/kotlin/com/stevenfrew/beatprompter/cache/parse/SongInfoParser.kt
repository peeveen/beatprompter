package com.stevenfrew.beatprompter.cache.parse

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.LineBeatInfo
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

class SongInfoParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor,currentAudioFiles:List<AudioFile>,currentImageFiles:List<ImageFile>):SongFileParser<SongFile>(cachedCloudFileDescriptor,currentAudioFiles,currentImageFiles) {
    var mTitle:String?=null
    var mArtist:String?=null
    var mKey:String?=null
    var mFirstChord:String?=null
    var mMIDIProgramChangeTrigger:String?=null
    var mMIDISongSelectTrigger:String?=null
    var mBPM:Double?=null
    var mMixedMode:Boolean=false
    val mAudioFiles=mutableListOf<AudioFile>()
    val mImageFiles=mutableListOf<ImageFile>()

    override fun parseLine(line: TextFileLine<SongFile>)
    {
        val titleTag=line.mTags.filterIsInstance<TitleTag>().firstOrNull()
        val artistTag=line.mTags.filterIsInstance<ArtistTag>().firstOrNull()
        val keyTag=line.mTags.filterIsInstance<KeyTag>().firstOrNull()
        val chordTag=line.mTags.filterIsInstance<ChordTag>().firstOrNull()
        val midiSongSelectTriggerTag=line.mTags.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()
        val midiProgramChangeTriggerTag=line.mTags.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()
        val bpmTag=line.mTags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val beatStartTag=line.mTags.filterIsInstance<BeatStartTag>().firstOrNull()
        val trackTag=line.mTags.filterIsInstance<TrackTag>().firstOrNull()
        val imageFileTag=line.mTags.filterIsInstance<ImageTag>().firstOrNull()

        if(titleTag!=null)
            if(!mTitle.isNullOrBlank())
                mErrors.add(FileParseError(titleTag,BeatPrompterApplication.getResourceString(R.string.title_defined_twice)))
            else
                mTitle=titleTag.mTitle

        if(artistTag!=null)
            if(!mArtist.isNullOrBlank())
                mErrors.add(FileParseError(titleTag,BeatPrompterApplication.getResourceString(R.string.artist_defined_twice)))
            else
                mArtist=artistTag.mArtist

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

    override fun getResult(): SongFile {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return SongFile(mCachedCloudFileDescriptor,"",listOf(),listOf())
    }
}
