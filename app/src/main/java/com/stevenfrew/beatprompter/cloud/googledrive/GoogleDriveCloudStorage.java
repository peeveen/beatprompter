package com.stevenfrew.beatprompter.cloud.googledrive;

import android.app.Activity;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudCacheFolder;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudItemInfo;
import com.stevenfrew.beatprompter.cloud.CloudListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;

import java.io.File;
import java.util.List;

import io.reactivex.subjects.PublishSubject;

public class GoogleDriveCloudStorage extends CloudStorage {
    private final static String GOOGLE_DRIVE_ROOT_FOLDER_ID="root";
    private final static String GOOGLE_DRIVE_ROOT_PATH="/";
    private static final String GOOGLE_DRIVE_CACHE_FOLDER_NAME="google_drive";

    private Activity mParentActivity;
    private CloudCacheFolder mGoogleDriveFolder;

    public GoogleDriveCloudStorage(Activity parentActivity)
    {
        mParentActivity=parentActivity;
        mGoogleDriveFolder=new CloudCacheFolder(SongList.mBeatPrompterSongFilesFolder,GOOGLE_DRIVE_CACHE_FOLDER_NAME);
        if(!mGoogleDriveFolder.exists())
            if(!mGoogleDriveFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG,"Failed to create Google Drive sync folder.");
    }

    @Override
    public String getCloudStorageName() {
        return SongList.mSongListInstance.getString(R.string.google_drive_string);
    }

    @Override
    public CloudType getCloudStorageType() {
        return CloudType.GoogleDrive;
    }

    @Override
    protected void getRootPath(CloudListener listener,PublishSubject<CloudFolderInfo> rootPathSource)
    {
        rootPathSource.onNext(new CloudFolderInfo(GOOGLE_DRIVE_ROOT_FOLDER_ID,GOOGLE_DRIVE_ROOT_PATH,GOOGLE_DRIVE_ROOT_PATH));
    }

    @Override
    public String getDirectorySeparator() {
        return "/";
    }

    @Override
    public int getCloudIconResourceId() {
        return R.drawable.ic_google_drive;
    }


    @Override
    public CloudCacheFolder getCacheFolder()
    {
        return mGoogleDriveFolder;
    }

    @Override
    protected void downloadFiles(List<CloudFileInfo> filesToRefresh, CloudListener cloudListener, PublishSubject<CloudDownloadResult> itemSource, PublishSubject<String> messageSource) {

    }

    @Override
    protected void readFolderContents(CloudFolderInfo folder, CloudListener listener, PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders, boolean returnFolders) {

    }
}
