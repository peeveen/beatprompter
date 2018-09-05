package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.TagFinder
import com.stevenfrew.beatprompter.cache.parse.tag.find.FoundTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.removeControlCharacters
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

abstract class TextFileParser<TFileResult>(cachedCloudFileDescriptor: CachedCloudFileDescriptor, private vararg val mTagFinders: TagFinder):FileParser<TFileResult>(cachedCloudFileDescriptor),LineParser<TFileResult> {
    final override fun parse():TFileResult
    {
        var lineNumber = 0
        val fileTags = mutableSetOf<KClass<out Tag>>()
        val livePairings = mutableSetOf<Pair<KClass<out Tag>, KClass<out Tag>>>()
        mCachedCloudFileDescriptor.mFile.forEachLine { strLine ->
            // TODO: solve double blank line issue
            ++lineNumber
            val txt = strLine.trim().removeControlCharacters()
            if (!txt.startsWith('#')) {
                val textLine = TextFileLine(txt, lineNumber, this)
                val lineTags = mutableSetOf<KClass<out Tag>>()
                textLine.mTags.forEach { tag ->
                    val tagClass = tag::class
                    val isOncePerFile = tagClass.findAnnotation<OncePerFile>() != null
                    val isOncePerLine = tagClass.findAnnotation<OncePerLine>() != null
                    val startedByAnnotation = tagClass.findAnnotation<StartedBy>()
                    val endedByAnnotation = tagClass.findAnnotation<EndedBy>()
                    val lineExclusiveTags = tagClass.annotations.filterIsInstance<LineExclusive>()
                    val alreadyUsedInFile = fileTags.contains(tagClass)
                    val alreadyUsedInLine = lineTags.contains(tagClass)
                    fileTags.add(tagClass)
                    lineTags.add(tagClass)
                    if (isOncePerFile && alreadyUsedInFile)
                        mErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.tag_used_multiple_times_in_file, tag.mName)))
                    if (isOncePerLine && alreadyUsedInLine)
                        mErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.tag_used_multiple_times_in_line, tag.mName)))
                    if (startedByAnnotation != null) {
                        val startedByClass = startedByAnnotation.mStartedBy
                        val startedByEndedByAnnotation = startedByClass.findAnnotation<EndedBy>()!!
                        val endedByClass = startedByEndedByAnnotation.mEndedBy
                        if (!livePairings.remove(Pair(startedByClass, endedByClass)))
                            mErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.ending_tag_found_before_starting_tag, tag.mName)))
                    } else if (endedByAnnotation != null) {
                        val endedByClass = endedByAnnotation.mEndedBy
                        val endedByStartedByAnnotation = endedByClass.findAnnotation<StartedBy>()!!
                        val startedByClass = endedByStartedByAnnotation.mStartedBy
                        val pairing = Pair(startedByClass, endedByClass)
                        if (livePairings.contains(pairing))
                            mErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.starting_tag_found_after_starting_tag, tag.mName)))
                        else
                            livePairings.add(pairing)
                    }
                    lineExclusiveTags.forEach {
                        if (lineTags.contains(it.mCantShareWith))
                            mErrors.add(FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.tag_cant_share_line_with,
                                    tag.mName,
                                    it.mCantShareWith.findAnnotation<TagName>()!!.mNames.first())))
                    }
                }
                parseLine(textLine)
            }
        }
        return getResult()
    }

    abstract fun getResult():TFileResult

    fun parseTag(foundTag: FoundTag, lineNumber:Int):Tag?
    {
        val thisClass=this::class

        // Should we ignore this tag?
        val ignoreTagsAnnotations=thisClass.annotations.filterIsInstance<IgnoreTags>()
        val matchingIgnoreTagClass=ignoreTagsAnnotations.flatMap { it.mTagClasses.toList() }.firstOrNull{tagClass->tagClass.annotations.filterIsInstance<TagName>().any{it.mNames.contains(foundTag.mName)}}
        if(matchingIgnoreTagClass!=null)
            // Yes we should!
            return null

        // OK, can't ignore this tag, so better parse it.
        val parseTagsAnnotations=thisClass.annotations.filterIsInstance<ParseTags>()
        val parseTagClasses=parseTagsAnnotations.flatMap { it.mTagClasses.toList() }.filter{it.annotations.filterIsInstance<TagType>().any{typeAnnotation->typeAnnotation.mType==foundTag.mType}}
        var matchingTagClass=parseTagClasses.firstOrNull{tagClass->tagClass.annotations.filterIsInstance<TagName>().any{it.mNames.contains(foundTag.mName)}}
        // Do any of the tag classes match the tag name?
        if(matchingTagClass==null)
            // If there is a tag class with no tag name, we use that.
            matchingTagClass=parseTagClasses.firstOrNull{it.annotations.filterIsInstance<TagName>().isEmpty()}
        if(matchingTagClass==null)
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unexpected_tag_in_file,foundTag.mName))
        // Construct a tag of this class
        // If it's a value class, pass the value to the constructor
        return if(matchingTagClass.isSubclassOf(ValueTag::class))
            matchingTagClass.primaryConstructor!!.call(foundTag.mName,lineNumber,foundTag.mStart,foundTag.mValue)
        else
            matchingTagClass.primaryConstructor!!.call(foundTag.mName,lineNumber,foundTag.mStart)
    }

    fun findFirstTag(text:String): FoundTag?
    {
        var bestInfo: FoundTag?=null
        for(finder in mTagFinders)
        {
            val info=finder.findTag(text)
            if(info!=null)
                if(bestInfo==null || info.mStart<bestInfo.mStart)
                    bestInfo=info
        }
        return bestInfo
    }
}