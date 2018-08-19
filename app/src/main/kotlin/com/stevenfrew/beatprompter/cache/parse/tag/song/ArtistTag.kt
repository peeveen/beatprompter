package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
class ArtistTag internal constructor(name:String, lineNumber:Int, position:Int, val mArtist:String): ValueTag(name,lineNumber,position,mArtist)