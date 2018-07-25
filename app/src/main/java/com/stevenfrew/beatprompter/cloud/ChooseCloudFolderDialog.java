package com.stevenfrew.beatprompter.cloud;

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
import android.widget.Toast;

import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

class ChooseCloudFolderDialog implements DialogInterface.OnCancelListener,DialogInterface.OnDismissListener,CloudFolderSearchListener {
    private static final String PARENT_DIR = "..";

    static class FolderContentsFetchHandler extends Handler
    {
        ChooseCloudFolderDialog mChooseFolderDialog;
        FolderContentsFetchHandler(ChooseCloudFolderDialog parentDialog)
        {
            mChooseFolderDialog=parentDialog;
        }
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EventHandler.FOLDER_CONTENTS_FETCHED:
                    mChooseFolderDialog.populateBrowser((List<CloudItemInfo>) msg.obj);
                    break;
                case EventHandler.FOLDER_CONTENTS_FETCHING:
                    mChooseFolderDialog.updateProgress(msg.arg1,msg.arg2);
                    break;
            }
        }
    }

    private Dialog mDialog;
    private CompositeDisposable mFolderSelectionEventSubscription;
    private CloudFolderInfo mCurrentFolder;
    private CloudFolderInfo mParentFolder;
    private FolderContentsFetchHandler mHandler;
    private Activity mActivity;
    private FolderFetcherTask mFolderFetcher;
    private PublishSubject<CloudFolderInfo> mFolderSelectionSource=PublishSubject.create();
    private CloudStorage mCloudStorage;
    private List<CloudItemInfo> mDisplayItems=new ArrayList<>();

    ChooseCloudFolderDialog(final Activity activity,CloudStorage cloudStorage,CloudFolderSelectionListener listener,CloudFolderInfo rootPath) {
        mActivity=activity;
        mCloudStorage=cloudStorage;
        mCurrentFolder=rootPath;
        mDialog = new Dialog(activity, R.style.CustomDialog);
        mDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        mDialog.setContentView(R.layout.choose_folder_dialog_loading);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(this);
        mDialog.setOnDismissListener(this);
        TextView tv = mDialog.findViewById(android.R.id.title);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setLines(1);
        tv.setHorizontallyScrolling(true);
        mFolderSelectionEventSubscription=new CompositeDisposable();
        mFolderSelectionEventSubscription.add(mFolderSelectionSource.subscribe(listener::onFolderSelected,listener::onFolderSelectedError));
        mDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, cloudStorage.getCloudIconResourceId());
        mHandler=new FolderContentsFetchHandler(this);
    }

    public void showDialog()
    {
        refresh(mCurrentFolder);
        mDialog.setTitle(getDisplayPath(mCurrentFolder));
        mDialog.show();
    }

    private void setNewPath(final CloudFolderInfo newFolder) {
        if(newFolder!=null)
            mFolderSelectionSource.onNext(newFolder);
        mDialog.dismiss();
    }

    private void refresh(CloudFolderInfo folder)
    {
        if(folder==null)
            return;
        this.mCurrentFolder = folder;
        this.mParentFolder=folder.mParentFolder;
        cancelFolderFetcher();
        mDisplayItems.clear();
        mFolderFetcher=new FolderFetcherTask(mCloudStorage,this);
        mFolderFetcher.execute(mCurrentFolder);
    }

    private void populateBrowser(List<CloudItemInfo> contents) {
        if (contents == null)
            mDialog.dismiss();
        else {
            contents.sort(CloudItemInfo::compareTo);

            if (mCurrentFolder.mParentFolder != null)
                contents.add(0, new CloudFolderInfo(mCurrentFolder.mParentFolder.mParentFolder,mCurrentFolder.mParentFolder.mID, PARENT_DIR, mCurrentFolder.mParentFolder.mDisplayPath));

            // refresh the user interface
            mDialog.setContentView(R.layout.choose_folder_dialog);
            final ListView list = mDialog.findViewById(R.id.chooseFolderListView);
            Button okButton = mDialog.findViewById(R.id.chooseFolderOkButton);
            mDialog.setTitle(getDisplayPath(mCurrentFolder));
            list.setAdapter(new CloudBrowserItemListAdapter(contents));

            list.setOnItemClickListener((parent, view, which, id) -> {
                CloudFolderInfo folderChosen;
                if((which==0)&&(mParentFolder!=null))
                    folderChosen=mParentFolder;
                else
                    folderChosen = (CloudFolderInfo) list.getItemAtPosition(which);

                mDialog.setContentView(R.layout.choose_folder_dialog_loading);
                mDialog.setTitle(getDisplayPath(folderChosen));
                refresh(folderChosen);
            });
            okButton.setOnClickListener(v -> setNewPath(mCurrentFolder));
        }
    }

    private String getDisplayPath(CloudFolderInfo folder)
    {
        if(folder.mParentFolder!=null) {
            String parentPath=getDisplayPath(folder.mParentFolder);
            if(!parentPath.endsWith(mCloudStorage.getDirectorySeparator()))
                parentPath+=mCloudStorage.getDirectorySeparator();
            return parentPath + folder.mName;
        }
        return folder.mName;
    }

    private void updateProgress(int found,int max)
    {
        TextView progressText = mDialog.findViewById(R.id.loading_count);
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
        mFolderSelectionEventSubscription.dispose();
    }

    public void onCancel(DialogInterface dialog)
    {
        cancelFolderFetcher();
        mFolderSelectionSource.onComplete();
        mFolderSelectionEventSubscription.dispose();
    }

    private void cancelFolderFetcher()
    {
        if(mFolderFetcher!=null) {
            mFolderFetcher.cancel(true);
            mFolderFetcher=null;
        }
    }

    public static class FolderFetcherTask extends AsyncTask<CloudFolderInfo, Void, Void>
    {
        CloudStorage mCloudStorage;
        CloudFolderSearchListener mCloudFolderSearchListener;
        FolderFetcherTask(CloudStorage cloudStorage,CloudFolderSearchListener listener)
        {
            mCloudStorage=cloudStorage;
            mCloudFolderSearchListener=listener;
        }
        protected Void doInBackground(CloudFolderInfo... args)
        {
            CloudFolderInfo folderToSearch=args[0];
            mCloudStorage.readFolderContents(folderToSearch, mCloudFolderSearchListener,false,true);
            return null;
        }
    }

    public void onCloudItemFound(CloudItemInfo cloudItem)
    {
        mDisplayItems.add(cloudItem);
    }

    public void onFolderSearchError(Throwable t)
    {
        Toast.makeText(mActivity,t.getMessage(),Toast.LENGTH_LONG).show();
        onFolderSearchComplete();
    }

    public void onFolderSearchComplete()
    {
        mDisplayItems.sort(CloudItemInfo::compareTo);
        mHandler.obtainMessage(EventHandler.FOLDER_CONTENTS_FETCHED,mDisplayItems).sendToTarget();
    }

    @Override
    public void onProgressMessageReceived(String message) {
        // Do nothing.
    }

    @Override
    public void onAuthenticationRequired() {
        // Cancel the dialog.
        mDialog.cancel();
    }

    @Override
    public boolean shouldCancel() {
        return mFolderFetcher != null && mFolderFetcher.isCancelled();
    }


}