package com.stevenfrew.beatprompter.cloud;

import android.app.Activity;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.demo.DemoCloudStorage;
import com.stevenfrew.beatprompter.cloud.dropbox.DropboxCloudStorage;
import com.stevenfrew.beatprompter.cloud.googledrive.GoogleDriveCloudStorage;
import com.stevenfrew.beatprompter.cloud.onedrive.OneDriveCloudStorage;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

public abstract class CloudStorage {
    protected Activity mParentActivity;
    protected CloudCacheFolder mCloudCacheFolder;

    protected CloudStorage(Activity parentActivity,String cloudCacheFolderName)
    {
        mParentActivity=parentActivity;
        mCloudCacheFolder=new CloudCacheFolder(SongList.mBeatPrompterSongFilesFolder,cloudCacheFolderName);
        if(!mCloudCacheFolder.exists())
            if(!mCloudCacheFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG,"Failed to create cloud cache folder.");
    }
    public String constructFullPath(String folderPath,String itemName)
    {
        String fullPath = folderPath;
        if (!fullPath.endsWith(getDirectorySeparator()))
            fullPath += getDirectorySeparator();
        return fullPath +itemName;
    }

    public static CloudStorage getInstance(CloudType cloudType,Activity parentActivity)
    {
        if (cloudType == CloudType.Dropbox)
            return new DropboxCloudStorage(parentActivity);
        if (cloudType == CloudType.OneDrive)
            return new OneDriveCloudStorage(parentActivity);
        if (cloudType == CloudType.GoogleDrive)
            return new GoogleDriveCloudStorage(parentActivity);
        return new DemoCloudStorage(parentActivity);
    }

    public void downloadFiles(List<CloudFileInfo> filesToRefresh,CloudItemDownloadListener listener)
    {
        for(CloudDownloadResult defaultCloudDownload:SongList.mDefaultCloudDownloads)
            if(filesToRefresh.contains(defaultCloudDownload.mCloudFileInfo))
                filesToRefresh.remove(defaultCloudDownload.mCloudFileInfo);

        CompositeDisposable disp=new CompositeDisposable();
        PublishSubject<CloudDownloadResult> downloadSource=PublishSubject.create();
        disp.add(downloadSource.subscribe(listener::onItemDownloaded,listener::onDownloadError,listener::onDownloadComplete));
        PublishSubject<String> messageSource=PublishSubject.create();
        disp.add(messageSource.subscribe(listener::onProgressMessageReceived));
        try {
            // Always include the temporary set list and default midi alias files.
            for(CloudDownloadResult defaultCloudDownload:SongList.mDefaultCloudDownloads)
                downloadSource.onNext(defaultCloudDownload);
            downloadFiles(filesToRefresh, listener,downloadSource, messageSource);
        }
        finally
        {
            // TODO: figure out how/when to dispose of this
//            disp.dispose();
        }
    }

    public void readFolderContents(CloudFolderInfo folder, CloudFolderSearchListener listener, boolean includeSubfolders,boolean returnFolders)
    {
        CompositeDisposable disp=new CompositeDisposable();
        PublishSubject<CloudItemInfo> folderContentsSource=PublishSubject.create();
        disp.add(folderContentsSource.subscribe(listener::onCloudItemFound,listener::onFolderSearchError,listener::onFolderSearchComplete));
        try {
            for(CloudDownloadResult defaultCloudDownload:SongList.mDefaultCloudDownloads)
                folderContentsSource.onNext(defaultCloudDownload.mCloudFileInfo);
            readFolderContents(folder, listener,folderContentsSource, includeSubfolders, returnFolders);
        }
        finally
        {
            // TODO: figure out how/when to dispose of this
//            disp.dispose();
        }
    }

    public void selectFolder(Activity parentActivity,CloudFolderSelectionListener listener)
    {
        try {
            getRootPath(new CloudRootPathListener() {
                @Override
                public void onRootPathFound(CloudFolderInfo rootPath) {
                    ChooseCloudFolderDialog dialog = new ChooseCloudFolderDialog(parentActivity, CloudStorage.this, listener, rootPath);
                    dialog.showDialog();
                }

                @Override
                public void onRootPathError(Throwable t) {
                    listener.onFolderSelectedError(t);
                }

                @Override
                public void onAuthenticationRequired() {
                    listener.onAuthenticationRequired();
                }

                @Override
                public boolean shouldCancel() {
                    return listener.shouldCancel();
                }
            });
        }
        catch(Exception e)
        {
            listener.onFolderSelectedError(e);
        }
    }

    private void getRootPath(CloudRootPathListener listener)
    {
        CompositeDisposable disp=new CompositeDisposable();
        PublishSubject<CloudFolderInfo> rootPathSource=PublishSubject.create();
        disp.add(rootPathSource.subscribe(listener::onRootPathFound,listener::onRootPathError));
        try {
            getRootPath(listener,rootPathSource);
        }
        finally
        {
            // TODO: figure out how/when to dispose of this
//            disp.dispose();
        }
    }

    public CloudCacheFolder getCacheFolder()
    {
        return mCloudCacheFolder;
    }

    public abstract String getCloudStorageName();

    public abstract String getDirectorySeparator();

    public abstract int getCloudIconResourceId();

    protected abstract void getRootPath(CloudListener listener,PublishSubject<CloudFolderInfo> rootPathSource);

    protected abstract void downloadFiles(List<CloudFileInfo> filesToRefresh,CloudListener cloudListener, PublishSubject<CloudDownloadResult> itemSource,PublishSubject<String> messageSource);

    protected abstract void readFolderContents(CloudFolderInfo folder, CloudListener listener,PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders,boolean returnFolders);

}