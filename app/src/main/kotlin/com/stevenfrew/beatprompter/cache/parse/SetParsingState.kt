package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFile

class SetParsingState(sourceFile:CachedCloudFile):FileParsingState(sourceFile) {
    var mSetName:String?=null

    fun addSongToSet(song:String)
    {
    }
}