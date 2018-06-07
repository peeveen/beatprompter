/*package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

class GoogleDriveChooseFolderDialog extends ChooseFolderDialog
{
    private final static String TAG="beatprompter";
    private final static String GOOGLE_DRIVE_ROOT_FOLDER_ID="root";
    private com.google.api.services.drive.Drive mGoogleDriveClient;
    private final static String GOOGLE_DRIVE_ROOT_PATH="/";
    private Exception mException;
    UserRecoverableAuthIOException mUserRecoverableAuthIOException=null;

    GoogleDriveChooseFolderDialog(Activity parentActivity, Context context, String preferenceName, String displayPreferenceName, com.google.api.services.drive.Drive googleDriveClient)
    {
        super(parentActivity,context,preferenceName,displayPreferenceName,R.drawable.ic_google_drive);
        mGoogleDriveClient=googleDriveClient;
    }

    CloudItem getRootPath()
    {
        return new CloudItem(GOOGLE_DRIVE_ROOT_PATH,GOOGLE_DRIVE_ROOT_FOLDER_ID,true);
    }

    private class GetGoogleDriveFoldersTask extends ChooseFolderDialog.FolderFetcherTask
    {
        protected List<CloudItem> doInBackground(CloudItem ... args) {
            List<CloudItem> items=new ArrayList<>();
            try {
                CloudItem parentFolder =args[0];
                Drive.Files.List request = mGoogleDriveClient.files().list().setQ("trashed=false and '"+parentFolder.mInternalPath+"' in parents").setFields("nextPageToken,files(id,name,mimeType)");
                mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,0,0).sendToTarget();
                do {
                    if(isCancelled())
                        return null;
                    Log.d(TAG, "Executing GoogleDrive query ...");
                    FileList children = request.execute();
                    Log.d(TAG, "Iterating through contents, seeing what needs updated/downloaded/deleted ...");

                    for (File child : children.getFiles())
                    {
                        if(isCancelled())
                            return null;
                        String fileID=child.getId();
                        Log.d(TAG, "File ID: " + fileID);
                        //com.google.api.services.drive.model.File file=mGoogleDriveClient.files().get(fileID).execute();
                        if(isCancelled())
                            return null;
                        String mimeType=child.getMimeType();
                        boolean isFolder= ((mimeType != null) && (mimeType.equalsIgnoreCase("application/vnd.google-apps.folder")));
                        CloudItem cloudItem=new CloudItem(parentFolder,child.getName(),child.getId(),isFolder);
                        items.add(cloudItem);
                        mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,items.size(),0).sendToTarget();
                    }
                    request.setPageToken(children.getNextPageToken());
                } while (request.getPageToken() != null &&
                        request.getPageToken().length() > 0);
            }
            catch(Exception e) {
                Log.d(TAG, "Exception while accessing Google Drive:"+e.getMessage());
                mException = e;
                if(e instanceof UserRecoverableAuthIOException) {
                    mUserRecoverableAuthIOException = (UserRecoverableAuthIOException) e;
                    mActivity.startActivityForResult(mUserRecoverableAuthIOException.getIntent(), SettingsActivity.COMPLETE_AUTHORIZATION_REQUEST_CODE);
                    items = null;
                }
            }
            return items;
        }

    }

    ChooseFolderDialog.FolderFetcherTask getFolderFetcher()
    {
        return new GetGoogleDriveFoldersTask();
    }

    String getDirectorySeparator()
    {
        return "/";
    }
}
*/