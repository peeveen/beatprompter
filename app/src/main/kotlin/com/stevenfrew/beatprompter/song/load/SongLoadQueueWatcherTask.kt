package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ui.SongDisplayActivity

object SongLoadQueueWatcherTask : Task(true) {
	private val mSongLoadLock = Any()
	private var mLoadingSong: SongLoadJob? = null
	private var mSongToLoad: SongLoadJob? = null
	private var mSongToLoadOnResume: SongLoadJob? = null
	private val nextSongToLoad: SongLoadJob?
		get() = synchronized(mSongLoadLock)
		{
			val stl = mSongToLoad
			mSongToLoad = null
			stl
		}
	val hasASongToLoad: Boolean
		get() = synchronized(mSongLoadLock)
		{
			mSongToLoad != null || mSongToLoadOnResume != null
		}
	val isLoadingASong: Boolean
		get() = synchronized(mSongLoadLock)
		{
			mLoadingSong != null
		}

	fun isAlreadyLoadingSong(songFile: SongFile): Boolean {
		return mSongToLoad?.mSongLoadInfo?.mSongFile?.mID == songFile.mID
			|| mLoadingSong?.mSongLoadInfo?.mSongFile?.mID == songFile.mID
			|| SongLoadJob.mLoadedSong?.mLoadJob?.mSongLoadInfo?.mSongFile?.mID == songFile.mID
	}

	override fun doWork() {
		synchronized(mSongLoadLock)
		{
			val songToLoad = nextSongToLoad
			if (songToLoad != null) {
				mLoadingSong = songToLoad
				Logger.logLoader { "Found a song to load: ${songToLoad.mSongLoadInfo.mSongFile.mTitle}" }
				synchronized(songToLoad)
				{
					songToLoad.startLoading()
				}
				return
			}
		}
		Thread.sleep(250)
	}

	fun onSongLoadFinished() {
		synchronized(mSongLoadLock)
		{
			mLoadingSong = null
		}
	}

	override fun stop() {
		stopCurrentLoads()
		super.stop()
	}

	private fun stopCurrentLoads() {
		synchronized(mSongLoadLock)
		{
			if (mSongToLoadOnResume != null) {
				Logger.logLoader { "Removing an unstarted load-on-resume from the queue: ${mSongToLoadOnResume!!.mSongLoadInfo.mSongFile.mTitle}" }
				mSongToLoadOnResume = null
			}
			if (mSongToLoad != null) {
				Logger.logLoader { "Removing an unstarted load from the queue: ${mSongToLoad!!.mSongLoadInfo.mSongFile.mTitle}" }
				mSongToLoad = null
			}
			if (mLoadingSong != null) {
				Logger.logLoader { "Cancelling started load: ${mLoadingSong!!.mSongLoadInfo.mSongFile.mTitle}" }
				mLoadingSong!!.stopLoading()
				mLoadingSong = null
			}
		}
	}

	fun loadSong(loadJob: SongLoadJob) {
		stopCurrentLoads()

		// If the song-display activity is currently active, then try to interrupt
		// the current song with this one. If not possible, don't bother.
		// A result of CannotInterrupt means that the current song refuses to stop. In which case, we can't load.
		// A result of CanInterrupt means that the current song has been instructed to end and, once it has, it will load the new one.
		// A result of NoSongToInterrupt, however, means full steam ahead.
		when (SongDisplayActivity.interruptCurrentSong(loadJob)) {
			SongInterruptResult.NoSongToInterrupt -> {
				synchronized(mSongLoadLock)
				{
					Logger.logLoader { "Adding a song to the load queue: ${loadJob.mSongLoadInfo.mSongFile.mTitle}" }
					mSongToLoadOnResume = null
					mSongToLoad = loadJob
				}
			}

			SongInterruptResult.CannotInterrupt -> loadJob.stopLoading()
			SongInterruptResult.CanInterrupt -> {
				Logger.logLoader("CanInterrupt ... song will load when activity finishes.")
			}

			SongInterruptResult.SongAlreadyLoaded -> return
		}
	}

	fun onResume() {
		synchronized(mSongLoadLock)
		{
			if (mSongToLoadOnResume != null) {
				val loadJob = mSongToLoadOnResume!!
				mSongToLoadOnResume = null
				loadSong(loadJob)
			}
		}
	}

	fun setSongToLoadOnResume(songToLoadOnResume: SongLoadJob?) {
		synchronized(mSongLoadLock)
		{
			mSongToLoadOnResume = songToLoadOnResume
		}
	}
}