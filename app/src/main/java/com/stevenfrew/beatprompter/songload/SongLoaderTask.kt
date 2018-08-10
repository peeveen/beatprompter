package com.stevenfrew.beatprompter.songload

import android.os.Handler
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Song
import com.stevenfrew.beatprompter.SongParser
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.event.CancelEvent
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
            val songLoadHandler = songLoadHandler
            val cancelEvent = cancelEvent
            try {
                val loader = SongParser(sli, cancelEvent!!, songLoadHandler!!, mRegistered)
                val loadedSong = loader.parse()
                if (cancelEvent!!.isCancelled)
                    songLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
                else {
                    currentSong = loadedSong
                    songLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_COMPLETED).sendToTarget()
                }
            } catch (ioe: IOException) {
                songLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_FAILED, ioe.message).sendToTarget()
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
