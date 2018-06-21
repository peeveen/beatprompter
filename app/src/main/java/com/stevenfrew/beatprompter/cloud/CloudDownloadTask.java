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

public class CloudDownloadTask extends AsyncTask<String, String, Boolean> implements CloudFolderSearchListener,CloudItemDownloadListener
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

        mIncludeSubFolders=includeSubFolders;
        mHandler=handler;
        mCloudPath=cloudPath;
        mSubfolderOrigin=null;

        if(filesToUpdate==null)
            mFilesToUpdate=null;
        else {
            mFilesToUpdate = filesToUpdate.stream().map(ftu -> new CloudFileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolder)).collect(Collectors.toList());
            if(!mFilesToUpdate.isEmpty())
                mSubfolderOrigin=mFilesToUpdate.get(0).mSubfolder;
        }
    }

    private boolean isRefreshingSelectedFiles()
    {
        return mFilesToUpdate!=null && !mFilesToUpdate.isEmpty();
    }

    @Override
    protected Boolean doInBackground(String... paramParams) {

        if(isRefreshingSelectedFiles())
            updateSelectedFiles();
        else
            updateEntireCache();
        return true;
    }

    private void updateEntireCache()
    {
        mCloudStorage.readFolderContents(new CloudFolderInfo(mCloudPath),this,mIncludeSubFolders,false);
    }

    private void updateSelectedFiles()
    {
        mCloudStorage.downloadFiles(mFilesToUpdate,this);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mProgressDialog.setMessage(values[0]);
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

    @Override
    public void onCloudItemFound(CloudItemInfo cloudItem) {
        // We're only interested in downloading files.
        if(cloudItem instanceof CloudFileInfo) {
            CloudFileInfo cloudFileInfo=(CloudFileInfo)cloudItem;
            mCloudFilesFound.add(cloudFileInfo);
            if (!SongList.mCachedCloudFiles.hasLatestVersionOf(cloudFileInfo))
                mCloudFilesToDownload.add(cloudFileInfo);
        }
    }

    @Override
    public void onFolderSearchError(Throwable t) {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, t.getMessage()).sendToTarget();
    }

    public void onFolderSearchComplete()
    {
        mCloudStorage.downloadFiles(mCloudFilesToDownload,this);
    }

    @Override
    public void onItemDownloaded(CloudDownloadResult downloadResult) {
        if(downloadResult.mResultType==CloudDownloadResultType.Succeeded)
            SongList.mCachedCloudFiles.add(CachedCloudFile.createCachedCloudFile(downloadResult));
        else// IMPLICIT if(downloadResult.mResultType==CloudDownloadResultType.NoLongerExists)
            SongList.mCachedCloudFiles.remove(downloadResult.mCloudFileInfo);
    }

    @Override
    public void onProgressMessageReceived(String message) {
        publishProgress(message);
    }

    @Override
    public void onDownloadError(Throwable t) {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, t.getMessage()).sendToTarget();
        onDownloadComplete();
    }

    @Override
    public void onDownloadComplete() {
        if(!isRefreshingSelectedFiles())
            SongList.mCachedCloudFiles.removeNonExistent(mCloudFilesFound.stream().map(c->c.mID).collect(Collectors.toSet()));
        mHandler.obtainMessage(BeatPrompterApplication.CACHE_UPDATED,SongList.mCachedCloudFiles).sendToTarget();
        if (mProgressDialog!=null)
            mProgressDialog.dismiss();
    }

    @Override
    public void onAuthenticationRequired() {
        this.cancel(true);
    }

    @Override
    public boolean shouldCancel() {
        return isCancelled();
    }
}