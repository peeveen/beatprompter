package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag

@ParseTags(SetNameTag::class)
class SetListFileParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):TextFileParser<SetListFile>(cachedCloudFileDescriptor, true, DirectiveFinder) {

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
        else if(!line.mLineWithNoTags.isEmpty())
            mSetListEntries.add(SetListEntry(line.mLineWithNoTags))
    }

    override fun getResult(): SetListFile {
        if(mSetName.isBlank())
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.no_set_name_defined))
        return SetListFile(mCachedCloudFileDescriptor,mSetName,mSetListEntries,mErrors)
    }
}