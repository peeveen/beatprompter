package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFile
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasNameTag
import com.stevenfrew.beatprompter.midi.*
import java.util.ArrayList

class MIDIAliasParsingState(sourceFile: CachedCloudFile):FileParsingState(sourceFile) {
}