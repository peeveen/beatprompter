package com.stevenfrew.beatprompter.cache

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.CoroutineTask
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Task that reads the song database, and parses the song files.
 */
class ReadCacheTask(
	private val context: Context,
	private val handler: Handler,
	private val onComplete: (Boolean) -> Unit
) : CoroutineTask<Unit, String, Boolean> {
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main
	private var progressDialog: Dialog? = null
	private var errorOccurred = false

	override fun onError(t: Throwable) {
		errorOccurred = true
		handler.obtainMessage(Events.DATABASE_READ_ERROR, t.message).sendToTarget()
	}

	private fun closeProgressDialog() = progressDialog?.dismiss()

	override fun doInBackground(params: Unit, progressUpdater: suspend (String) -> Unit): Boolean {
		val databaseReadListener = object : CacheReadListener {
			override fun onItemRead(cachedFile: CachedItem) =
				Cache.cachedCloudItems.add(cachedFile)

			override fun onCacheReadError(t: Throwable) {
				onError(t)
				onCacheReadComplete()
			}

			override fun onCacheReadComplete() {
				handler.obtainMessage(
					Events.CACHE_UPDATED,
					Cache.cachedCloudItems
				).sendToTarget()
				closeProgressDialog()
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)
		}
		return if (initialDatabaseReadHasBeenPerformed) {
			databaseReadListener.onCacheReadComplete()
			true
		} else Cache.readDatabase(databaseReadListener)
	}

	override fun onPreExecute() {
		if (!initialDatabaseReadHasBeenPerformed) {
			val title = BeatPrompter.appResources.getString(R.string.readingDatabase)
			progressDialog = Dialog(context, R.style.ReadingDatabaseDialog).apply {
				setTitle(title)
				setContentView(R.layout.reading_database)
				setCancelable(false)
				show()
			}
		}
	}

	override fun onProgressUpdate(progress: String) {
		progressDialog!!.findViewById<TextView>(R.id.readDatabaseProgress)?.text = progress
	}

	override fun onPostExecute(result: Boolean) {
		initialDatabaseReadHasBeenPerformed = true
		onComplete(result)
		closeProgressDialog()
	}

	companion object {
		private var initialDatabaseReadHasBeenPerformed: Boolean = false
	}
}