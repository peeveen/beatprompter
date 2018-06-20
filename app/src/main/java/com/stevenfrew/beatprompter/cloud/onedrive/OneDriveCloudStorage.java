package com.stevenfrew.beatprompter.cloud.onedrive;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
import com.onedrive.sdk.extensions.IItemCollectionPage;
import com.onedrive.sdk.extensions.IItemCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;

import com.onedrive.sdk.extensions.OneDriveClient;
import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.Utils;
import com.stevenfrew.beatprompter.cloud.CloudCacheFolder;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResultType;
import com.stevenfrew.beatprompter.cloud.CloudException;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudItemInfo;
import com.stevenfrew.beatprompter.cloud.CloudListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.subjects.PublishSubject;

public class OneDriveCloudStorage extends CloudStorage {
    private static final String ONEDRIVE_CACHE_FOLDER_NAME="onedrive";
    private final static String ONEDRIVE_CLIENT_ID = "dc584873-700c-4377-98da-d088cca5c1f5"; //This is your client ID
    private final static String ONEDRIVE_ROOT_PATH="/";

    public interface OneDriveAction
    {
        void onConnected(IOneDriveClient client);
        void onAuthenticationRequired();
    }

    private final MSAAuthenticator ONEDRIVE_MSA_AUTHENTICATOR = new MSAAuthenticator()
    {
        @Override
        public String getClientId() {
            return ONEDRIVE_CLIENT_ID;
        }

        @Override
        public String[] getScopes() {
            return new String[] { "onedrive.readonly","wl.offline_access" };
        }
    };

    private Activity mParentActivity;
    private CloudCacheFolder mOneDriveFolder;

    public OneDriveCloudStorage(Activity parentActivity)
    {
        mParentActivity=parentActivity;
        mOneDriveFolder=new CloudCacheFolder(SongList.mBeatPrompterSongFilesFolder,ONEDRIVE_CACHE_FOLDER_NAME);
        if(!mOneDriveFolder.exists())
            if(!mOneDriveFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG,"Failed to create OneDrive sync folder.");
    }

    private static class GetOneDriveFolderContentsTask extends AsyncTask<Void, Void, Void>
    {
        IOneDriveClient mClient;
        CloudFolderInfo mFolder;
        CloudListener mListener;
        PublishSubject<CloudItemInfo> mItemSource;
        boolean mIncludeSubfolders;
        OneDriveCloudStorage mCloudStorage;
        boolean mReturnFolders;
        GetOneDriveFolderContentsTask(IOneDriveClient client, OneDriveCloudStorage cloudStorage,CloudFolderInfo folder, CloudListener listener,PublishSubject<CloudItemInfo> itemSource,boolean includeSubfolders, boolean returnFolders)
        {
            mClient=client;
            mCloudStorage=cloudStorage;
            mFolder=folder;
            mListener=listener;
            mItemSource=itemSource;
            mIncludeSubfolders=includeSubfolders;
            mReturnFolders=returnFolders;
        }
        private boolean isSuitableFileToDownload(Item childItem)
        {
            return (childItem.audio!=null) || (childItem.image!=null) ||  ((childItem.name.toLowerCase().endsWith(".txt")));
        }
        protected Void doInBackground(Void... args) {
            List<CloudFolderInfo> folders = new ArrayList<>();
            folders.add(mFolder);

            while (!folders.isEmpty()) {
                if (mListener.shouldCancel())
                    break;
                CloudFolderInfo nextFolder = folders.remove(0);
                String currentFolderID = nextFolder.mID;

                try {
                    Log.d(BeatPrompterApplication.TAG, "Getting list of everything in OneDrive folder.");
                    IItemCollectionPage page = mClient.getDrive().getItems(currentFolderID).getChildren().buildRequest().get();
                    while (page != null) {
                        if (mListener.shouldCancel())
                            break;
                        List<Item> children = page.getCurrentPage();
                        for (Item child : children) {
                            if (mListener.shouldCancel())
                                break;
                            if (child.file != null) {
                                if (isSuitableFileToDownload(child))
                                    mItemSource.onNext(new CloudFileInfo(child.id, child.name, child.lastModifiedDateTime.getTime(), nextFolder.mName));
                            } else if (child.folder != null) {
                                String fullPath=mCloudStorage.constructFullPath(nextFolder.mDisplayPath,child.name);
                                CloudFolderInfo newFolder = new CloudFolderInfo(nextFolder, child.id, child.name, fullPath);
                                if (mIncludeSubfolders) {
                                    Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                                    folders.add(newFolder);
                                }
                                if (mReturnFolders)
                                    mItemSource.onNext(newFolder);
                            }
                        }
                        if (mListener.shouldCancel())
                            break;
                        IItemCollectionRequestBuilder builder = page.getNextPage();
                        if (builder != null)
                            page = builder.buildRequest().get();
                        else
                            page = null;
                    }
                } catch (Exception e) {
                    mItemSource.onError(e);
                }
            }
            mItemSource.onComplete();
            return null;
        }
    }

