package com.stevenfrew.beatprompter.cache.parse.tag.find

/**
 * Base class for tag finders that find tags that are enclosed in delimiters.
 */
open class EnclosedTagFinder(private val mStartChar:Char, private val mEndChar:Char, private val mTagType:Type, private val mRetainCase:Boolean, private val mValued:Boolean): TagFinder {
    override fun findTag(text: String): FoundTag? {
        val directiveStart = text.indexOf(mStartChar)
        if(directiveStart!=-1)
        {
            val directiveEnd=text.indexOf(mEndChar,directiveStart+1)
            if(directiveEnd!=-1) {
                val enclosedText=text.substring(directiveStart+1,directiveEnd).trim()
                val name:String
                val value:String
                if(mValued)
                {
                    // Can't use splitAndTrim in case of something like {time:5:00}
                    val colonIndex=enclosedText.indexOf(":")
                    if(colonIndex==-1) {
                        name = enclosedText
                        value = ""
                    }
                    else {
                        name=enclosedText.substring(0,colonIndex).trim()
                        value=enclosedText.substring(colonIndex+1).trim()
                    }
                }
                else {
                    name = enclosedText
                    value=""
                }
                return FoundTag(directiveStart, directiveEnd, if(mRetainCase) name else name.toLowerCase(), value, mTagType)
            }
        }
        return null
    }
}