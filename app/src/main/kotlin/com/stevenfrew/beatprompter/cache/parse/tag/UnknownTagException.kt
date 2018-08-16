package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

class UnknownTagException internal constructor(unknownTag: String) : Exception(BeatPrompterApplication.getResourceString(R.string.unknown_tag,unknownTag))