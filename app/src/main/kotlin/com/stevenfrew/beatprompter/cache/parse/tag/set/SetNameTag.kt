package com.stevenfrew.beatprompter.cache.parse.tag.set

import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerFile
@TagName("set")
@TagType(Type.Directive)
/**
 * Tag that defines the name of a setlist.
 */
class SetNameTag internal constructor(name: String,
                                      lineNumber: Int,
                                      position: Int,
                                      val mSetName: String)
    : ValueTag(name, lineNumber, position, mSetName)