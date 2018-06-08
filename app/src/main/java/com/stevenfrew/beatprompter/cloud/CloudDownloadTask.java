package com.stevenfrew.beatprompter.cloud;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.cache.CachedCloudFile;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CloudDownloadTask extends AsyncTask<String, String, Boolean>
{
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private String mCloudPath;
    private boolean mIncludeSubFolders;
    private String mSubfolderOrigin;
    private List<CloudFileInfo> mFilesToUpdate;
    private CloudStorage mCloudStorage;
    private List<CloudFileInfo> mCloudFilesFound=new ArrayList<>();
    private List<CloudFileInfo> mCloudFilesToDownload=new ArrayList<>();

    public CloudDownloadTask(CloudStorage cloudStorage,Handler handler, String cloudPath, boolean includeSubFolders,ArrayList<CachedCloudFile> filesToUpdate)
    {
        mCloudStorage=cloudStorage;
        mCloudStorage.getProgressMessageSource().subscribe(this::publishProgress);
        mCloudStorage.getFolderContentsSource().subscribe(this::onCloudFileFound,this::onErrorSearchingFolder,this::onFolderSearchComplete);
        mCloudStorage.getDownloadResultSource().subscribe(this::onCloudFileDownloadComplete,this::onErrorDownloadingCloudFile,this::onAllDownloadsComplete);

        mIncludeSubFolders=includeSubFolders;
        mHandler=handler;
        mCloudPath=cloudPath;
        mSubfolderOrigin=null;

        if(filesToUpdate==null)
            mFilesToUpdate=null;
        else {
            mFilesToUpdate = filesToUpdate.stream().map(ftu -> new CloudFileInfo(ftu.mStorageID, ftu.mName, ftu.mLastModified, ftu.mSubfolder)).collect(Collectors.toList());
            if(!mFilesToUpdate.isEmpty())
                mSubfolderOrigin=mFilesToUpdate.get(0).mSubfolder;
        }
    }

    private boolean isRefreshingFiles()
    {
        return mFilesToUpdate!=null && !mFilesToUpdate.isEmpty();
    }

    @Override
    protected Boolean doInBackground(String... paramParams) {

        if(isRefreshingFiles())
            updateSelectedFiles();
        else
            updateEntireCache();
        return true;
    }

    private void updateEntireCache()
    {
        mCloudStorage.readFolderContents(mCloudPath,mIncludeSubFolders);
    }

    private void updateSelectedFiles()
    {
        mCloudStorage.downloadFiles(mFilesToUpdate);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mProgressDialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean b) {
        super.onPostExecute(b);
        if (mProgressDialog!=null)
            mProgressDialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(SongList.getContext());
        mProgressDialog.setTitle(SongList.getContext().getString(R.string.downloadingFiles));
        mProgressDialog.setMessage(String.format(SongList.getContext().getString(R.string.accessingCloudStorage),mCloudStorage.getCloudStorageName()));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    private void onCloudFileFound(CloudFileInfo cloudFileInfo)
    {
        mCloudFilesFound.add(cloudFileInfo);
        if(!SongList.mCachedCloudFiles.hasLatestVersionOf(cloudFileInfo))
            mCloudFilesToDownload.add(cloudFileInfo);
    }

    private void onErrorSearchingFolder(Throwable e)
    {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, e.getMessage()).sendToTarget();
    }

    private void onFolderSearchComplete()
    {
        mCloudStorage.downloadFiles(mCloudFilesToDownload);
    }

    private void onCloudFileDownloadComplete(CloudDownloadResult downloadResult)
    {
        // TODO: deal with "not found", or "error".
        if(downloadResult.mResultType==CloudDownloadResultType.Succeeded)
            SongList.mCachedCloudFiles.add(CachedCloudFile.createCachedCloudFile(downloadResult));
    }

    private void onErrorDownloadingCloudFile(Throwable e)
    {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, e.getMessage()).sendToTarget();
    }

    private void onAllDownloadsComplete()
    {
        if(!isRefreshingFiles()) {
            SongList.mCachedCloudFiles.removeNonExistent(mCloudFilesFound.stream().map(c->c.mStorageID).collect(Collectors.toSet()));
        }
        mHandler.obtainMessage(BeatPrompterApplication.CACHE_UPDATED,SongList.mCachedCloudFiles).sendToTarget();
    }
}