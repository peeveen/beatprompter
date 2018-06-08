package com.stevenfrew.beatprompter.cloud.dropbox;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.Utils;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResultType;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.function.*;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class DropboxCloudStorage implements CloudStorage {
    private final static String DROPBOX_CACHE_FOLDER_NAME="dropbox";
    private final static String DROPBOX_APP_KEY = "hay1puzmg41f02r";

    private static final Set<String> EXTENSIONS_TO_DOWNLOAD = new HashSet<>(Arrays.asList(
            "txt", "mp3", "wav", "m4a", "aac", "ogg", "png","jpg","bmp","tif","tiff","jpeg","jpe","pcx"));

    private List<CloudFileInfo> mFilesToDownload;
    private Activity mParentActivity;
    private File mDropboxFolder;
    private String mFolderToSearch;
    private boolean mIncludeSubfolders;

    private PublishSubject<String> mProgressMessageSource=PublishSubject.create();
    private PublishSubject<CloudDownloadResult> mDownloadResultsSource=PublishSubject.create();
    private PublishSubject<CloudFileInfo> mFolderSearchResultSource=PublishSubject.create();
    private PublishSubject<CloudFolderInfo> mFolderSelectionResultSource=PublishSubject.create();

    public DropboxCloudStorage(Activity parentActivity)
    {
        mParentActivity=parentActivity;
        mDropboxFolder=new File(SongList.mBeatPrompterSongFilesFolder,DROPBOX_CACHE_FOLDER_NAME);
        if(!mDropboxFolder.exists())
            if(!mDropboxFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG,"Failed to create Dropbox sync folder.");
    }

    private void dropboxDo(Consumer<DbxClientV2> function)
    {
        SharedPreferences sharedPrefs = SongList.mSongListInstance.getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
        String storedAccessToken = sharedPrefs.getString(SongList.mSongListInstance.getString(R.string.pref_dropboxAccessToken_key), null);
        if (storedAccessToken == null) {
            // Did we authenticate last time it failed?
            storedAccessToken = Auth.getOAuth2Token();
            if (storedAccessToken != null)
                sharedPrefs.edit().putString(SongList.getContext().getString(R.string.pref_dropboxAccessToken_key), storedAccessToken).apply();
        }
        if (storedAccessToken == null) {
            Auth.startOAuth2Authentication(mParentActivity, DROPBOX_APP_KEY);
        }
        else {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BeatPrompterApplication.APP_NAME)
                    .build();
            function.accept(new DbxClientV2(requestConfig, storedAccessToken));
        }
    }

    private void _downloadFiles(DbxClientV2 client)
    {
        for(CloudFileInfo file:mFilesToDownload) {
            CloudDownloadResult result;
            try {
                Metadata mdata = client.files().getMetadata(file.mStorageID);
                if ((mdata != null) && (mdata instanceof FileMetadata)) {
                    FileMetadata fmdata = (FileMetadata) mdata;
                    String title = fmdata.getName();
                    Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                    mProgressMessageSource.onNext(String.format(SongList.getContext().getString(R.string.checking), title));
                    String safeFilename = Utils.makeSafeFilename(title);
                    File targetFile = new File(mDropboxFolder, safeFilename);
                    Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

                    Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                    mProgressMessageSource.onNext(String.format(SongList.getContext().getString(R.string.downloading), title));
                    // Don't check lastModified ... ALWAYS download.
                    File localFile = downloadDropboxFile(client, fmdata, targetFile);
                    result=new CloudDownloadResult(file, localFile);
                } else
                    result=new CloudDownloadResult(file, CloudDownloadResultType.NoLongerExists);
                mDownloadResultsSource.onNext(result);
            } catch (Exception e) {
                mDownloadResultsSource.onError(e);
            }
        }
        mDownloadResultsSource.onComplete();
    }

    private File downloadDropboxFile(DbxClientV2 client,FileMetadata file, File localfile) throws IOException, DbxException
    {
        FileOutputStream fos =null;
        try {
            fos = new FileOutputStream(localfile);
            DbxDownloader<FileMetadata> downloader=client.files().download(file.getId());
            downloader.download(fos);
        }
        finally
        {
            if(fos!=null)
                try {
                    fos.close();
                }
                catch(Exception eee)
                {
                    Log.e(BeatPrompterApplication.TAG,"Failed to close file",eee);
                }
        }
        return localfile;
    }

    @Override
    public void downloadFiles(List<CloudFileInfo> filesToDownload) {
        mFilesToDownload=filesToDownload;
        dropboxDo(this::_downloadFiles);
    }

    @Override
    public String getCloudStorageName() {
        return SongList.mSongListInstance.getString(R.string.dropbox_string);
    }

    @Override
    public CloudType getCloudStorageType() {
        return CloudType.Dropbox;
    }

    private void _getFolderContents(DbxClientV2 client)
    {
        List<String> folderIDs=new ArrayList<>();
        folderIDs.add(mFolderToSearch);
        List<String> folderNames=new ArrayList<>();
        folderNames.add("");

        while(!folderIDs.isEmpty())
        {
            String currentFolderID=folderIDs.remove(0);
            String currentFolderName=folderNames.remove(0);
            try
            {
                Log.d(BeatPrompterApplication.TAG, "Getting list of everything in Dropbox folder.");
                ListFolderResult listResult = client.files().listFolder(currentFolderID);
                while(listResult!=null)
                {
                    List<Metadata> entries=listResult.getEntries();
                    for(Metadata mdata:entries)
                    {
                        if(mdata instanceof FileMetadata)
                        {
                            FileMetadata fmdata=(FileMetadata)mdata;
                            String filename=fmdata.getName().toLowerCase();
                            if(isSuitableFileToDownload(filename))
                                mFolderSearchResultSource.onNext(new CloudFileInfo(fmdata.getId(), fmdata.getName(), fmdata.getServerModified(),currentFolderName));
                        }
                        else if((mdata instanceof FolderMetadata) && (mIncludeSubfolders))
                        {
                            Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                            folderIDs.add(mdata.getPathLower());
                            folderNames.add(mdata.getName());
                        }
                    }
                    if(listResult.getHasMore())
                        listResult=client.files().listFolderContinue(listResult.getCursor());
                    else
                        listResult=null;
                }
            }
            catch(DbxException de)
            {
                mFolderSearchResultSource.onError(de);
            }
        }
        mFolderSearchResultSource.onComplete();
    }

    private boolean isSuitableFileToDownload(String filename)
    {
        return EXTENSIONS_TO_DOWNLOAD.contains(FilenameUtils.getExtension(filename));
    }

    @Override
    public void readFolderContents(String folderID, boolean includeSubfolders) {
        mFolderToSearch=folderID;
        mIncludeSubfolders=includeSubfolders;
        dropboxDo(this::_getFolderContents);
    }

    @Override
    public Observable<String> getProgressMessageSource() {
        return mProgressMessageSource;
    }

    @Override
    public Observable<CloudDownloadResult> getDownloadResultSource()
    {
        return mDownloadResultsSource;
    }

    @Override
    public Observable<CloudFileInfo> getFolderContentsSource()
    {
        return mFolderSearchResultSource;
    }

    @Override
    public Observable<CloudFolderInfo> getFolderSelectionSource()
    {
        return mFolderSelectionResultSource;
    }

    private void _selectFolder(DbxClientV2 client)
    {
        DropboxChooseFolderDialog dialog=new DropboxChooseFolderDialog(mParentActivity,client);
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

    @Override
    public void selectFolder()
    {
        dropboxDo(this::_selectFolder);
    }
}
