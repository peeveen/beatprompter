package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*

abstract class SongFileParser<TResultType> constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):TextFileParser<TResultType>(cachedCloudFileDescriptor) {
    override fun parseTag(text:String,lineNumber:Int,position:Int):Tag
    {
        var txt=text
        val chord=txt.startsWith('[')
        txt=if(chord)txt.trim('[',']')else txt.trim('{','}')
        txt=txt.trim()
        var colonIndex = txt.indexOf(":")
        val spaceIndex = txt.indexOf(" ")
        if (colonIndex == -1)
            colonIndex = spaceIndex
        val name:String
        val value:String
        if (colonIndex == -1) {
            name = if (chord) txt else txt.toLowerCase()
            value = ""
        } else {
            name = if (chord) txt else txt.substring(0, colonIndex).toLowerCase()
            value = txt.substring(colonIndex + 1).trim()
       }
        if(chord)
            return ChordTag(name, lineNumber, position)
        return createSongTag(name,lineNumber,position,value)
    }

    abstract fun createSongTag(name:String,lineNumber:Int,position:Int,value:String):Tag
}