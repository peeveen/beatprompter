package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.NormalizedName
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag

@OncePerFile
@NormalizedName("artist")
/**
 * Tag that defines the artist or subtitle for a song file.
 */
class ArtistTag internal constructor(name:String, lineNumber:Int, position:Int, val mArtist:String): ValueTag(name,lineNumber,position,mArtist)