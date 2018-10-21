package com.stevenfrew.beatprompter.song.load

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.AsyncTask
import com.stevenfrew.beatprompter.ui.SongListActivity

class SongLoadUITask(private val mLoadJob:SongLoadJob): AsyncTask<String, Int, Boolean>() {
    private var mProgressDialog:ProgressDialog?=null
    private var mProgressTitle=""

    override fun onPreExecute() {
        super.onPreExecute()
        if(!isCancelled)
            mProgressDialog = ProgressDialog(SongListActivity.mSongListInstance).apply {
                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                setMessage(mLoadJob.mSongLoadInfo.mSongFile.mTitle)
                max = mLoadJob.mSongLoadInfo.mSongFile.mLines
                isIndeterminate = false
                setCancelable(false)
                setButton(DialogInterface.BUTTON_NEGATIVE, Resources.getSystem().getString(android.R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                    mLoadJob.stopLoading()
                }
                show()
            }
    }

    override fun onCancelled() {
        super.onCancelled()
        mProgressDialog?.dismiss()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        if (values.size > 1) {
            mProgressDialog!!.apply {
                setMessage(mProgressTitle)
                max = values[1]!!
                progress = values[0]!!
            }
        }
    }

    override fun doInBackground(vararg params: String?): Boolean {
        mLoadJob.waitForCompletion()
        return true
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        mProgressDialog?.dismiss()
    }

    internal fun updateProgress(message:String,currentProgress:Int,max:Int)
    {
        mProgressTitle=message
        publishProgress(currentProgress,max)
    }
}