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
import com.stevenfrew.beatprompter.cloud.CloudItemInfo;
import com.stevenfrew.beatprompter.cloud.CloudListener;
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

import io.reactivex.subjects.PublishSubject;

public class DropboxCloudStorage extends CloudStorage {

    public interface DropboxAction
    {
        void onConnected(DbxClientV2 client);
        void onAuthenticationRequired();
    }

    private final static String DROPBOX_CACHE_FOLDER_NAME="dropbox";
    private final static String DROPBOX_APP_KEY = "hay1puzmg41f02r";
    private final static String DROPBOX_ROOT_PATH="/";

    private static final Set<String> EXTENSIONS_TO_DOWNLOAD = new HashSet<>(Arrays.asList(
            "txt", "mp3", "wav", "m4a", "aac", "ogg", "png","jpg","bmp","tif","tiff","jpeg","jpe","pcx"));

    private Activity mParentActivity;
    private File mDropboxFolder;

    public DropboxCloudStorage(Activity parentActivity)
    {
        mParentActivity=parentActivity;
        mDropboxFolder=new File(SongList.mBeatPrompterSongFilesFolder,DROPBOX_CACHE_FOLDER_NAME);
        if(!mDropboxFolder.exists())
            if(!mDropboxFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG,"Failed to create Dropbox sync folder.");
    }

    private void downloadFiles(DbxClientV2 client, CloudListener listener,PublishSubject<CloudDownloadResult> itemSource, PublishSubject<String> messageSource, List<CloudFileInfo> filesToDownload)
    {
        for(CloudFileInfo file:filesToDownload) {
            if(listener.shouldCancel())
                break;
            CloudDownloadResult result;
            try {
                Metadata mdata = client.files().getMetadata(file.mID);
                if ((mdata != null) && (mdata instanceof FileMetadata)) {
                    FileMetadata fmdata = (FileMetadata) mdata;
                    String title = fmdata.getName();
                    Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                    messageSource.onNext(String.format(SongList.getContext().getString(R.string.checking), title));
                    String safeFilename = Utils.makeSafeFilename(title);
                    File targetFile = new File(mDropboxFolder, safeFilename);
                    Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

                    Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                    messageSource.onNext(String.format(SongList.getContext().getString(R.string.downloading), title));
                    // Don't check lastModified ... ALWAYS download.
                    if(listener.shouldCancel())
                        break;
                    File localFile = downloadDropboxFile(client, fmdata, targetFile);
                    result=new CloudDownloadResult(file, localFile);
                } else
                    result=new CloudDownloadResult(file, CloudDownloadResultType.NoLongerExists);
                itemSource.onNext(result);
                if(listener.shouldCancel())
                    break;
            } catch (Exception e) {
                itemSource.onError(e);
            }
        }
        itemSource.onComplete();
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
    public String getCloudStorageName() {
        return SongList.mSongListInstance.getString(R.string.dropbox_string);
    }

    @Override
    public CloudType getCloudStorageType() {
        return CloudType.Dropbox;
    }

    private boolean isSuitableFileToDownload(String filename)
    {
        return EXTENSIONS_TO_DOWNLOAD.contains(FilenameUtils.getExtension(filename));
    }

    private void readFolderContents(DbxClientV2 client, CloudFolderInfo folder, CloudListener listener,PublishSubject<CloudItemInfo> itemSource,boolean includeSubfolders, boolean returnFolders)
    {
        List<CloudFolderInfo> foldersToSearch=new ArrayList<>();
        foldersToSearch.add(folder);

        while(!foldersToSearch.isEmpty())
        {
            if(listener.shouldCancel())
                break;
            CloudFolderInfo folderToSearch=foldersToSearch.remove(0);
            String currentFolderID=folderToSearch.mID;
            String currentFolderName=folderToSearch.mName;
            try
            {
                Log.d(BeatPrompterApplication.TAG, "Getting list of everything in Dropbox folder.");
                ListFolderResult listResult = client.files().listFolder(currentFolderID);
                while(listResult!=null)
                {
                    if(listener.shouldCancel())
                        break;
                    List<Metadata> entries=listResult.getEntries();
                    for(Metadata mdata:entries)
                    {
                        if(mdata instanceof FileMetadata)
                        {
                            FileMetadata fmdata=(FileMetadata)mdata;
                            String filename=fmdata.getName().toLowerCase();
                            if(isSuitableFileToDownload(filename))
                                itemSource.onNext(new CloudFileInfo(fmdata.getId(), fmdata.getName(),fmdata.getServerModified(),currentFolderName));
                        }
                        else if(mdata instanceof FolderMetadata)
                        {
                            Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                            CloudFolderInfo newFolder=new CloudFolderInfo(folderToSearch,mdata.getPathLower(),mdata.getName(),mdata.getPathDisplay());
                            if(includeSubfolders)
                                foldersToSearch.add(newFolder);
                            if(returnFolders)
                                itemSource.onNext(newFolder);
                        }
                    }
                    if(listener.shouldCancel())
                        break;
                    if(listResult.getHasMore())
                        listResult=client.files().listFolderContinue(listResult.getCursor());
                    else
                        listResult=null;
                }
            }
            catch(DbxException de)
            {
                itemSource.onError(de);
            }
        }
        itemSource.onComplete();
    }

    private void doDropboxAction(DropboxAction action)
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
            action.onAuthenticationRequired();
            Auth.startOAuth2Authentication(mParentActivity, DROPBOX_APP_KEY);
        }
        else {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BeatPrompterApplication.APP_NAME)
                    .build();
            action.onConnected(new DbxClientV2(requestConfig, storedAccessToken));
        }
    }

    @Override
    public void downloadFiles(List<CloudFileInfo> filesToDownload,CloudListener cloudListener,PublishSubject<CloudDownloadResult> itemSource,PublishSubject<String> messageSource) {
        doDropboxAction(new DropboxAction() {
            @Override
            public void onConnected(DbxClientV2 client) {
                downloadFiles(client,cloudListener,itemSource,messageSource,filesToDownload);
            }

            @Override
            public void onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired();
            }
        });
    }

    @Override
    public void readFolderContents(CloudFolderInfo folder, CloudListener cloudListener,PublishSubject<CloudItemInfo> itemSource,boolean includeSubfolders, boolean returnFolders) {
        doDropboxAction(new DropboxAction() {
            @Override
            public void onConnected(DbxClientV2 client) {
                readFolderContents(client,folder,cloudListener,itemSource,includeSubfolders,returnFolders);
            }

            @Override
            public void onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired();
            }
        });
    }

    public CloudFolderInfo getRootPath()
    {
        return new CloudFolderInfo("",DROPBOX_ROOT_PATH,DROPBOX_ROOT_PATH);
    }

    public String getDirectorySeparator()
    {
        return "/";
    }

    public int getCloudIconResourceId()
    {
        return R.drawable.ic_dropbox;
    }
}
