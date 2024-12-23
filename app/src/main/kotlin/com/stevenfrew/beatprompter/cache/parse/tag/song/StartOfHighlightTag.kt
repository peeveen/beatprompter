package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@EndedBy(EndOfHighlightTag::class)
@TagName("soh", "start_of_highlight", "startofhighlight")
@TagType(Type.Directive)
/**
 * Tag that defines the start of a block of highlighted text.
 */
class StartOfHighlightTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ColorTag(
	name,
	lineNumber,
	position,
	value.ifBlank { getDefaultHighlightColorString() }
) {
	companion object {
		fun getDefaultHighlightColorString(): String =
			"#${
				((BeatPrompter.preferences.defaultHighlightColor and 0x00FFFFFF).toString(16)
					.padStart(6, '0'))
			}"
	}
}
