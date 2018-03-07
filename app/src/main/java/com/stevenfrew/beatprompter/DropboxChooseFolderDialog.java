package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.content.Context;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;

class DropboxChooseFolderDialog extends ChooseFolderDialog
{
    private DbxClientV2 mDropboxClient;
    private final static String DROPBOX_ROOT_PATH="/";
    private Exception mException;

    DropboxChooseFolderDialog(Activity parentActivity, Context context, String preferenceName, String displayPreferenceName, DbxClientV2 dropboxClient)
    {
        super(parentActivity,context,preferenceName,displayPreferenceName,R.drawable.ic_dropbox);
        mDropboxClient=dropboxClient;
    }
    CloudItem getRootPath()
    {
        return new CloudItem(DROPBOX_ROOT_PATH,"",true);
    }

    private class GetDropboxFoldersTask extends ChooseFolderDialog.FolderFetcherTask
    {
        protected List<CloudItem> doInBackground(CloudItem ... args) {
            List<CloudItem> items=new ArrayList<>();
            try {
                CloudItem parentFolder =args[0];
                if(isCancelled())
                    return null;
                ListFolderResult listResult = mDropboxClient.files().listFolder(parentFolder.mInternalPath);

                if(isCancelled())
                    return null;
                mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,items.size(),0).sendToTarget();
                while(listResult!=null) {
                    if(isCancelled())
                        return null;
                    List<Metadata> entries = listResult.getEntries();
                    for(Metadata entry:entries)
                    {
                        if(isCancelled())
                            return null;
                        if(entry instanceof FolderMetadata)
                        {
                            FolderMetadata folderEntry=(FolderMetadata)entry;
                            CloudItem sf = new CloudItem(parentFolder,folderEntry.getName(), folderEntry.getPathLower(),true);
                            items.add(sf);
                        }
                        else if(entry instanceof FileMetadata)
                        {
                            FileMetadata fileEntry=(FileMetadata)entry;
                            CloudItem sf = new CloudItem(parentFolder,fileEntry.getName(), fileEntry.getPathLower(),false);
                            items.add(sf);
                        }
                        mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,items.size(),0).sendToTarget();
                    }
                    if (listResult.getHasMore())
                        listResult = mDropboxClient.files().listFolderContinue(listResult.getCursor());
                    else
                        listResult = null;
                }
            } catch (Exception e) {
                mException = e;
                //Toast.makeText(mActivity,e.getMessage(),Toast.LENGTH_LONG).show();
            }
            return items;
        }
    }

    ChooseFolderDialog.FolderFetcherTask getFolderFetcher()
    {
        return new GetDropboxFoldersTask();
    }

    String getDirectorySeparator()
    {
        return "/";
    }
}
