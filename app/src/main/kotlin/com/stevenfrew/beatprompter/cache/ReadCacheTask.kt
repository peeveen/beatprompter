package com.stevenfrew.beatprompter.cache

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.BuildConfig
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
) : CoroutineTask<Unit, Pair<String?, Boolean>, Boolean> {
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main
	private var progressDialog: Dialog? = null
	private var errorOccurred = false

	override fun onError(t: Throwable) {
		errorOccurred = true
		handler.obtainMessage(Events.DATABASE_READ_ERROR, t.message).sendToTarget()
	}

	private fun closeProgressDialog() = progressDialog?.dismiss()

	override fun doInBackground(
		params: Unit,
		progressUpdater: suspend (Pair<String?, Boolean>) -> Unit
	): Boolean {
		val databaseReadListener = object : CacheReadListener {
			override fun onItemRead(cachedFile: CachedItem) =
				Cache.cachedCloudItems.add(cachedFile)

			override fun onCacheReadError(t: Throwable) {
				BeatPrompter.addDebugMessage("Cache read error: ${t.message}")
				onError(t)
				onCacheReadComplete()
			}

			override fun onCacheReadComplete() {
				BeatPrompter.addDebugMessage("Sending CACHE_UPDATED to SongList")
				handler.obtainMessage(
					Events.CACHE_UPDATED,
					Cache.cachedCloudItems
				).sendToTarget()
				BeatPrompter.addDebugMessage("Sent CACHE_UPDATED to SongList")
				closeProgressDialog()
			}

			override suspend fun onProgressMessageReceived(message: Pair<String?, Boolean>) =
				progressUpdater(message)
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
				findViewById<TextView>(R.id.versionInfo)?.text = BeatPrompter.appResources.getString(
					R.string.versionInfo,
					BuildConfig.VERSION_NAME,
					BuildConfig.VERSION_CODE
				)
				setCancelable(false)
				show()
			}
		}
	}

	override fun onProgressUpdate(progress: Pair<String?, Boolean>) {
		if (progress.first != null)
			progressDialog!!.findViewById<TextView>(R.id.readDatabaseProgress)?.text = progress.first
		if (progress.second)
			progressDialog!!.findViewById<TextView>(R.id.readingOrRebuilding)?.text =
				BeatPrompter.appResources.getString(R.string.rebuildingDatabase)
	}

	override fun onPostExecute(result: Boolean) {
		BeatPrompter.addDebugMessage("ReadCacheTask.onPostExecute()")
		initialDatabaseReadHasBeenPerformed = true
		onComplete(result)
		BeatPrompter.addDebugMessage("ReadCacheTask.onPostExecute(), onComplete() called")
		closeProgressDialog()
	}

	companion object {
		private var initialDatabaseReadHasBeenPerformed: Boolean = false
	}
}