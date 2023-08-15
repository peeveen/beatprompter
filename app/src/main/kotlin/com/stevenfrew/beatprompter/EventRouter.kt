package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.ui.SongDisplayActivity
import com.stevenfrew.beatprompter.ui.SongListFragment
import com.stevenfrew.beatprompter.ui.pref.SettingsEventHandler

object EventRouter {
	private val mSongListEventHandlerLock = Any()
	private val mSongDisplayEventHandlerLock = Any()
	private val mSettingsEventHandlerLock = Any()
	private var mSongListEventHandler: SongListFragment.SongListEventHandler? = null
	private var mSongDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler? = null
	private var mSettingsEventHandler: SettingsEventHandler? = null

	fun setSongListEventHandler(songListEventHandler: SongListFragment.SongListEventHandler?) {
		synchronized(mSongListEventHandlerLock) {
			mSongListEventHandler = songListEventHandler
		}
	}

	fun setSongDisplayEventHandler(songDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler) {
		synchronized(mSongDisplayEventHandlerLock) {
			mSongDisplayEventHandler = songDisplayEventHandler
		}
	}

	fun setSettingsEventHandler(settingsEventHandler: SettingsEventHandler?) {
		synchronized(mSettingsEventHandlerLock) {
			mSettingsEventHandler = settingsEventHandler
		}
	}

	fun sendEventToSongList(event: Int) {
		synchronized(mSongListEventHandlerLock) {
			if (mSongListEventHandler != null)
				mSongListEventHandler!!.obtainMessage(event).sendToTarget()
		}
	}

	fun sendEventToSongDisplay(event: Int) {
		synchronized(mSongDisplayEventHandlerLock) {
			if (mSongDisplayEventHandler != null)
				mSongDisplayEventHandler!!.obtainMessage(event).sendToTarget()
		}
	}

	fun sendEventToSettings(event: Int) {
		synchronized(mSettingsEventHandlerLock) {
			if (mSettingsEventHandler != null)
				mSettingsEventHandler!!.obtainMessage(event).sendToTarget()
		}
	}

	fun sendEventToSongList(event: Int, arg: Any) {
		synchronized(mSongListEventHandlerLock) {
			if (mSongListEventHandler != null)
				mSongListEventHandler!!.obtainMessage(event, arg).sendToTarget()
		}
	}

	fun sendEventToSongList(event: Int, arg1: Int, arg2: Int) {
		synchronized(mSongListEventHandlerLock) {
			if (mSongListEventHandler != null)
				mSongListEventHandler!!.obtainMessage(event, arg1, arg2).sendToTarget()
		}
	}

	fun sendEventToSongDisplay(event: Int, arg: Any) {
		synchronized(mSongDisplayEventHandlerLock) {
			if (mSongDisplayEventHandler != null)
				mSongDisplayEventHandler!!.obtainMessage(event, arg).sendToTarget()
		}
	}

	fun sendEventToSongDisplay(event: Int, arg1: Int, arg2: Int) {
		synchronized(mSongDisplayEventHandlerLock) {
			if (mSongDisplayEventHandler != null)
				mSongDisplayEventHandler!!.obtainMessage(event, arg1, arg2).sendToTarget()
		}
	}
}
