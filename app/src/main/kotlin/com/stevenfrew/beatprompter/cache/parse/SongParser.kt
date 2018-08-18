package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.Song
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Element
import java.io.File

class SongParser:SongFileParser<Song> {
    constructor(downloadedFile: File, cloudFile: CloudFileInfo):super(downloadedFile,cloudFile)
    constructor(element: Element):super(element)

    override fun parseLine(line: TextFileLine<Song>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getResult(): Song {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return SongFile(mDownloadedFile,mCloudFile,"",listOf(),listOf())
    }
}

