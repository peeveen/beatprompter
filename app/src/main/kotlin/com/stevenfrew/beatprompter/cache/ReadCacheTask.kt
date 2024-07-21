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
	private val mContext: Context,
	private val mHandler: Handler,
	private val mOnComplete: (Boolean) -> Unit
) : CoroutineTask<Unit, String, Boolean> {
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main
	private var mProgressDialog: Dialog? = null
	private var mErrorOccurred = false

	override fun onError(t: Throwable) {
		mErrorOccurred = true
		mHandler.obtainMessage(Events.DATABASE_READ_ERROR, t.message).sendToTarget()
	}

	private fun closeProgressDialog() = mProgressDialog?.dismiss()

	override fun doInBackground(params: Unit, progressUpdater: suspend (String) -> Unit): Boolean {
		val databaseReadListener = object : CacheReadListener {
			override fun onItemRead(cachedFile: CachedItem) =
				Cache.mCachedCloudItems.add(cachedFile)

			override fun onCacheReadError(t: Throwable) {
				onError(t)
				onCacheReadComplete()
			}

			override fun onCacheReadComplete() {
				mHandler.obtainMessage(
					Events.CACHE_UPDATED,
					Cache.mCachedCloudItems
				).sendToTarget()
				closeProgressDialog()
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)
		}
		return if (mInitialDatabaseReadHasBeenPerformed) {
			databaseReadListener.onCacheReadComplete()
			true
		} else Cache.readDatabase(databaseReadListener)
	}

	override fun onPreExecute() {
		if (!mInitialDatabaseReadHasBeenPerformed) {
			val title = BeatPrompter.appResources.getString(R.string.readingDatabase)
			mProgressDialog = Dialog(mContext, R.style.ReadingDatabaseDialog).apply {
				setTitle(title)
				setContentView(R.layout.reading_database)
				setCancelable(false)
				show()
			}
		}
	}

	override fun onProgressUpdate(progress: String) {
		mProgressDialog!!.findViewById<TextView>(R.id.readDatabaseProgress)?.text = progress
	}

	override fun onPostExecute(result: Boolean) {
		mInitialDatabaseReadHasBeenPerformed = true
		mOnComplete(result)
		closeProgressDialog()
	}

	companion object {
		private var mInitialDatabaseReadHasBeenPerformed: Boolean = false
	}
}