    private static class DownloadOneDriveFilesTask extends AsyncTask<Void, Void, Void>
    {
        IOneDriveClient mClient;
        CloudListener mListener;
        PublishSubject<CloudDownloadResult> mItemSource;
        PublishSubject<String> mMessageSource;
        List<CloudFileInfo> mFilesToDownload;
        File mDownloadFolder;

        DownloadOneDriveFilesTask(IOneDriveClient client, CloudListener listener, PublishSubject<CloudDownloadResult> itemSource, PublishSubject<String> messageSource, List<CloudFileInfo> filesToDownload,File downloadFolder)
        {
            mClient = client;
            mDownloadFolder=downloadFolder;
            mFilesToDownload=filesToDownload;
            mMessageSource=messageSource;
            mListener = listener;
            mItemSource = itemSource;
        }

        protected Void doInBackground(Void... args)
        {
            for (CloudFileInfo file : mFilesToDownload) {
                if (mListener.shouldCancel())
                    break;
                CloudDownloadResult result;
                try {
                    Item driveFile = mClient.getDrive().getItems(file.mID).buildRequest().get();
                    if (driveFile != null) {
                        String title = file.mName;
                        Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                        mMessageSource.onNext(String.format(SongList.getContext().getString(R.string.checking), title));
                        String safeFilename = Utils.makeSafeFilename(title);
                        File targetFile = new File(mDownloadFolder, safeFilename);
                        Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

                        Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                        mMessageSource.onNext(String.format(SongList.getContext().getString(R.string.downloading), title));
                        // Don't check lastModified ... ALWAYS download.
                        if (mListener.shouldCancel())
                            break;
                        File localFile = downloadOneDriveFile(mClient, driveFile, targetFile);
                        result = new CloudDownloadResult(file, localFile);
                    } else
                        result = new CloudDownloadResult(file, CloudDownloadResultType.NoLongerExists);
                    mItemSource.onNext(result);
                    if (mListener.shouldCancel())
                        break;
                } catch (Exception e) {
                    mItemSource.onError(e);
                }
            }
            mItemSource.onComplete();
            return null;
        }
        private File downloadOneDriveFile(IOneDriveClient client,Item file, File localFile) throws IOException
        {
            FileOutputStream fos =null;
            InputStream inputStream=null;
            try {
                fos = new FileOutputStream(localFile);
                inputStream=client.getDrive().getItems(file.id).getContent().buildRequest().get();
                Utils.streamToStream(inputStream,fos);
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
                if(inputStream!=null)
                    try {
                        inputStream.close();
                    }
                    catch(Exception eee)
                    {
                        Log.e(BeatPrompterApplication.TAG,"Failed to close input stream.",eee);
                    }
            }
            return localFile;
        }
    }

