package com.stevenfrew.beatprompter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.*;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

class DropboxCloudStorage implements CloudStorage {
    private List<CachedFile> mFilesToRefresh;
    private PublishSubject<String> mProgressMessageSource=PublishSubject.create();

    private List<CloudDownloadResult> dropboxDo(Function<DbxClientV2,List<CloudDownloadResult>> function)
    {
        SharedPreferences sharedPrefs = SongList.mSongListInstance.getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
        String storedAccessToken = sharedPrefs.getString(SongList.mSongListInstance.getString(R.string.pref_dropboxAccessToken_key), null);
        if (storedAccessToken != null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BeatPrompterApplication.APP_NAME)
                    .build();
            return function.apply(new DbxClientV2(requestConfig, storedAccessToken));
        }
        return null;
    }

    private List<CloudDownloadResult> _refreshFiles(DbxClientV2 client)
    {
        List<CloudDownloadResult> results=new ArrayList<>();
        for(CachedFile file:mFilesToRefresh) {
            try {
                Metadata mdata = client.files().getMetadata(file.mStorageName);
                if ((mdata != null) && (mdata instanceof FileMetadata)) {
                    FileMetadata fmdata = (FileMetadata) mdata;
                    String title = fmdata.getName();
                    Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                    mProgressMessageSource.onNext(String.format(SongList.getContext().getString(R.string.checking), title));
                    String safeFilename = Utils.makeSafeFilename(title);
                    File targetFile = new File(file.mFile.getParent(), safeFilename);
                    Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

                    Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                    mProgressMessageSource.onNext(String.format(SongList.getContext().getString(R.string.downloading), title));
                    File localFile = downloadDropboxFile(client, fmdata, targetFile);
                    Date lastModified = fmdata.getServerModified();
                    results.add(new CloudDownloadResult(new CloudFileInfo(file.mStorageName, title, lastModified, file.mSubfolder), localFile));
                } else
                    results.add(new CloudDownloadResult(new CloudFileInfo(file.mStorageName, file.mTitle, file.mLastModified, file.mSubfolder),CloudDownloadResultType.NoLongerExists));
            } catch (Exception e) {
                results.add(new CloudDownloadResult(file.mStorageName, e));
            }
        }
        return results;
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
    public List<CloudDownloadResult> refreshFiles(List<CachedFile> filesToRefresh) {
        mFilesToRefresh=filesToRefresh;
        return dropboxDo(this::_refreshFiles);
    }

    @Override
    public String getCloudStorageName() {
        return SongList.mSongListInstance.getString(R.string.dropboxValue);
    }

    @Override
    public CloudType getCloudStorageType() {
        return CloudType.Dropbox;
    }

    @Override
    public List<CloudDownloadResult> downloadFolderContents(String folderID, boolean includeSubfolders, Map<String, File> existingCachedFiles) throws IOException {
        return null;
    }

    @Override
    public Observable<String> getProgressMessageSource() {
        return mProgressMessageSource;
    }
}
