package com.stevenfrew.beatprompter.cache.parse.tag.song

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.EndedBy
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@EndedBy(EndOfHighlightTag::class)
@TagName("soh")
@TagType(Type.Directive)
/**
 * Tag that defines the start of a block of highlighted text.
 */
class StartOfHighlightTag internal constructor(name:String,lineNumber:Int,position:Int,value:String)
    : ColorTag(name,lineNumber,position,if(value.isBlank()) "#"+((BeatPrompterApplication.preferences.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default))) and 0x00FFFFFF).toString(16).padStart(6,'0')) else value)