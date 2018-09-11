package com.stevenfrew.beatprompter

import android.os.Handler
import com.stevenfrew.beatprompter.pref.SettingsFragment

abstract class EventHandler : Handler() {
    companion object {
        private const val HANDLER_MESSAGE_BASE_ID = 1834739585

        const val BLUETOOTH_CHOOSE_SONG = HANDLER_MESSAGE_BASE_ID
        const val CLIENT_CONNECTED = HANDLER_MESSAGE_BASE_ID + 1
        const val SERVER_CONNECTED = HANDLER_MESSAGE_BASE_ID + 2
        const val MIDI_START_SONG = HANDLER_MESSAGE_BASE_ID + 3
        const val MIDI_CONTINUE_SONG = HANDLER_MESSAGE_BASE_ID + 4
        const val MIDI_STOP_SONG = HANDLER_MESSAGE_BASE_ID + 5
        const val END_SONG = HANDLER_MESSAGE_BASE_ID + 6
        const val CLOUD_SYNC_ERROR = HANDLER_MESSAGE_BASE_ID + 7
        const val FOLDER_CONTENTS_FETCHING = HANDLER_MESSAGE_BASE_ID + 8
        const val FOLDER_CONTENTS_FETCHED = HANDLER_MESSAGE_BASE_ID + 9
        const val CLIENT_DISCONNECTED = HANDLER_MESSAGE_BASE_ID + 10
        const val MIDI_SONG_SELECT = HANDLER_MESSAGE_BASE_ID + 11
        const val MIDI_PROGRAM_CHANGE = HANDLER_MESSAGE_BASE_ID + 12
        const val SONG_LOAD_CANCELLED = HANDLER_MESSAGE_BASE_ID + 13
        const val SONG_LOAD_FAILED = HANDLER_MESSAGE_BASE_ID + 14
        const val SERVER_DISCONNECTED = HANDLER_MESSAGE_BASE_ID + 15
        const val SONG_LOAD_LINE_PROCESSED = HANDLER_MESSAGE_BASE_ID + 16
        const val SONG_LOAD_COMPLETED = HANDLER_MESSAGE_BASE_ID + 17
        const val MIDI_SET_SONG_POSITION = HANDLER_MESSAGE_BASE_ID + 18
        const val CACHE_UPDATED = HANDLER_MESSAGE_BASE_ID + 19
        const val SET_CLOUD_PATH = HANDLER_MESSAGE_BASE_ID + 20
        const val CLEAR_CACHE = HANDLER_MESSAGE_BASE_ID + 21
        const val BLUETOOTH_PAUSE_ON_SCROLL_START = HANDLER_MESSAGE_BASE_ID + 22
        const val BLUETOOTH_QUIT_SONG = HANDLER_MESSAGE_BASE_ID + 23
        const val BLUETOOTH_SET_SONG_TIME = HANDLER_MESSAGE_BASE_ID + 24
        const val BLUETOOTH_TOGGLE_START_STOP = HANDLER_MESSAGE_BASE_ID + 25

        private val mSongListEventHandlerLock = Any()
        private val mSongDisplayEventHandlerLock = Any()
        private val mSettingsEventHandlerLock = Any()
        private var mSongListEventHandler: SongList.SongListEventHandler? = null
        private var mSongDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler? = null
        private var mSettingsEventHandler: Handler? = null

        fun setSongListEventHandler(songListEventHandler: SongList.SongListEventHandler?) {
            synchronized(mSongListEventHandlerLock) {
                mSongListEventHandler = songListEventHandler
            }
        }

        fun setSongDisplayEventHandler(songDisplayEventHandler: SongDisplayActivity.SongDisplayEventHandler) {
            synchronized(mSongDisplayEventHandlerLock) {
                mSongDisplayEventHandler = songDisplayEventHandler
            }
        }

        fun setSettingsEventHandler(settingsEventHandler: SettingsFragment.SettingsEventHandler?) {
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
}
