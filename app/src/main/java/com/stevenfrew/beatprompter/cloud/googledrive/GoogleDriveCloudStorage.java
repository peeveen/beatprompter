package com.stevenfrew.beatprompter.cloud.googledrive;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;
import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.Utils;
import com.stevenfrew.beatprompter.cache.AudioFile;
import com.stevenfrew.beatprompter.cache.ImageFile;
import com.stevenfrew.beatprompter.cloud.CloudCacheFolder;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResultType;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFileType;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudItemInfo;
import com.stevenfrew.beatprompter.cloud.CloudListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;
import com.stevenfrew.beatprompter.cloud.onedrive.OneDriveCloudStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.subjects.PublishSubject;

public class GoogleDriveCloudStorage extends CloudStorage {
    private final static String GOOGLE_DRIVE_ROOT_FOLDER_ID="root";
    private final static String GOOGLE_DRIVE_ROOT_PATH="/";
    private final static String GOOGLE_DRIVE_CACHE_FOLDER_NAME="google_drive";
    private final static String GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder";
    private final static String[] SCOPES = { DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA };
    public final static int REQUEST_CODE_RESOLUTION = 1;
    public final static int COMPLETE_AUTHORIZATION_REQUEST_CODE=2;

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

    interface GoogleDriveAction
    {
        void onConnected(com.google.api.services.drive.Drive client);
        void onAuthenticationRequired();
    }

    class GoogleDriveConnectionListener implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener
    {
        GoogleDriveAction mAction;
        GoogleApiClient mClient;
        GoogleDriveConnectionListener(GoogleDriveAction action)
        {
            mAction=action;
        }
        void setClient(GoogleApiClient client)
        {
            mClient=client;
        }
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            // Called whenever the API client fails to connect.
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection failed: " + result.toString());
            mAction.onAuthenticationRequired();
            if (!result.hasResolution()) {
                // show the localized error dialog.
                GoogleApiAvailability.getInstance().getErrorDialog(SongList.mSongListInstance, result.getErrorCode(), 0).show();
            }
            else {
                // The failure has a resolution. Resolve it.
                // Called typically when the app is not yet authorized, and an
                // authorization
                // dialog is displayed to the user.
                try {
                    Log.i(BeatPrompterApplication.TAG, "GoogleApiClient starting connection resolution ...");
                    result.startResolutionForResult(SongList.mSongListInstance, GoogleDriveCloudStorage.REQUEST_CODE_RESOLUTION);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(BeatPrompterApplication.TAG, "Exception while starting resolution activity", e);
                }
            }
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.i(BeatPrompterApplication.TAG, "API client connected.");

