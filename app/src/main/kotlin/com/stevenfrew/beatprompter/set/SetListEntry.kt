package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ui.filter.SetListMatch
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Represents one entry from a set list file.
 */
class SetListEntry private constructor(private val mNormalizedTitle:String,private val mNormalizedArtist:String) {
    private constructor(titleAndArtist:Pair<String,String>):this(titleAndArtist.first,titleAndArtist.second)
    constructor(songFile:SongFile):this(songFile.mNormalizedTitle,songFile.mNormalizedArtist)
    constructor(setListFileLine:String):this(getTitleAndArtistFromSetListLine(setListFileLine))

    fun matches(songFile: SongFile): SetListMatch
    {
        if(songFile.mNormalizedTitle.equals(mNormalizedTitle,true))
        {
            if(songFile.mNormalizedArtist.equals(mNormalizedArtist,true))
                return SetListMatch.TitleAndArtistMatch
            return SetListMatch.TitleMatch
        }
        else
            return SetListMatch.NoMatch
    }

    fun toDisplayString():String
    {
        return if(mNormalizedArtist.isBlank()) mNormalizedTitle else "$mNormalizedArtist - $mNormalizedTitle"
    }

    override fun toString():String{
        return mNormalizedTitle+ SET_LIST_ENTRY_DELIMITER +mNormalizedArtist
    }

    companion object {
        private const val SET_LIST_ENTRY_DELIMITER="==="

        private fun getTitleAndArtistFromSetListLine(setListFileLine:String):Pair<String,String>
        {
            val bits=setListFileLine.splitAndTrim(SET_LIST_ENTRY_DELIMITER)
            return if(bits.size>1)
                bits[0] to bits[1]
            else
                bits[0] to ""
        }
    }
}