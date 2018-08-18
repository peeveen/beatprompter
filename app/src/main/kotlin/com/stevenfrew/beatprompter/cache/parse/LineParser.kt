package com.stevenfrew.beatprompter.cache.parse

interface LineParser<TFileType> {
    fun parseLine(line:TextFileLine<TFileType>)
}