package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class TextFileParser<TFileResult>(cachedCloudFileDescriptor: CachedCloudFileDescriptor):FileParser<TFileResult>(cachedCloudFileDescriptor),LineParser<TFileResult> {

    final override fun parse():TFileResult
    {
        var lineNumber=0
        val fileTags=mutableSetOf<KClass<out Tag>>()
        mCachedCloudFileDescriptor.mFile.forEachLine { strLine ->
            ++lineNumber
            val txt=strLine.trim()
            if(!txt.startsWith('#')) {
                val textLine=TextFileLine(txt,lineNumber,this)
                val lineTags=mutableSetOf<KClass<out Tag>>()
                val livePairings=mutableSetOf<Pair<KClass<out Tag>,KClass<out Tag>>>()
                textLine.mTags.forEach{tag->
                    val tagClass=tag::class
                    val isOncePerFile=tagClass.findAnnotation<OncePerFile>()!=null
                    val isOncePerLine=tagClass.findAnnotation<OncePerLine>()!=null
                    val startedByAnnotation=tagClass.findAnnotation<StartedBy>()
                    val endedByAnnotation=tagClass.findAnnotation<EndedBy>()
                    val alreadyUsedInFile=fileTags.contains(tagClass)
                    val alreadyUsedInLine=lineTags.contains(tagClass)
                    fileTags.add(tagClass)
                    lineTags.add(tagClass)
                    if(isOncePerFile && alreadyUsedInFile)
                        mErrors.add(FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.tag_used_multiple_times_in_file,tag.mName)))
                    if(isOncePerLine && alreadyUsedInLine)
                        mErrors.add(FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.tag_used_multiple_times_in_line,tag.mName)))
                    if(startedByAnnotation!=null)
                    {
                        val startedByClass=startedByAnnotation.mStartedBy
                        val startedByEndedByAnnotation=startedByClass.findAnnotation<EndedBy>()!!
                        val endedByClass=startedByEndedByAnnotation.mEndedBy
                        if(!livePairings.remove(Pair(startedByClass, endedByClass)))
                            mErrors.add(FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.ending_tag_found_before_starting_tag,tag.mName)))
                    }
                    else if(endedByAnnotation!=null)
                    {
                        val endedByClass=endedByAnnotation.mEndedBy
                        val endedByStartedByAnnotation=endedByClass.findAnnotation<StartedBy>()!!
                        val startedByClass=endedByStartedByAnnotation.mStartedBy
                        val pairing=Pair(startedByClass, endedByClass)
                        if(livePairings.contains(pairing))
                            mErrors.add(FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.starting_tag_found_after_starting_tag,tag.mName)))
                        else
                            livePairings.add(pairing)
                    }
                }
                parseLine(textLine)
            }
        }
        return getResult()
    }

    abstract fun getResult():TFileResult

    abstract fun parseTag(text:String,lineNumber:Int,position:Int):Tag
}