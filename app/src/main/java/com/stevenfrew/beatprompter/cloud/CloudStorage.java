package com.stevenfrew.beatprompter.cloud;

import android.app.Activity;

import com.dropbox.core.v2.DbxClientV2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public abstract class CloudStorage {
    String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    private PublishSubject<CloudFolderInfo> mFolderSelectionResultSource=PublishSubject.create();

    public abstract void downloadFiles(List<CloudFileInfo> filesToRefresh);

    public abstract String getCloudStorageName();

    public abstract CloudType getCloudStorageType();

    public abstract void readFolderContents(CloudFolderInfo folder, boolean includeSubfolders,boolean returnFolders);

    public abstract Observable<String> getProgressMessageSource();

    public abstract Observable<CloudDownloadResult> getDownloadResultSource();

    public abstract Observable<CloudItemInfo> getFolderContentsSource();

    public Observable<CloudFolderInfo> getFolderSelectionSource()
    {
        return mFolderSelectionResultSource;
    }

    public abstract CloudFolderInfo getRootPath();

    public abstract String getDirectorySeparator();

    public abstract int getCloudIconResourceId();

    public void selectFolder(Activity parentActivity)
    {
        ChooseCloudFolderDialog dialog=new ChooseCloudFolderDialog(parentActivity,this);
        dialog.getFolderSelectionSource().subscribe(this::onFolderSelected,this::onFolderSelectedError,this::onFolderSelectedComplete);
        dialog.showDialog();
    }

    private void onFolderSelected(CloudFolderInfo folderInfo)
    {
        mFolderSelectionResultSource.onNext(folderInfo);
    }

    private void onFolderSelectedError(Throwable t)
    {
        mFolderSelectionResultSource.onError(t);
    }

    private void onFolderSelectedComplete()
    {
        mFolderSelectionResultSource.onComplete();
    }


}