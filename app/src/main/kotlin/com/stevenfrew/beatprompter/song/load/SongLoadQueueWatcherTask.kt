package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ui.SongDisplayActivity

object SongLoadQueueWatcherTask : Task(true) {
	private val songLoadLock = Any()
	private var loadingSong: SongLoadJob? = null
	private var songToLoad: SongLoadJob? = null
	private var songToLoadOnResume: SongLoadJob? = null
	private val nextSongToLoad: SongLoadJob?
		get() = synchronized(songLoadLock) {
			val stl = songToLoad
			songToLoad = null
			stl
		}
	val hasASongToLoad: Boolean
		get() = synchronized(songLoadLock) {
			songToLoad != null || songToLoadOnResume != null
		}
	val isLoadingASong: Boolean
		get() = synchronized(songLoadLock) {
			loadingSong != null
		}

	fun isAlreadyLoadingSong(songFile: SongFile): Boolean =
		songToLoad?.songLoadInfo?.songFile?.id == songFile.id
			|| loadingSong?.songLoadInfo?.songFile?.id == songFile.id
			|| SongLoadJob.mLoadedSong?.loadJob?.songLoadInfo?.songFile?.id == songFile.id

	override fun doWork() {
		synchronized(songLoadLock) {
			val songToLoad = nextSongToLoad
			if (songToLoad != null) {
				loadingSong = songToLoad
				Logger.logLoader { "Found a song to load: ${songToLoad.songLoadInfo.songFile.title}" }
				synchronized(songToLoad)
				{
					songToLoad.startLoading()
				}
				return
			}
		}
		try {
			Thread.sleep(250)
		} catch (interruptedException: InterruptedException) {
			// Thread was interrupted in order to be paused or stopped.
		}
	}

	fun onSongLoadFinished() =
		synchronized(songLoadLock) {
			loadingSong = null
		}

	override fun stop(): Boolean =
		stopCurrentLoads().let {
			super.stop()
		}

	private fun stopCurrentLoads() =
		synchronized(songLoadLock) {
			if (songToLoadOnResume != null) {
				Logger.logLoader { "Removing an unstarted load-on-resume from the queue: ${songToLoadOnResume!!.songLoadInfo.songFile.title}" }
				songToLoadOnResume = null
			}
			if (songToLoad != null) {
				Logger.logLoader { "Removing an unstarted load from the queue: ${songToLoad!!.songLoadInfo.songFile.title}" }
				songToLoad = null
			}
			if (loadingSong != null) {
				Logger.logLoader { "Cancelling started load: ${loadingSong!!.songLoadInfo.songFile.title}" }
				loadingSong!!.stopLoading()
				loadingSong = null
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
				synchronized(songLoadLock)
				{
					Logger.logLoader { "Adding a song to the load queue: ${loadJob.songLoadInfo.songFile.title}" }
					songToLoadOnResume = null
					songToLoad = loadJob
				}
			}

			SongInterruptResult.CannotInterrupt -> loadJob.stopLoading()
			SongInterruptResult.CanInterrupt -> {
				Logger.logLoader("CanInterrupt ... song will load when activity finishes.")
			}

			SongInterruptResult.SongAlreadyLoaded -> return
		}
	}

	fun onResume() =
		synchronized(songLoadLock)
		{
			if (songToLoadOnResume != null) {
				val loadJob = songToLoadOnResume!!
				songToLoadOnResume = null
				loadSong(loadJob)
			}
		}

	fun setSongToLoadOnResume(songToLoadOnResume: SongLoadJob?) =
		synchronized(songLoadLock) {
			this.songToLoadOnResume = songToLoadOnResume
		}
}