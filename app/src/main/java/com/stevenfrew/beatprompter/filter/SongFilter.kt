package com.stevenfrew.beatprompter.filter

import com.stevenfrew.beatprompter.cache.SongFile

open class SongFilter internal constructor(name: String, @JvmField var mSongs: MutableList<SongFile>, canSort: Boolean) : Filter(name, canSort)