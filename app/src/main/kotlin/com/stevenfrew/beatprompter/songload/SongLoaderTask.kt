package com.stevenfrew.beatprompter.songload

import android.os.Handler
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Song
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.parse.SongParser
import java.io.IOException


class SongLoaderTask : Task(true) {
    private var mCancelEvent: CancelEvent? = null
    private var mSongLoadInfo: SongLoadInfo? = null
    private var mSongLoadHandler: Handler? = null
    private var mRegistered: Boolean = false
    private val mSongLoadHandlerSync = Any()
    private val mLoadingSongFileSync = Any()
    private val mCancelEventSync = Any()

    private var songLoadHandler: Handler?
        get() = synchronized(mSongLoadHandlerSync) {
            return mSongLoadHandler
        }
        set(handler) = synchronized(mSongLoadHandlerSync) {
            mSongLoadHandler = handler
        }

    private val loadingSongFile: SongLoadInfo?
        get() = synchronized(mLoadingSongFileSync) {
            val result = mSongLoadInfo
            mSongLoadInfo = null
            return result
        }
    private var cancelEvent: CancelEvent?
        get() = synchronized(mCancelEventSync) {
            return mCancelEvent
        }
        set(cancelEvent) = synchronized(mCancelEventSync) {
            mCancelEvent = cancelEvent
        }

    override fun doWork() {
        val sli = loadingSongFile
        if (sli != null) {
            System.gc()
            val vSongLoadHandler = songLoadHandler
            val cancelEvent = cancelEvent
            try {
                val loadedSong = SongParser(sli.songFile,cancelEvent!!, vSongLoadHandler!!, mRegistered).parse()
                if (cancelEvent.isCancelled)
                    vSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
                else {
                    currentSong = loadedSong
                    vSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED).sendToTarget()
                }
            } catch (ioe: IOException) {
                vSongLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_FAILED, ioe.message).sendToTarget()
            }

            System.gc()
        } else
            try {
                // Nothing to do, wait a bit
                Thread.sleep(250)
            } catch (ignored: InterruptedException) {
            }

    }

    fun loadSong(sli: SongLoadInfo, handler: Handler, vCancelEvent: CancelEvent, registered: Boolean) {
        songLoadHandler = handler
        synchronized(mLoadingSongFileSync) {
            val existingCancelEvent = cancelEvent
            existingCancelEvent?.set()
            cancelEvent = vCancelEvent
            mRegistered = registered
            mSongLoadInfo = sli
        }
    }

    companion object {
        private var mCurrentSong: Song? = null

        private val mCurrentSongSync = Any()

        var currentSong: Song?
            get() = synchronized(mCurrentSongSync) {
                return mCurrentSong
            }
            private set(song) = synchronized(mCurrentSongSync) {
                mCurrentSong = song
                System.gc()
            }
    }
}
