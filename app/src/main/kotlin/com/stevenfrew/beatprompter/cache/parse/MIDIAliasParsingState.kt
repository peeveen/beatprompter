package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFile

class MIDIAliasParsingState(sourceFile: CachedCloudFile):FileParsingState(sourceFile) {
    var mAliasSetName:String?=null

    fun startNewAlias(name:String)
    {
    }

    fun addInstructionToCurrentAlias(instruction:String)
    {
    }
}