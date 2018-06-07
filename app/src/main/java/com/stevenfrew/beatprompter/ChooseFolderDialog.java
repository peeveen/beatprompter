package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

abstract class ChooseFolderDialog implements DialogInterface.OnCancelListener,DialogInterface.OnDismissListener {
    private static final String PARENT_DIR = "..";

    Handler mChooseFolderDialogHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BeatPrompterApplication.FOLDER_CONTENTS_FETCHED:
                    populateBrowser((List<CloudBrowserItem>)msg.obj);
                    break;
                case BeatPrompterApplication.FOLDER_CONTENTS_FETCHING:
                    updateProgress(msg.arg1,msg.arg2);
                    break;
            }
        }
    };

    private Dialog mDialog;
    private CloudBrowserItem mCurrentFolder;
    private CloudBrowserItem mParentFolder;
    private Activity mActivity;
    private FolderFetcherTask mFolderFetcher;
    private PublishSubject<CloudFolderInfo> mFolderSelectionSource=PublishSubject.create();

    ChooseFolderDialog(final Activity activity,int iconResourceID) {
        mActivity=activity;
        mDialog = new Dialog(activity,R.style.CustomDialog);
        mDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        mDialog.setContentView(R.layout.choose_folder_dialog_loading);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(this);
        mDialog.setOnDismissListener(this);
        TextView tv = mDialog.findViewById(android.R.id.title);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setLines(1);
        tv.setHorizontallyScrolling(true);
        mDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, iconResourceID);
    }

    void showDialog()
    {
        mCurrentFolder=getRootPath();
        if(mCurrentFolder==null)
            return;
        refresh(mCurrentFolder);
        mDialog.setTitle(getDisplayPath(mCurrentFolder));
        mDialog.show();
    }

    private void setNewPath(final CloudBrowserItem newFolder) {
        if(newFolder!=null)
            mFolderSelectionSource.onNext(new CloudFolderInfo(newFolder.mInternalPath, getDisplayPath(newFolder)));
        mDialog.dismiss();
    }

    private void refresh(CloudBrowserItem folder)
    {
        if(folder==null)
            return;
        this.mCurrentFolder = folder;
        this.mParentFolder=folder.mParentFolder;
        cancelFolderFetcher();
        mFolderFetcher=getFolderFetcher();
        mFolderFetcher.execute(mCurrentFolder);
    }

    private void populateBrowser(List<CloudBrowserItem> dirs) {
        if (dirs == null)
            mDialog.dismiss();
        else {
            dirs.sort(CloudBrowserItem::compareTo);

            if (mCurrentFolder.mParentFolder != null)
                dirs.add(0, new CloudBrowserItem(mCurrentFolder.mParentFolder, PARENT_DIR, mCurrentFolder.mParentFolder.mInternalPath, true));

            // refresh the user interface
            mDialog.setContentView(R.layout.choose_folder_dialog);
            final ListView list = mDialog.findViewById(R.id.chooseFolderListView);
            Button okButton = mDialog.findViewById(R.id.chooseFolderOkButton);
            mDialog.setTitle(getDisplayPath(mCurrentFolder));
            list.setAdapter(new CloudBrowserItemListAdapter(dirs));

            list.setOnItemClickListener((parent, view, which, id) -> {
                CloudBrowserItem folderChosen;
                if((which==0)&&(mParentFolder!=null))
                    folderChosen=mParentFolder;
                else
                    folderChosen = (CloudBrowserItem) list.getItemAtPosition(which);

                mDialog.setContentView(R.layout.choose_folder_dialog_loading);
                mDialog.setTitle(getDisplayPath(folderChosen));
                refresh(folderChosen);
            });
            okButton.setOnClickListener(v -> setNewPath(mCurrentFolder));
        }
    }

    abstract CloudBrowserItem getRootPath();
    abstract String getDirectorySeparator();
    abstract ChooseFolderDialog.FolderFetcherTask getFolderFetcher();

    private String getDisplayPath(CloudBrowserItem folder)
    {
        if(folder.mParentFolder!=null) {
            String parentPath=getDisplayPath(folder.mParentFolder);
            if(!parentPath.endsWith(getDirectorySeparator()))
                parentPath+=getDirectorySeparator();
            return parentPath + folder.mDisplayName;
        }
        return folder.mDisplayName;
    }

    private void updateProgress(int found,int max)
    {
        TextView progressText = (TextView) mDialog.findViewById(R.id.loading_count);
        if(progressText!=null)
            if(max==0) {
                String itemsFound = mActivity.getString(R.string.itemsFound);
                progressText.setText(String.format(Locale.getDefault(), itemsFound, found));
            }
            else
            {
                String itemsFound = mActivity.getString(R.string.itemsFoundWithMax);
                progressText.setText(String.format(Locale.getDefault(), itemsFound, found, max));
            }
    }

    public void onDismiss(DialogInterface dialog)
    {
        cancelFolderFetcher();
        mFolderSelectionSource.onComplete();
    }

    public void onCancel(DialogInterface dialog)
    {
        cancelFolderFetcher();
        mFolderSelectionSource.onComplete();
    }

    private void cancelFolderFetcher()
    {
        if(mFolderFetcher!=null) {
            mFolderFetcher.cancel(true);
            mFolderFetcher=null;
        }
    }

    abstract class FolderFetcherTask extends AsyncTask<CloudBrowserItem, Void, List<CloudBrowserItem>>
    {
        protected abstract List<CloudBrowserItem> doInBackground(CloudBrowserItem... args);
        protected void onPostExecute(List<CloudBrowserItem> folders)
        {
            mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHED,folders).sendToTarget();
        }
    }

    Observable<CloudFolderInfo> getFolderSelectionSource()
    {
        return mFolderSelectionSource;
    }
}