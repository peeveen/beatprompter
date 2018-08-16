package com.stevenfrew.beatprompter.cache.parse.tag

class MalformedTagException: Exception
{
    val mTag:Tag?
    internal constructor(tag:Tag?,message: String) : super(message)
    {
        mTag=tag
    }
    internal constructor(tag:Tag?,ex:Exception):super(ex)
    {
        mTag=tag
    }
}