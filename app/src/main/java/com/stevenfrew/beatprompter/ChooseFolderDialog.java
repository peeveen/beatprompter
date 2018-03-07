package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static android.app.DialogFragment.STYLE_NORMAL;

abstract class ChooseFolderDialog implements DialogInterface.OnCancelListener,DialogInterface.OnDismissListener {
    private static final String PARENT_DIR = "..";

    Handler mChooseFolderDialogHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BeatPrompterApplication.FOLDER_CONTENTS_FETCHED:
                    populateBrowser((List<CloudItem>)msg.obj);
                    break;
                case BeatPrompterApplication.FOLDER_CONTENTS_FETCHING:
                    updateProgress(msg.arg1,msg.arg2);
                    break;
            }
        }
    };

    private Dialog mDialog;
    private CloudItem mCurrentFolder;
    private CloudItem mParentFolder;
    private String mPreferenceName;
    private String mDisplayPreferenceName;
    private Context mContext;
    protected Activity mActivity;
    private FolderFetcherTask mFolderFetcher;

    ChooseFolderDialog(final Activity activity,final Context context,String preferenceName,String displayPreferenceName, int iconResourceID) {
        mContext=context;
        mActivity=activity;
        mPreferenceName=preferenceName;
        mDisplayPreferenceName=displayPreferenceName;
        mDialog = new Dialog(activity,R.style.CustomDialog);
        mDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        mDialog.setContentView(R.layout.choose_folder_dialog_loading);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(this);
        mDialog.setOnDismissListener(this);
        TextView tv = (TextView) mDialog.findViewById(android.R.id.title);
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

    private void setNewPath(final SharedPreferences settings, final CloudItem newFolder) {
        if(newFolder!=null) {
            SharedPreferences.Editor editor=settings.edit();
            editor.putString(mDisplayPreferenceName, getDisplayPath(newFolder));
            editor.putString(mPreferenceName, newFolder.mInternalPath);
            editor.apply();
        }
        mDialog.dismiss();
    }

    private void refresh(CloudItem folder)
    {
        if(folder==null)
            return;
        this.mCurrentFolder = folder;
        this.mParentFolder=folder.mParentFolder;
        cancelFolderFetcher();
        mFolderFetcher=getFolderFetcher();
        mFolderFetcher.execute(mCurrentFolder);
    }

    private void populateBrowser(List<CloudItem> dirs) {
        if (dirs == null)
            mDialog.dismiss();
        else {
            Collections.sort(dirs, new Comparator<CloudItem>() {
                @Override
                public int compare(CloudItem folder1, CloudItem folder2) {
                    return folder1.compareTo(folder2);
                }
            });

            if (mCurrentFolder.mParentFolder != null)
                dirs.add(0, new CloudItem(mCurrentFolder.mParentFolder, PARENT_DIR, mCurrentFolder.mParentFolder.mInternalPath, true));

            // refresh the user interface
            mDialog.setContentView(R.layout.choose_folder_dialog);
            final ListView list = (ListView) mDialog.findViewById(R.id.chooseFolderListView);
            Button okButton = (Button) mDialog.findViewById(R.id.chooseFolderOkButton);
            mDialog.setTitle(getDisplayPath(mCurrentFolder));
            list.setAdapter(new CloudItemListAdapter(mContext, dirs));
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mActivity);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                    CloudItem folderChosen;
                    if((which==0)&&(mParentFolder!=null))
                        folderChosen=mParentFolder;
                    else
                        folderChosen = (CloudItem) list.getItemAtPosition(which);

                    mDialog.setContentView(R.layout.choose_folder_dialog_loading);
                    mDialog.setTitle(getDisplayPath(folderChosen));
                    refresh(folderChosen);
                }
            });
            okButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v)
                {
                    setNewPath(settings, mCurrentFolder);
                }
            });
        }
    }

    abstract CloudItem getRootPath();
    abstract String getDirectorySeparator();
    abstract ChooseFolderDialog.FolderFetcherTask getFolderFetcher();

    private String getDisplayPath(CloudItem folder)
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
    }

    public void onCancel(DialogInterface dialog)
    {
        cancelFolderFetcher();
    }

    private void cancelFolderFetcher()
    {
        if(mFolderFetcher!=null) {
            mFolderFetcher.cancel(true);
            mFolderFetcher=null;
        }
    }

    abstract class FolderFetcherTask extends AsyncTask<CloudItem, Void, List<CloudItem>>
    {
        protected abstract List<CloudItem> doInBackground(CloudItem ... args);
        protected void onPostExecute(List<CloudItem> folders)
        {
            mChooseFolderDialogHandler.obtainMessage(BeatPrompterApplication.FOLDER_CONTENTS_FETCHED,folders).sendToTarget();
        }
    }
}