package com.stevenfrew.beatprompter.storage

import android.content.Intent

/**
 * Interface for any storage that provides editing facilities.
 */
interface EditableStorage {
	fun getEditIntent(id: String): Intent
}