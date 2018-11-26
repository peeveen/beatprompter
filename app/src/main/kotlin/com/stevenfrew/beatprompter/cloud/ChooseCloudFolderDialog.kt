package com.stevenfrew.beatprompter.cloud

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.Window
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

internal class ChooseCloudFolderDialog(private val mActivity: Activity, private val mCloudStorage: CloudStorage, listener: CloudFolderSelectionListener, private var mCurrentFolder: CloudFolderInfo) : DialogInterface.OnCancelListener, DialogInterface.OnDismissListener, CloudFolderSearchListener {

    private val mDialog: Dialog = Dialog(mActivity, R.style.CustomDialog)
    private val mFolderSelectionEventSubscription: CompositeDisposable
    private var mParentFolder: CloudFolderInfo? = null
    private val mHandler: FolderContentsFetchHandler
    private var mFolderFetcher: FolderFetcherTask? = null
    private val mFolderSelectionSource = PublishSubject.create<CloudFolderInfo>()
    private val mDisplayItems = ArrayList<CloudItemInfo>()

    internal class FolderContentsFetchHandler(private var mChooseFolderDialog: ChooseCloudFolderDialog) : Handler() {
        override fun handleMessage(msg: Message) {
            @Suppress("UNCHECKED_CAST")
            when (msg.what) {
                EventHandler.FOLDER_CONTENTS_FETCHED -> mChooseFolderDialog.populateBrowser(msg.obj as MutableList<CloudItemInfo>)
                EventHandler.FOLDER_CONTENTS_FETCHING -> mChooseFolderDialog.updateProgress(msg.arg1, msg.arg2)
            }
        }
    }

    init {
        mDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON)
        mDialog.setContentView(R.layout.choose_folder_dialog_loading)
        mDialog.setCanceledOnTouchOutside(false)
        mDialog.setOnCancelListener(this)
        mDialog.setOnDismissListener(this)
        val tv = mDialog.findViewById<TextView>(android.R.id.title)
        tv.ellipsize = TextUtils.TruncateAt.END
        tv.setLines(1)
        tv.setHorizontallyScrolling(true)
        mFolderSelectionEventSubscription = CompositeDisposable()
        mFolderSelectionEventSubscription.add(mFolderSelectionSource.subscribe({ listener.onFolderSelected(it) }, { listener.onFolderSelectedError(it) }))
        mDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, mCloudStorage.cloudIconResourceId)
        mHandler = FolderContentsFetchHandler(this)
    }

    fun showDialog() {
        refresh(mCurrentFolder)
        mDialog.setTitle(getDisplayPath(mCurrentFolder))
        mDialog.show()
    }

    private fun setNewPath(newFolder: CloudFolderInfo?) {
        if (newFolder != null)
            mFolderSelectionSource.onNext(newFolder)
        mDialog.dismiss()
    }

    private fun refresh(folder: CloudFolderInfo?) {
        if (folder == null)
            return
        this.mCurrentFolder = folder
        this.mParentFolder = folder.mParentFolder
        cancelFolderFetcher()
        mDisplayItems.clear()
        mFolderFetcher = FolderFetcherTask(mCloudStorage, this)
        mFolderFetcher!!.execute(mCurrentFolder)
    }

    private fun populateBrowser(contents: MutableList<CloudItemInfo>?) {
        if (contents == null)
            mDialog.dismiss()
        else {
            contents.removeAll(SongListActivity.mDefaultCloudDownloads.map { it.mCloudFileInfo })
            contents.sort()

            val parentFolder = mCurrentFolder.mParentFolder
            if (parentFolder != null)
                contents.add(0, CloudFolderInfo(parentFolder.mParentFolder, parentFolder.mID, PARENT_DIR, parentFolder.mDisplayPath))

            // refresh the user interface
            mDialog.setContentView(R.layout.choose_folder_dialog)
            val list = mDialog.findViewById<ListView>(R.id.chooseFolderListView)
            val okButton = mDialog.findViewById<Button>(R.id.chooseFolderOkButton)
            mDialog.setTitle(getDisplayPath(mCurrentFolder))
            list.adapter = CloudBrowserItemListAdapter(contents)

            list.setOnItemClickListener { _, _, which, _ ->
                val folderChosen: CloudFolderInfo = if (which == 0 && mParentFolder != null)
                    mParentFolder as CloudFolderInfo
                else
                    list.getItemAtPosition(which) as CloudFolderInfo

                mDialog.setContentView(R.layout.choose_folder_dialog_loading)
                mDialog.setTitle(getDisplayPath(folderChosen))
                refresh(folderChosen)
            }
            okButton.setOnClickListener { _ -> setNewPath(mCurrentFolder) }
        }
    }

    private fun getDisplayPath(folder: CloudFolderInfo): String {
        if (folder.mParentFolder != null) {
            var parentPath = getDisplayPath(folder.mParentFolder!!)
            if (!parentPath.endsWith(mCloudStorage.directorySeparator))
                parentPath += mCloudStorage.directorySeparator
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
        cancelFolderFetcher()
        mFolderSelectionSource.onComplete()
        mFolderSelectionEventSubscription.dispose()
    }

    private fun cancelFolderFetcher() {
        if (mFolderFetcher != null)
            mFolderFetcher!!.cancel(true)
        mFolderFetcher = null
    }

    class FolderFetcherTask internal constructor(private var mCloudStorage: CloudStorage, private var mCloudFolderSearchListener: CloudFolderSearchListener) : AsyncTask<CloudFolderInfo, Void, Void>() {
        override fun doInBackground(vararg args: CloudFolderInfo): Void? {
            val folderToSearch = args[0]
            mCloudStorage.readFolderContents(folderToSearch, mCloudFolderSearchListener, false, true)
            return null
        }
    }

    override fun onCloudItemFound(cloudItem: CloudItemInfo) {
        mDisplayItems.add(cloudItem)
    }

    override fun onFolderSearchError(t: Throwable) {
        Toast.makeText(mActivity, t.message, Toast.LENGTH_LONG).show()
        onFolderSearchComplete()
    }

    override fun onFolderSearchComplete() {
        mDisplayItems.sort()
        mHandler.obtainMessage(EventHandler.FOLDER_CONTENTS_FETCHED, mDisplayItems).sendToTarget()
    }

    override fun onProgressMessageReceived(message: String) {
        // Do nothing.
    }

    override fun onAuthenticationRequired() {
        // Cancel the dialog.
        mDialog.cancel()
    }

    override fun shouldCancel(): Boolean {
        return mFolderFetcher != null && mFolderFetcher!!.isCancelled
    }

    companion object {
        private const val PARENT_DIR = ".."
    }
}