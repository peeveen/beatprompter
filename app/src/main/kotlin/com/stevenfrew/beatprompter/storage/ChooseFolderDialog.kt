package com.stevenfrew.beatprompter.storage

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.Window
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListFragment
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Dialog allowing the user to choose the folder to use as the source for data.
 */
internal class ChooseFolderDialog(
	private val mActivity: Activity,
	private val mStorage: Storage,
	listener: FolderSelectionListener,
	private var mCurrentFolder: FolderInfo
) : DialogInterface.OnCancelListener, DialogInterface.OnDismissListener, FolderSearchListener {
	private var mFolderSearchError: Throwable? = null
	private val mDialog: Dialog = Dialog(mActivity, R.style.CustomDialog)
	private val mFolderSelectionEventSubscription: CompositeDisposable
	private var mParentFolder: FolderInfo? = null
	private val mHandler: FolderContentsFetchHandler
	private var mFolderFetcher: FolderFetcherTask? = null
	private val mFolderSelectionSource = PublishSubject.create<FolderInfo?>()
	private val mDisplayItems = ArrayList<ItemInfo>()
	private var mShouldCancel: Boolean = false

	internal class FolderContentsFetchHandler(private var mChooseFolderDialog: ChooseFolderDialog) :
		Handler() {
		override fun handleMessage(msg: Message) {
			@Suppress("UNCHECKED_CAST")
			when (msg.what) {
				Events.FOLDER_CONTENTS_FETCHED -> mChooseFolderDialog.populateBrowser(msg.obj as MutableList<ItemInfo>)
				Events.FOLDER_CONTENTS_FETCHING -> mChooseFolderDialog.updateProgress(msg.arg1, msg.arg2)
			}
		}
	}

	init {
		val thisDialog = this
		mDialog.apply {
			requestWindowFeature(Window.FEATURE_LEFT_ICON)
			setContentView(R.layout.choose_folder_dialog_loading)
			setCanceledOnTouchOutside(false)
			setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, mStorage.cloudIconResourceId)
			mDialog.setOnCancelListener(thisDialog)
			mDialog.setOnDismissListener(thisDialog)
		}

		mHandler = FolderContentsFetchHandler(this)
		mFolderSelectionEventSubscription = CompositeDisposable().apply {
			add(
				mFolderSelectionSource.subscribe(
					{ listener.onFolderSelected(it) },
					{ listener.onFolderSelectedError(it, mActivity) },
					{ listener.onFolderSelectionComplete() })
			)
		}

		mDialog.findViewById<TextView>(android.R.id.title).apply {
			ellipsize = TextUtils.TruncateAt.END
			setLines(1)
			setHorizontallyScrolling(true)
		}
	}

	fun showDialog() {
		refresh(mCurrentFolder)
		mDialog.apply {
			setTitle(getDisplayPath(mCurrentFolder))
			show()
		}
	}

	private fun setNewPath(newFolder: FolderInfo) {
		mFolderSelectionSource.onNext(newFolder)
		mDialog.dismiss()
	}

	private fun refresh(folder: FolderInfo?) {
		if (folder == null)
			return
		mCurrentFolder = folder
		mParentFolder = folder.mParentFolder
		cancelFolderFetcher()
		mDisplayItems.clear()
		mFolderFetcher = FolderFetcherTask(mStorage, this)
		mFolderFetcher!!.execute(mCurrentFolder)
	}

	private fun populateBrowser(contents: MutableList<ItemInfo>?) {
		if (contents == null)
			mDialog.dismiss()
		else {
			contents.removeAll(SongListFragment.mDefaultDownloads.map { it.mFileInfo })
			contents.sort()

			mCurrentFolder.mParentFolder?.also {
				contents.add(0, FolderInfo(it.mParentFolder, it.mID, PARENT_DIR, it.mDisplayPath))
			}

			// refresh the user interface
			mDialog.apply {
				setContentView(R.layout.choose_folder_dialog)
				setTitle(getDisplayPath(mCurrentFolder))
				val list = findViewById<ListView>(R.id.chooseFolderListView)
				list.adapter = BrowserItemListAdapter(contents)

				list.setOnItemClickListener { _, _, which, _ ->
					val folderChosen: FolderInfo = if (which == 0 && mParentFolder != null)
						mParentFolder as FolderInfo
					else
						list.getItemAtPosition(which) as FolderInfo

					setContentView(R.layout.choose_folder_dialog_loading)
					setTitle(getDisplayPath(folderChosen))
					refresh(folderChosen)
				}
				val okButton = findViewById<Button>(R.id.chooseFolderOkButton)
				if (mFolderSearchError == null)
					okButton.setOnClickListener { setNewPath(mCurrentFolder) }
				else
					okButton.isEnabled = false
			}
		}
	}

	private fun getDisplayPath(folder: FolderInfo): String {
		if (folder.mParentFolder != null) {
			var parentPath = getDisplayPath(folder.mParentFolder)
			if (!parentPath.endsWith(mStorage.directorySeparator))
				parentPath += mStorage.directorySeparator
			return parentPath + folder.mName
		}
		return folder.mName
	}

	private fun updateProgress(found: Int, max: Int) {
		val progressText = mDialog.findViewById<TextView>(R.id.loading_count)
		if (progressText != null)
			if (max == 0) {
				val itemsFound = mActivity.getString(R.string.itemsFound)
				progressText.text = String.format(Locale.getDefault(), itemsFound, found)
			} else {
				val itemsFound = mActivity.getString(R.string.itemsFoundWithMax)
				progressText.text = String.format(Locale.getDefault(), itemsFound, found, max)
			}
	}

	override fun onDismiss(dialog: DialogInterface) {
		cancelFolderFetcher()
		mFolderSelectionSource.onComplete()
		mFolderSelectionEventSubscription.dispose()
	}

	override fun onCancel(dialog: DialogInterface) {
		onDismiss(dialog)
	}

	private fun cancelFolderFetcher() {
		if (mFolderFetcher != null)
			try {
				mFolderFetcher!!.cancel("Cancelling folder fetcher ...")
			} catch (e: IllegalStateException) {
				// Fetcher wasn't running, so nothing to cancel.
			}
		mFolderFetcher = null
	}

	class FolderFetcherTask internal constructor(
		private val mStorage: Storage,
		private val mFolderSearchListener: FolderSearchListener
	) : CoroutineTask<FolderInfo, Unit, Unit> {
		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO

		override fun onPreExecute() {
			// Do nothing.
		}

		override fun onError(t: Throwable) {
			// Listener will receive error callbacks.
		}

		override fun onProgressUpdate(progress: Unit) {
			// No progress updating required.
		}

		override fun onPostExecute(result: Unit) {
			// Do nothing. Listener will receive a completion callback.
		}

		override fun doInBackground(params: FolderInfo, progressUpdater: suspend (Unit) -> Unit) {
			return mStorage.readFolderContents(params, mFolderSearchListener, recurseSubFolders = false)
		}
	}

	override fun onCloudItemFound(item: ItemInfo) {
		mDisplayItems.add(item)
	}

	override fun onFolderSearchError(t: Throwable, context: Context) {
		mActivity.runOnUiThread {
			Utils.showExceptionDialog(t, context)
		}
		mFolderSearchError = t
		onFolderSearchComplete()
	}

	override fun onFolderSearchComplete() {
		mDisplayItems.sort()
		mHandler.obtainMessage(Events.FOLDER_CONTENTS_FETCHED, mDisplayItems).sendToTarget()
	}

	override suspend fun onProgressMessageReceived(message: String) {
		// Do nothing.
	}

	override fun onAuthenticationRequired() {
		// Cancel the dialog.
		mDialog.cancel()
		mShouldCancel = true
	}

	override fun shouldCancel(): Boolean {
		return mShouldCancel
	}

	companion object {
		private const val PARENT_DIR = ".."
	}
}