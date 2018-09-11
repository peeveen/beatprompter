package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

open class SongFilter internal constructor(name: String, var mSongs: MutableList<SongFile>, canSort: Boolean) : Filter(name, canSort)