package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.filter.SetListMatch

class SetListEntry(line: String) {
    private val mTitle:String
    private val mArtist:String

    constructor(songFile:SongFile):this(songFile.mTitle,songFile.mArtist)
    constructor(title:String,artist:String):this(title+ SET_LIST_ENTRY_DELIMITER+artist)

    init {
        val bits=line.split(SET_LIST_ENTRY_DELIMITER)
        if(bits.size>1) {
            mTitle = bits[0].trim()
            mArtist = bits[1].trim()
        }
        else {
            mTitle = line.trim()
            mArtist=""
        }
    }
    companion object {
        private const val SET_LIST_ENTRY_DELIMITER="____"
    }

    fun matches(songFile: SongFile): SetListMatch
    {
        if(songFile.mTitle==mTitle)
        {
            if(songFile.mArtist==mArtist)
                return SetListMatch.TitleAndArtistMatch
            return SetListMatch.TitleMatch
        }
        else
            return SetListMatch.NoMatch
    }

    override fun toString():String{
        return mTitle+ SET_LIST_ENTRY_DELIMITER+mArtist
    }
}