package com.stevenfrew.beatprompter.ui.pref

import android.os.Message

interface SettingsEventHandler {
	fun handleMessage(msg: Message)
	fun obtainMessage(msg: Int): Message
}