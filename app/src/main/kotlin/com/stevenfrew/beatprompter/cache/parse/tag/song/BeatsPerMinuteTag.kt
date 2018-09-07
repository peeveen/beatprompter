package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("bpm","beatsperminute","metronome")
@TagType(Type.Directive)
/**
 * Tag that defines (or redefines) the tempo of a song file from this point onwards.
 */
class BeatsPerMinuteTag internal constructor(name:String, lineNumber:Int, position:Int, value:String): ValueTag(name,lineNumber,position,value) {
    val mBPM:Double = TagUtility.parseDoubleValue(value, 10,300)
}