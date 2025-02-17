package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagName(
	"start_of_tab",
	"end_of_tab",
	"sot",
	"eot",
	"define",
	"textfont",
	"tf",
	"textsize",
	"ts",
	"chordfont",
	"cf",
	"chordsize",
	"cs",
	"no_grid",
	"ng",
	"grid",
	"g",
	"titles",
	"new_page",
	"np",
	"new_physical_page",
	"npp",
	"columns",
	"col",
	"column_break",
	"colb",
	"pagetype",
	"zoom-android",
	"zoom",
	"tempo",
	"tempo-android",
	"instrument",
	"tuning"
)
@TagType(Type.Directive)
/**
 * Tags that aren't used by BeatPrompter, but might exist in files used by other similar apps.
 * Also tags that BeatPrompter once supported, but doesn't anymore.
 */
class LegacyTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : Tag(name, lineNumber, position)