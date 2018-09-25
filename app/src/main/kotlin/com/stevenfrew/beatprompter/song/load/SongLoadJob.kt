package com.stevenfrew.beatprompter.song.load

import android.os.Handler

data class SongLoadJob(val mSongLoadInfo:SongLoadInfo, val mHandler: Handler, val mCancelEvent: SongLoadCancelEvent, val mRegistered: Boolean)