            if (SongList.mSongListInstance.wasPowerwashed())
                mClient.clearDefaultAccountAndReconnect();
            else {
                String accountName = Plus.AccountApi.getAccountName(mClient);
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        mParentActivity, Arrays.asList(SCOPES))
                        .setSelectedAccountName(accountName)
                        .setBackOff(new ExponentialBackOff());
                HttpTransport transport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                com.google.api.services.drive.Drive service = new com.google.api.services.drive.Drive.Builder(
                        transport, jsonFactory, credential)
                        .setApplicationName(BeatPrompterApplication.APP_NAME)
                        .build();
                mAction.onConnected(service);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection suspended");
        }
    }

    void doGoogleDriveAction(GoogleDriveAction action)
    {
        try {
            GoogleDriveConnectionListener listener=new GoogleDriveConnectionListener(action);
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mParentActivity)
                    .addApi(Drive.API)
                    .addApi(Plus.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(listener)
                    .addOnConnectionFailedListener(listener)
                    .build();
            listener.setClient(googleApiClient);
            googleApiClient.connect();
        }
        catch(Exception e)
        {
            int ffff;
            ffff=3;
        }
    }

    static void recoverAuthorization(UserRecoverableAuthIOException uraioe)
    {
        SongList.mSongListInstance.startActivityForResult(uraioe.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
    }

    private static class ReadGoogleDriveFolderContentsTask extends AsyncTask<Void, Void, Void> {
        com.google.api.services.drive.Drive mClient;
        CloudFolderInfo mFolder;
        CloudListener mListener;
        PublishSubject<CloudItemInfo> mItemSource;
        boolean mIncludeSubfolders;
        GoogleDriveCloudStorage mCloudStorage;
        boolean mReturnFolders;

        ReadGoogleDriveFolderContentsTask(com.google.api.services.drive.Drive client, GoogleDriveCloudStorage cloudStorage, CloudFolderInfo folder, CloudListener listener, PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders, boolean returnFolders) {
            mClient = client;
            mCloudStorage = cloudStorage;
            mFolder = folder;
            mListener = listener;
            mItemSource = itemSource;
            mIncludeSubfolders = includeSubfolders;
            mReturnFolders = returnFolders;
        }

        protected Void doInBackground(Void... args) {
            List<CloudFolderInfo> foldersToQuery = new ArrayList<>();
            foldersToQuery.add(mFolder);

            while (!foldersToQuery.isEmpty()) {
                if (mListener.shouldCancel())
                    break;
                CloudFolderInfo currentFolder = foldersToQuery.remove(0);
                String currentFolderID = currentFolder.mID;
                String currentFolderName = currentFolder.mName;
                String queryString = "trashed=false and '" + currentFolderID + "' in parents";
                if ((!mIncludeSubfolders) && (!mReturnFolders))
                    queryString += " and mimeType != '" + GOOGLE_DRIVE_FOLDER_MIMETYPE + "'";
                try {
                    if (mListener.shouldCancel())
                        break;
                    com.google.api.services.drive.Drive.Files.List request = mClient.files().list().setQ(queryString).setFields("nextPageToken,files(id,name,mimeType,modifiedTime)");
                    do {
                        if (mListener.shouldCancel())
                            break;
                        FileList children = request.execute();

                        Log.d(BeatPrompterApplication.TAG, "Iterating through contents, seeing what needs updated/downloaded/deleted ...");

                        for (com.google.api.services.drive.model.File child : children.getFiles()) {
                            if (mListener.shouldCancel())
                                break;
                            String fileID = child.getId();
                            String title = child.getName();
                            Log.d(BeatPrompterApplication.TAG, "File ID: " + fileID);
                            String mimeType = child.getMimeType();
                            if (GOOGLE_DRIVE_FOLDER_MIMETYPE.equals(mimeType)) {
                                CloudFolderInfo newFolder = new CloudFolderInfo(currentFolder,fileID, title, mCloudStorage.constructFullPath(currentFolder.mDisplayPath, title));
                                if (mIncludeSubfolders) {
                                    Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                                    foldersToQuery.add(newFolder);
                                }
                                if (mReturnFolders)
                                    mItemSource.onNext(newFolder);
                            } else {
                                Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                                CloudFileInfo newFile = new CloudFileInfo(fileID, title, new Date(child.getModifiedTime().getValue()), currentFolderName);
                                mItemSource.onNext(newFile);
                            }
                        }
                        request.setPageToken(children.getNextPageToken());
                        if (mListener.shouldCancel())
                            break;
                    } while (request.getPageToken() != null &&
                            request.getPageToken().length() > 0);
                } catch (UserRecoverableAuthIOException uraioe) {
                    recoverAuthorization(uraioe);
                } catch (Exception e) {
                    mItemSource.onError(e);
                }
            }
            mItemSource.onComplete();
            return null;
        }
    }

    private static class DownloadGoogleDriveFilesTask extends AsyncTask<Void, Void, Void>
    {
        com.google.api.services.drive.Drive mClient;
        CloudListener mListener;
        PublishSubject<CloudDownloadResult> mItemSource;
        PublishSubject<String> mMessageSource;
        List<CloudFileInfo> mFilesToDownload;
        File mDownloadFolder;

        DownloadGoogleDriveFilesTask(com.google.api.services.drive.Drive client, CloudListener listener, PublishSubject<CloudDownloadResult> itemSource, PublishSubject<String> messageSource, List<CloudFileInfo> filesToDownload,File downloadFolder)
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
            for (CloudFileInfo cloudFile : mFilesToDownload) {
                if (mListener.shouldCancel())
                    break;
                CloudDownloadResult result;
                try {
                    com.google.api.services.drive.model.File file = mClient.files().get(cloudFile.mID).setFields("id,name,mimeType,trashed,modifiedTime").execute();
                    if (!file.getTrashed()) {
                        String title = file.getName();
                        Log.d(BeatPrompterApplication.TAG, "File title: " + title);
                        String lowerCaseTitle = title.toLowerCase();
                        String safeFilename = Utils.makeSafeFilename(cloudFile.mID);
                        Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);
                        Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                        mMessageSource.onNext(String.format(SongList.getContext().getString(R.string.downloading), title));
                        if (mListener.shouldCancel())
                            break;
                        File localFile = downloadGoogleDriveFile(file, safeFilename);
                        result = new CloudDownloadResult(cloudFile, localFile);
                    } else
                        result = new CloudDownloadResult(cloudFile, CloudDownloadResultType.NoLongerExists);
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

        private File downloadGoogleDriveFile(com.google.api.services.drive.model.File file, String filename) throws IOException {
            File localFile = new File(mDownloadFolder, filename);
            InputStream inputStream = getDriveFileInputStream(file);
            FileOutputStream fos = null;
            if (inputStream != null) {
                try {
                    Log.d(BeatPrompterApplication.TAG, "Creating new local file, " + localFile.getAbsolutePath());
                    fos = new FileOutputStream(localFile);
                    Utils.streamToStream(inputStream, fos);
                } finally {
                    try {
                        if (fos != null)
                            fos.close();
                    }
                    finally {
                        inputStream.close();
                    }
                }
            }
            return localFile;
        }

        private InputStream getDriveFileInputStream(com.google.api.services.drive.model.File file) throws IOException
        {
            boolean isGoogleDoc = file.getMimeType().startsWith("application/vnd.google-apps.");
            if (isGoogleDoc) {
                boolean isGoogleTextDoc = file.getMimeType().equals("application/vnd.google-apps.document");
                if (isGoogleTextDoc)
                    return mClient.files().export(file.getId(), "text/plain").executeMediaAsInputStream();
                // Ignore spreadsheets, drawings, etc.
            } else
                // Binary files.
                return mClient.files().get(file.getId()).executeMediaAsInputStream();
            return null;
        }

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
        doGoogleDriveAction(new GoogleDriveAction() {
            @Override
            public void onConnected(com.google.api.services.drive.Drive client) {
                new DownloadGoogleDriveFilesTask(client,cloudListener,itemSource,messageSource,filesToRefresh,mGoogleDriveFolder).execute();
            }

            @Override
            public void onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired();
            }
        });
    }

    @Override
    protected void readFolderContents(CloudFolderInfo folder, CloudListener listener, PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders, boolean returnFolders) {
        doGoogleDriveAction(new GoogleDriveAction() {
            @Override
            public void onConnected(com.google.api.services.drive.Drive client) {
                new ReadGoogleDriveFolderContentsTask(client,GoogleDriveCloudStorage.this,folder,listener,itemSource,includeSubfolders,returnFolders).execute();
            }

            @Override
            public void onAuthenticationRequired() {
                listener.onAuthenticationRequired();
            }
        });
    }

    @Override
    public void logout()
    {
    }
}
