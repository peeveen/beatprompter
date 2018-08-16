package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.AudioFile
import com.stevenfrew.beatprompter.cache.ImageFile

class SongParsingState {
    val mErrors:MutableList<FileParseError> = mutableListOf()
    val mTempAudioFileCollection: MutableList<AudioFile> = mutableListOf()
    val mTempImageFileCollection: MutableList<ImageFile> = mutableListOf()
    var mCurrentHighlightColor:Int =0
    var mSongTime:Long=0
    var mDefaultMIDIChannel:Byte=0
    var mBPM:Double=120.0
    var mBPL:Int=4
    var mBPB:Int=4
    var mScrollBeat:Int=3
}