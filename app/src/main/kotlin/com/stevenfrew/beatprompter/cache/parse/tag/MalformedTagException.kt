package com.stevenfrew.beatprompter.cache.parse.tag

class MalformedTagException: Exception
{
    internal constructor(message: String) : super(message)
    internal constructor(ex:Exception):super(ex)

}