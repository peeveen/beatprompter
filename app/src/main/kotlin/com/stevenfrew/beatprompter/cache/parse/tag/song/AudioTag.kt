package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.normalize
import java.io.File

@NormalizedName("audio")
class AudioTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mFilename: String
    val mVolume:Int

    init {
        var trackName = value
        val trackColonIndex = trackName.indexOf(":")
        val defaultTrackVolume = BeatPrompterApplication.preferences.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_key), BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_default).toInt())+1
        var volume = defaultTrackVolume
        if (trackColonIndex != -1 && trackColonIndex < trackName.length - 1) {
            val strVolume = trackName.substring(trackColonIndex + 1)
            trackName = trackName.substring(0, trackColonIndex)
            try {
                val tryVolume = Integer.parseInt(strVolume)
                if (tryVolume < 0 || tryVolume > 100)
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.badAudioVolume))
                else
                    volume = (volume.toDouble() * (tryVolume.toDouble() / 100.0)).toInt()
            } catch (nfe: NumberFormatException) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.badAudioVolume))
            }
        }
        mFilename = File(trackName).name.normalize()
        mVolume=volume
    }
}