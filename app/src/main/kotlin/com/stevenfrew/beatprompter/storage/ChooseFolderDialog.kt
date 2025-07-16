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
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * Dialog allowing the user to choose the folder to use as the source for data.
 */
internal class ChooseFolderDialog(
	private val activity: Activity,
	private val storage: Storage,
	listener: FolderSelectionListener,
	private var currentFolder: FolderInfo
) : DialogInterface.OnCancelListener, DialogInterface.OnDismissListener, FolderSearchListener {
	private var folderSearchError: Throwable? = null
	private val dialog: Dialog = Dialog(activity, R.style.FolderBrowserDialog)
	private val folderSelectionEventSubscription: CompositeDisposable
	private var parentFolder: FolderInfo? = null
	private val handler: FolderContentsFetchHandler
	private var folderFetcher: FolderFetcherTask? = null
	private val folderSelectionSource = PublishSubject.create<FolderInfo?>()
	private val displayItems = ArrayList<ItemInfo>()
	private var shouldCancel: Boolean = false

	internal class FolderContentsFetchHandler(private var chooseFolderDialog: ChooseFolderDialog) :
		Handler() {
		override fun handleMessage(msg: Message) {
			@Suppress("UNCHECKED_CAST")
			when (msg.what) {
				Events.FOLDER_CONTENTS_FETCHED -> chooseFolderDialog.populateBrowser(msg.obj as MutableList<ItemInfo>)
				Events.FOLDER_CONTENTS_FETCHING -> chooseFolderDialog.updateProgress(msg.arg1, msg.arg2)
			}
		}
	}

	init {
		val thisDialog = this
		dialog.apply {
			requestWindowFeature(Window.FEATURE_LEFT_ICON)
			setContentView(R.layout.choose_folder_dialog_loading)
			setCanceledOnTouchOutside(false)
			setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, storage.cloudIconResourceId)
			dialog.setOnCancelListener(thisDialog)
			dialog.setOnDismissListener(thisDialog)
		}

		handler = FolderContentsFetchHandler(this)
		folderSelectionEventSubscription = CompositeDisposable().apply {
			add(
				folderSelectionSource.subscribe(
					{ listener.onFolderSelected(it) },
					{ listener.onFolderSelectedError(it, activity) },
					{ listener.onFolderSelectionComplete() })
			)
		}

		dialog.findViewById<TextView>(android.R.id.title).apply {
			ellipsize = TextUtils.TruncateAt.END
			setLines(1)
			setHorizontallyScrolling(true)
		}
	}

	fun showDialog() {
		refresh(currentFolder)
		dialog.apply {
			setTitle(getDisplayPath(currentFolder))
			show()
		}
	}

	private fun setNewPath(newFolder: FolderInfo) {
		folderSelectionSource.onNext(newFolder)
		dialog.dismiss()
	}

	private fun refresh(folder: FolderInfo?) {
		if (folder == null)
			return
		currentFolder = folder
		parentFolder = folder.parentFolder
		cancelFolderFetcher()
		displayItems.clear()
		folderFetcher = FolderFetcherTask(storage, this)
		folderFetcher!!.execute(currentFolder)
	}

	private fun populateBrowser(contents: MutableList<ItemInfo>?) {
		if (contents == null)
			dialog.dismiss()
		else {
			contents.removeAll(Cache.defaultDownloads.map { it.fileInfo })
			contents.sort()

			currentFolder.parentFolder?.also {
				contents.add(0, FolderInfo(it.parentFolder, it.id, PARENT_DIR, it.displayPath))
			}

			// refresh the user interface
			dialog.apply {
				setContentView(R.layout.choose_folder_dialog)
				setTitle(getDisplayPath(currentFolder))
				val list = findViewById<ListView>(R.id.chooseFolderListView)
				list.adapter = BrowserItemListAdapter(contents, context)

				list.setOnItemClickListener { _, _, which, _ ->
					val folderChosen: FolderInfo = if (which == 0 && parentFolder != null)
						parentFolder as FolderInfo
					else
						list.getItemAtPosition(which) as FolderInfo

					setContentView(R.layout.choose_folder_dialog_loading)
					setTitle(getDisplayPath(folderChosen))
					refresh(folderChosen)
				}
				val okButton = findViewById<Button>(R.id.chooseFolderOkButton)
				if (folderSearchError == null)
					okButton.setOnClickListener { setNewPath(currentFolder) }
				else
					okButton.isEnabled = false
			}
		}
	}

	private fun getDisplayPath(folder: FolderInfo): String {
		if (folder.parentFolder != null) {
			var parentPath = getDisplayPath(folder.parentFolder)
			if (!parentPath.endsWith(storage.directorySeparator))
				parentPath += storage.directorySeparator
			return parentPath + folder.name
		}
		return folder.name
	}

	private fun updateProgress(found: Int, max: Int) {
		val progressText = dialog.findViewById<TextView>(R.id.loading_count)
		if (progressText != null)
			if (max == 0) {
				val itemsFound = activity.getString(R.string.itemsFound)
				progressText.text = String.format(Locale.getDefault(), itemsFound, found)
			} else {
				val itemsFound = activity.getString(R.string.itemsFoundWithMax)
				progressText.text = String.format(Locale.getDefault(), itemsFound, found, max)
			}
	}

	override fun onDismiss(dialog: DialogInterface) {
		cancelFolderFetcher()
		folderSelectionSource.onComplete()
		folderSelectionEventSubscription.dispose()
	}

	override fun onCancel(dialog: DialogInterface) {
		onDismiss(dialog)
	}

	private fun cancelFolderFetcher() {
		if (folderFetcher != null)
			try {
				folderFetcher!!.cancel("Cancelling folder fetcher ...")
			} catch (_: IllegalStateException) {
				// Fetcher wasn't running, so nothing to cancel.
			}
		folderFetcher = null
	}

	class FolderFetcherTask internal constructor(
		private val storage: Storage,
		private val folderSearchListener: FolderSearchListener
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

		override fun doInBackground(params: FolderInfo, progressUpdater: suspend (Unit) -> Unit) =
			storage.readFolderContents(params, folderSearchListener, recurseSubFolders = false)
	}

	override fun onCloudItemFound(item: ItemInfo) {
		displayItems.add(item)
	}

	override fun onFolderSearchError(t: Throwable, context: Context) {
		activity.runOnUiThread {
			Utils.showExceptionDialog(t, context)
		}
		folderSearchError = t
		onFolderSearchComplete()
	}

	override fun onFolderSearchComplete() {
		displayItems.sort()
		handler.obtainMessage(Events.FOLDER_CONTENTS_FETCHED, displayItems).sendToTarget()
	}

	override suspend fun onProgressMessageReceived(message: String) {
		// Do nothing.
	}

	override fun shouldCancel(): Boolean = shouldCancel

	companion object {
		private const val PARENT_DIR = ".."
	}
}