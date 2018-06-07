package com.stevenfrew.beatprompter;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class CloudDownloadTask extends AsyncTask<String, String, Boolean>
{
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private Exception mCloudSyncException=null;
    private String mCloudPath;
    private boolean mIncludeSubFolders;
    private CachedCloudFileCollection mCurrentCache;
    private String mSubfolderOrigin;
    private List<CloudFileInfo> mFilesToUpdate;
    File mTargetFolder;
    private ArrayList<MIDIAlias> mDefaultMIDIAliases;
    private CachedCloudFile mUpdatedFile=null;
    private CloudStorage mCloudStorage;
    private List<CloudFileInfo> mCloudFilesFound=new ArrayList<>();
    private List<CloudFileInfo> mCloudFilesToDownload=new ArrayList<>();

    private ArrayList<SongFile> mDownloadedSongs=new ArrayList<>();
    private ArrayList<MIDIAliasCachedCloudFile> mDownloadedMIDIAliasCachedCloudFiles=new ArrayList<>();
    private ArrayList<SetListFile> mDownloadedSets=new ArrayList<>();
    ArrayList<AudioFile> mDownloadedAudioFiles=new ArrayList<>();
    ArrayList<ImageFile> mDownloadedImageFiles=new ArrayList<>();

    CloudDownloadTask(CloudStorage cloudStorage,File targetFolder,Handler handler, String cloudPath, boolean includeSubFolders,CachedCloudFileCollection currentCache,ArrayList<MIDIAlias> defaultMIDIAliases,ArrayList<CachedCloudFile> filesToUpdate)
    {
        mCurrentCache=currentCache;

        mCloudStorage=cloudStorage;
        mCloudStorage.getProgressMessageSource().subscribe(this::publishProgress);
        mCloudStorage.getFolderContentsSource().subscribe(this::onCloudFileFound,this::onErrorSearchingFolder,this::onFolderSearchComplete);
        mCloudStorage.getDownloadResultSource().subscribe(this::onCloudFileDownloadComplete,this::onErrorDownloadingCloudFile,this::onAllDownloadsComplete);

        mTargetFolder=targetFolder;
        mIncludeSubFolders=includeSubFolders;

        mHandler=handler;

        mCloudPath=cloudPath;
        mDefaultMIDIAliases=defaultMIDIAliases;
        mSubfolderOrigin=null;

        if(filesToUpdate==null)
            mFilesToUpdate=null;
        else {
            mFilesToUpdate = filesToUpdate.stream().map(ftu -> new CloudFileInfo(ftu.mStorageID, ftu.mTitle, ftu.mLastModified, ftu.mSubfolder)).collect(Collectors.toList());
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

    void updateDownloadProgress(String filename)
    {
        publishProgress(String.format(SongList.getContext().getString(R.string.downloading),filename));
    }

    /*boolean onFileDownloaded(File file, String fileID, Date lastModified, boolean dependencyFile)
    {
        if(mUpdateType==CloudFileType.Song)
        {
            if (dependencyFile)
                return true;
            try {
                mUpdatedFile = new SongFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin), null,null);
                return true;
            } catch (Exception ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        else if(mUpdateType==CloudFileType.SetList) {
            try {
                mUpdatedFile = new SetListFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin));
                return true;
            } catch (InvalidBeatPrompterFileException ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        else if(mUpdateType==CloudFileType.MIDIAliases)
        {
            try {
                mUpdatedFile = new MIDIAliasCachedCloudFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin),mDefaultMIDIAliases);
                return true;
            } catch (InvalidBeatPrompterFileException ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        return false;
    }*/

    void onCloudFileFound(CloudFileInfo cloudFileInfo)
    {
        mCloudFilesFound.add(cloudFileInfo);
        if(!mCurrentCache.hasLatestVersionOf(cloudFileInfo))
            mCloudFilesToDownload.add(cloudFileInfo);
    }

    void onErrorSearchingFolder(Throwable e)
    {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, e.getMessage()).sendToTarget();
    }

    void onFolderSearchComplete()
    {
        mCloudStorage.downloadFiles(mCloudFilesToDownload);
    }

    void onCloudFileDownloadComplete(CloudDownloadResult downloadResult)
    {
        // TODO: deal with "not found", or "error".
        if(downloadResult.mResultType==CloudDownloadResultType.Succeeded)
            mCurrentCache.add(CachedCloudFile.createCachedCloudFile(downloadResult));
    }

    void onErrorDownloadingCloudFile(Throwable e)
    {
        mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, e.getMessage()).sendToTarget();
    }

    void onAllDownloadsComplete()
    {
        if(!isRefreshingFiles()) {
            // TODO: if performing a full update, remove all items from mCurrentCache that were not in mCloudFilesFound
        }
        mHandler.obtainMessage(BeatPrompterApplication.CACHE_UPDATED,mCurrentCache).sendToTarget();
    }
}