    private void doOneDriveAction(OneDriveAction action)
    {
        final ICallback<IOneDriveClient> callback = new ICallback<IOneDriveClient>() {
            @Override
            public void success(final IOneDriveClient clientResult) {
                Log.v(BeatPrompterApplication.TAG, "Signed in to OneDrive");
                action.onConnected(clientResult);
            }

            @Override
            public void failure(final ClientException error) {
                Log.e(BeatPrompterApplication.TAG, "Nae luck signing in to OneDrive");
                action.onAuthenticationRequired();
            }
        };

        IClientConfig oneDriveConfig = DefaultClientConfig.
                createWithAuthenticator(ONEDRIVE_MSA_AUTHENTICATOR);
        new OneDriveClient.Builder()
                .fromConfig(oneDriveConfig)
                .loginAndBuildClient(mParentActivity, callback);

    }

    private static class GetOneDriveRootFolderTask extends AsyncTask<Void, Void, CloudFolderInfo>
    {
        IOneDriveClient mClient;
        GetOneDriveRootFolderTask(IOneDriveClient client)
        {
            mClient=client;
        }
        protected CloudFolderInfo doInBackground(Void... args) {
            Item rootFolder = mClient.getDrive().getRoot().buildRequest().get();
            return new CloudFolderInfo(rootFolder.id, ONEDRIVE_ROOT_PATH, ONEDRIVE_ROOT_PATH);
        }
    }

    @Override
    protected void getRootPath(CloudListener listener,PublishSubject<CloudFolderInfo> rootPathSource)
    {
        doOneDriveAction(new OneDriveAction() {
            @Override
            public void onConnected(IOneDriveClient client) {
                try {
                    CloudFolderInfo rootFolder=new GetOneDriveRootFolderTask(client).execute().get();
                    rootPathSource.onNext(rootFolder);
                }
                catch(Exception e)
                {
                    rootPathSource.onError(e);
                }
            }

            @Override
            public void onAuthenticationRequired() {
                rootPathSource.onError(new CloudException(SongList.mSongListInstance.getString(R.string.could_not_find_cloud_root_error)));
            }
        });
    }

    @Override
    public String getDirectorySeparator() {
        return "/";
    }

    @Override
    public int getCloudIconResourceId() {
        return R.drawable.ic_onedrive;
    }

    @Override
    public String getCloudStorageName() {
        return SongList.mSongListInstance.getString(R.string.onedrive_string);
    }

    @Override
    public CloudType getCloudStorageType() {
        return CloudType.OneDrive;
    }

    @Override
    public CloudCacheFolder getCacheFolder()
    {
        return mOneDriveFolder;
    }

    @Override
    public void downloadFiles(List<CloudFileInfo> filesToDownload,CloudListener cloudListener,PublishSubject<CloudDownloadResult> itemSource,PublishSubject<String> messageSource) {
        doOneDriveAction(new OneDriveAction() {
            @Override
            public void onConnected(IOneDriveClient client) {
                try {
                    new DownloadOneDriveFilesTask(client, cloudListener, itemSource, messageSource, filesToDownload, mOneDriveFolder).execute();
                }
                catch(Exception e)
                {
                    itemSource.onError(e);
                }
            }

            @Override
            public void onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired();
            }
        });
    }

    @Override
    public void readFolderContents(CloudFolderInfo folder, CloudListener cloudListener,PublishSubject<CloudItemInfo> itemSource,boolean includeSubfolders, boolean returnFolders) {
        doOneDriveAction(new OneDriveAction() {
            @Override
            public void onConnected(IOneDriveClient client) {
                try {
                    new GetOneDriveFolderContentsTask(client, OneDriveCloudStorage.this, folder, cloudListener, itemSource, includeSubfolders, returnFolders).execute();
                }
                catch(Exception e)
                {
                    itemSource.onError(e);
                }
            }

            @Override
            public void onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired();
            }
        });
    }
}
