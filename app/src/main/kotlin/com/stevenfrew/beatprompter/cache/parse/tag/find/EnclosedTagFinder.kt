package com.stevenfrew.beatprompter.cache.parse.tag.find

open class EnclosedTagFinder(private val mStartChar:Char,private val mEndChar:Char,private val mTagType:TagType): TagFinder {
    override fun findTag(text: String): FoundTag? {
        val directiveStart = text.indexOf(mStartChar)
        if(directiveStart!=-1)
        {
            val directiveEnd=text.indexOf(mEndChar,directiveStart+1)
            if(directiveEnd!=-1) {
                val enclosedText=text.substring(directiveStart+1,directiveEnd).trim()
                return FoundTag(directiveStart, directiveEnd, enclosedText, mTagType)
            }
        }
        return null
    }
}