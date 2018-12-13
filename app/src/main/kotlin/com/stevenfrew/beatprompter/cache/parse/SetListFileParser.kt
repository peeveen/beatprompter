package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag

@ParseTags(SetNameTag::class)
/**
 * Parser for set list files.
 */
class SetListFileParser(cachedCloudFileDescriptor: CachedFileDescriptor)
    : TextFileParser<SetListFile>(cachedCloudFileDescriptor, true, DirectiveFinder) {
    private var mSetName: String = ""
    private val mSetListEntries = mutableListOf<SetListEntry>()

    override fun parseLine(line: TextFileLine<SetListFile>) {
        val setNameTag = line.mTags.asSequence().filterIsInstance<SetNameTag>().firstOrNull()
        if (setNameTag != null) {
            if (mSetName.isNotBlank())
                mErrors.add(FileParseError(setNameTag, R.string.set_name_defined_multiple_times))
            else
                mSetName = setNameTag.mSetName
        } else if (!line.mLineWithNoTags.isEmpty())
            mSetListEntries.add(SetListEntry(line.mLineWithNoTags))
    }

    override fun getResult(): SetListFile {
        if (mSetName.isBlank())
            throw InvalidBeatPrompterFileException(R.string.no_set_name_defined)
        return SetListFile(mCachedCloudFileDescriptor, mSetName, mSetListEntries, mErrors)
    }
}