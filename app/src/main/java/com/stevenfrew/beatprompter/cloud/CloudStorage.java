package com.stevenfrew.beatprompter.cloud;

import android.app.Activity;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

public abstract class CloudStorage {
    String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    public void downloadFiles(List<CloudFileInfo> filesToRefresh,CloudItemDownloadListener listener)
    {
        CompositeDisposable disp=new CompositeDisposable();
        PublishSubject<CloudDownloadResult> downloadSource=PublishSubject.create();
        disp.add(downloadSource.subscribe(listener::onItemDownloaded,listener::onDownloadError,listener::onDownloadComplete));
        PublishSubject<String> messageSource=PublishSubject.create();
        disp.add(messageSource.subscribe(listener::onProgressMessageReceived));
        try {
            downloadFiles(filesToRefresh, listener,downloadSource, messageSource);
        }
        finally
        {
            disp.dispose();
        }
    }

    public void readFolderContents(CloudFolderInfo folder, CloudFolderSearchListener listener, boolean includeSubfolders,boolean returnFolders)
    {
        CompositeDisposable disp=new CompositeDisposable();
        PublishSubject<CloudItemInfo> folderContentsSource=PublishSubject.create();
        disp.add(folderContentsSource.subscribe(listener::onCloudItemFound,listener::onFolderSearchError,listener::onFolderSearchComplete));
        try {
            readFolderContents(folder, listener,folderContentsSource, includeSubfolders, returnFolders);
        }
        finally
        {
            disp.dispose();
        }
    }

    public void selectFolder(Activity parentActivity,CloudFolderSelectionListener listener)
    {
        ChooseCloudFolderDialog dialog=new ChooseCloudFolderDialog(parentActivity,this,listener);
        dialog.showDialog();
    }

    public abstract String getCloudStorageName();

    public abstract CloudType getCloudStorageType();

    public abstract CloudFolderInfo getRootPath();

    public abstract String getDirectorySeparator();

    public abstract int getCloudIconResourceId();

    protected abstract void downloadFiles(List<CloudFileInfo> filesToRefresh,CloudListener cloudListener, PublishSubject<CloudDownloadResult> itemSource,PublishSubject<String> messageSource);

    protected abstract void readFolderContents(CloudFolderInfo folder, CloudListener listener,PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders,boolean returnFolders);
}