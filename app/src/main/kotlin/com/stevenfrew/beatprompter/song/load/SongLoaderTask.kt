package com.stevenfrew.beatprompter.song.load

import android.os.Handler
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.song.Song
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.parse.SongParser

class SongLoaderTask : Task(true) {
    private var mSongLoadCancelEvent: SongLoadCancelEvent? = null
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
    private var songLoadCancelEvent: SongLoadCancelEvent?
        get() = synchronized(mCancelEventSync) {
            return mSongLoadCancelEvent
        }
        set(cancelEvent) = synchronized(mCancelEventSync) {
            mSongLoadCancelEvent = cancelEvent
        }

    override fun doWork() {
        val sli = loadingSongFile
        if (sli != null) {
            System.gc()
            val vSongLoadHandler = songLoadHandler
            val cancelEvent = songLoadCancelEvent
            try {
                val loadedSong = SongParser(sli,cancelEvent!!, vSongLoadHandler!!, mRegistered).parse()
                if (cancelEvent.isCancelled)
                    throw SongLoadCancelledException()
                currentSong = loadedSong
                vSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_COMPLETED).sendToTarget()
            } catch (e: SongLoadCancelledException) {
                vSongLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_CANCELLED).sendToTarget()
            } catch (e: Exception) {
                vSongLoadHandler!!.obtainMessage(EventHandler.SONG_LOAD_FAILED, e.message).sendToTarget()
            }

            System.gc()
        } else
            try {
                // Nothing to do, wait a bit
                Thread.sleep(250)
            } catch (ignored: InterruptedException) { }
    }

    fun loadSong(sli: SongLoadInfo, handler: Handler, vSongLoadCancelEvent: SongLoadCancelEvent, registered: Boolean) {
        songLoadHandler = handler
        synchronized(mLoadingSongFileSync) {
            val existingCancelEvent = songLoadCancelEvent
            existingCancelEvent?.set()
            songLoadCancelEvent = vSongLoadCancelEvent
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