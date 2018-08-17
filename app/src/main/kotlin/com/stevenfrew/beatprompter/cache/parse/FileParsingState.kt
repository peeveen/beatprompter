package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFile

open class FileParsingState(val mSourceFile:CachedCloudFile) {
    val mErrors:MutableList<FileParseError> = mutableListOf()
}