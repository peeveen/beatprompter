package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import java.io.File

class TrackTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,sourceFile:File,tempAudioFileCollection: List<AudioFile>): Tag(name,lineNumber,position) {
    val mAudioFile: AudioFile

    init {
        var trackName = value
        val trackColonindex = trackName.indexOf(":")
        // volume?
        if (trackColonindex != -1 && trackColonindex < trackName.length - 1)
            trackName = trackName.substring(0, trackColonindex)
        val track = File(trackName).name
        val mappedTrack = SongList.mCachedCloudFiles.getMappedAudioFilename(track, tempAudioFileCollection)
                ?: throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track))
        val trackFile = File(sourceFile.parent, mappedTrack.mFile.name)
        if (!trackFile.exists())
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track))
        mAudioFile=mappedTrack
    }
}