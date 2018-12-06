package com.stevenfrew.beatprompter.song.load

import android.os.Message
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.cache.parse.SongParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class SongLoadJob(val mSongLoadInfo: SongLoadInfo,
                  private val mRegistered: Boolean)
    : CoroutineScope {
    private val mHandler = SongLoadJobEventHandler()
    private val mCancelEvent = SongLoadCancelEvent(mSongLoadInfo.mSongFile.mTitle)
    private val mCoRoutineJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + mCoRoutineJob

    fun startLoading() {
        synchronized(this)
        {
            val thisSongLoadJob = this
            launch {
                System.gc()
                try {
                    Logger.logLoader("Starting to load '${mSongLoadInfo.mSongFile.mTitle}'.")
                    val loadedSong = SongParser(mSongLoadInfo, mCancelEvent, mHandler, mRegistered).parse()
                    if (mCancelEvent.isCancelled)
                        throw SongLoadCancelledException()
                    Logger.logLoader("Song was loaded successfully.")
                    SongLoadQueueWatcherTask.onSongLoadFinished()
                    mLoadedSong = LoadedSong(loadedSong, thisSongLoadJob)
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED, mSongLoadInfo.mLoadID).sendToTarget()
                } catch (e: SongLoadCancelledException) {
                    Logger.logLoader("Song load was cancelled.")
                    SongLoadQueueWatcherTask.onSongLoadFinished()
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
                } catch (e: Exception) {
                    Logger.logLoader("Song load failed.")
                    SongLoadQueueWatcherTask.onSongLoadFinished()
                    mHandler.obtainMessage(EventHandler.SONG_LOAD_FAILED, e.message).sendToTarget()
                } finally {
                    System.gc()
                }
            }
        }
    }

    fun stopLoading() {
        mCancelEvent.set()
    }

    class SongLoadJobEventHandler : EventHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EventHandler.SONG_LOAD_CANCELLED ->
                    EventHandler.sendEventToSongList(msg.what)
                EventHandler.SONG_LOAD_FAILED, EventHandler.SONG_LOAD_COMPLETED ->
                    EventHandler.sendEventToSongList(msg.what, msg.obj)
                EventHandler.SONG_LOAD_LINE_PROCESSED ->
                    EventHandler.sendEventToSongList(EventHandler.SONG_LOAD_LINE_PROCESSED, msg.arg1, msg.arg2)
            }
        }
    }

    companion object {
        var mLoadedSong: LoadedSong? = null
    }
}