package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.find.*
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

abstract class SongFileParser<TResultType> constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):TextFileParser<TResultType>(cachedCloudFileDescriptor, DirectiveFinder, ChordFinder, MarkerFinder) {
    override fun parseTag(foundTag: FoundTag,lineNumber:Int):Tag
    {
        val txt=foundTag.mText
        val colonIndex = txt.indexOf(":")
        val spaceIndex = txt.indexOf(" ")
        val realIndex=if(colonIndex==-1) spaceIndex else colonIndex
        val name:String
        val value:String
        if (realIndex == -1) {
            name = if (foundTag.mType== TagType.Chord) txt else txt.toLowerCase()
            value = ""
        } else {
            name = if (foundTag.mType== TagType.Chord) txt else txt.substring(0, realIndex).toLowerCase()
            value = txt.substring(realIndex + 1).trim()
       }
        if(foundTag.mType== TagType.Chord)
            return ChordTag(name, lineNumber, foundTag.mStart)
        return createSongTag(name,lineNumber,foundTag.mStart,value)
    }

    abstract fun createSongTag(name:String,lineNumber:Int,position:Int,value:String):Tag
}