package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.onedrive.sdk.extensions.IItemCollectionPage;
import com.onedrive.sdk.extensions.IItemCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;

import java.util.ArrayList;
import java.util.List;

class OneDriveChooseFolderDialog extends ChooseFolderDialog
{
    private IOneDriveClient mOneDriveClient;
    private final static String ONEDRIVE_ROOT_PATH="/";
    private Exception mException;

    OneDriveChooseFolderDialog(Activity parentActivity, Context context, String preferenceName, String displayPreferenceName, IOneDriveClient oneDriveClient)
    {
        super(parentActivity,context,preferenceName,displayPreferenceName,R.drawable.ic_onedrive);
        mOneDriveClient=oneDriveClient;
    }

    private class GetOneDriveRootFolderTask extends AsyncTask<String, Void, CloudItem>
    {
        protected CloudItem doInBackground(String... args) {
            try {
                Item rootFolder = mOneDriveClient.getDrive().getRoot().buildRequest().get();
                return new CloudItem(ONEDRIVE_ROOT_PATH, rootFolder.id, true);
            }
            catch(Exception e)
            {
                mException=e;
            }
            return null;
        }
    }

    CloudItem getRootPath()
    {
        try
        {
            return new GetOneDriveRootFolderTask().execute().get();
        }
        catch(Exception e)
        {
            mException=e;
        }
        return null;
    }

    private class GetOneDriveFoldersTask extends ChooseFolderDialog.FolderFetcherTask
    {
        protected List<CloudItem> doInBackground(CloudItem... args) {
            List<CloudItem> items=new ArrayList<>();
            try {
                CloudItem parentFolder=args[0];
                if(isCancelled())
                    return null;
                IItemCollectionPage page= mOneDriveClient.getDrive().getItems(parentFolder.mInternalPath).getChildren().buildRequest().get();
                mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,0,0).sendToTarget();
                if(isCancelled())
                    return null;
                while(page!=null)
                {
                    if(isCancelled())
                        return null;
                    List<Item> children=page.getCurrentPage();
                    for(Item child:children)
                    {
                        if(isCancelled())
                            return null;
                        if(child.folder!=null)
                            items.add(new CloudItem(parentFolder,child.name,child.id,true));
                        else if(child.file!=null)
                            items.add(new CloudItem(parentFolder,child.name,child.id,false));
                        mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHING,items.size(),0).sendToTarget();
                    }
                    IItemCollectionRequestBuilder builder=page.getNextPage();
                    if(builder!=null)
                        page=builder.buildRequest().get();
                    else
                        page=null;
                }
            } catch (Exception e) {
                mException = e;
                //Toast.makeText(mActivity,e.getMessage(),Toast.LENGTH_LONG).show();
            }
            return items;
        }
    }

    String getDirectorySeparator()
    {
        return "/";
    }

    ChooseFolderDialog.FolderFetcherTask getFolderFetcher()
    {
        return new GetOneDriveFoldersTask();
    }
}
