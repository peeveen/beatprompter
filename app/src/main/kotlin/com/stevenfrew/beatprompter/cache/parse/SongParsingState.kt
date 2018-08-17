package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.LineBeatInfo
import com.stevenfrew.beatprompter.ScrollingMode
import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.ImageFile

class SongParsingState {
    val mErrors:MutableList<FileParseError> = mutableListOf()
    val mTempAudioFileCollection: MutableList<AudioFile> = mutableListOf()
    val mTempImageFileCollection: MutableList<ImageFile> = mutableListOf()
    var mCurrentHighlightColor:Int =0
    var mSongTime:Long=0
    var mDefaultMIDIChannel:Byte=0
    var mBeatInfo=LineBeatInfo(0,0,0.0,0,0,ScrollingMode.Beat)
}