package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Element
import java.io.File
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

abstract class TextFileParser<TFileResult>(cachedCloudFileDescriptor: CachedCloudFileDescriptor):FileParser<TFileResult>(cachedCloudFileDescriptor),LineParser<TFileResult> {

    final override fun parse():TFileResult
    {
        var lineNumber=0
        mCachedCloudFileDescriptor.mFile.forEachLine {
            ++lineNumber
            val txt=it.trim()
            if(!txt.startsWith('#'))
                parseLine(TextFileLine(txt,lineNumber,this))
        }
        return getResult()
    }

    abstract fun getResult():TFileResult

    abstract fun parseTag(text:String,lineNumber:Int,position:Int):Tag
}