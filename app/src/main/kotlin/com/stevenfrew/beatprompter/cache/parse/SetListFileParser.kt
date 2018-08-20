package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag

class SetListFileParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):TextFileParser<SetListFile>(cachedCloudFileDescriptor) {

    private var mSetName:String=""
    private val mSetListEntries=mutableListOf<SetListEntry>()

    override fun parseLine(line: TextFileLine<SetListFile>) {
        val setNameTag=line.mTags.filterIsInstance<SetNameTag>().firstOrNull()
        if(setNameTag!=null) {
            if (mSetName.isNotBlank())
                mErrors.add(FileParseError(setNameTag, BeatPrompterApplication.getResourceString(R.string.set_name_defined_multiple_times)))
            else
                mSetName = setNameTag.mSetName
        }
        else if(!line.mTaglessLine.isEmpty())
            mSetListEntries.add(SetListEntry(line.mTaglessLine))
    }

    override fun getResult(): SetListFile {
        if(mSetName.isBlank())
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.no_set_name_defined))
        return SetListFile(mCachedCloudFileDescriptor,mSetName,mSetListEntries,mErrors)
    }

    override fun parseTag(text: String, lineNumber: Int, position: Int): Tag {
        val txt=text.trim('{','}')
        val bits=txt.split(':')
        if(bits.size==2)
        {
            val tagName=bits[0].trim()
            val tagValue=bits[1].trim()
            when(tagName)
            {
                "set"->return SetNameTag(tagName,lineNumber,position,tagValue)
                else->throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unexpected_tag_in_setlist_file))
            }
        }
        else
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts))
    }
}