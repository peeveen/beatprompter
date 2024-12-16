package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.parse.tag.find.DirectiveFinder
import com.stevenfrew.beatprompter.cache.parse.tag.set.SetNameTag
import com.stevenfrew.beatprompter.set.SetListEntry

@ParseTags(SetNameTag::class)
/**
 * Parser for set list files.
 */
class SetListFileParser(cachedCloudFile: CachedFile) :
	TextFileParser<SetListFile>(cachedCloudFile, true, false, false, DirectiveFinder) {
	private var setName: String = ""
	private val setListEntries = mutableListOf<SetListEntry>()

	override fun parseLine(line: TextFileLine<SetListFile>): Boolean {
		val setNameTag = line
			.tags
			.asSequence()
			.filterIsInstance<SetNameTag>()
			.firstOrNull()
		if (setNameTag != null) {
			if (setName.isNotBlank())
				addError(FileParseError(setNameTag, R.string.set_name_defined_multiple_times))
			else
				setName = setNameTag.setName
		} else if (line.lineWithNoTags.isNotEmpty())
			setListEntries.add(SetListEntry(line.lineWithNoTags))
		return true
	}

	override fun getResult(): SetListFile {
		if (setName.isBlank())
			throw InvalidBeatPrompterFileException(R.string.no_set_name_defined)
		return SetListFile(cachedCloudFile, setName, setListEntries, errors)
	}
}