package com.stevenfrew.beatprompter;

import android.os.Handler;
import android.util.Log;

import com.onedrive.sdk.extensions.IItemCollectionPage;
import com.onedrive.sdk.extensions.IItemCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

class OneDriveDownloadTask extends CloudDownloadTask
{
    private IOneDriveClient mOneDriveClient;

    OneDriveDownloadTask(IOneDriveClient oneDriveClient,File targetFolder, Handler handler, String cloudPath, boolean includeSubFolders, CachedFileCollection currentCache, ArrayList<MIDIAlias> defaultMIDIAliases,ArrayList<CachedFile> filesToUpdate)
    {
        super(targetFolder,handler,cloudPath,includeSubFolders,currentCache,defaultMIDIAliases,filesToUpdate);
        mOneDriveClient=oneDriveClient;
    }
    void downloadFiles(String folderID, boolean includeSubfolders, Map<String,File> existingCachedFiles, ArrayList<DownloadedFile> downloadedFiles) throws IOException
    {
        List<String> folderIDs=new ArrayList<>();
        folderIDs.add(folderID);
        List<String> folderNames=new ArrayList<>();
        folderNames.add("");

        while(!folderIDs.isEmpty())
        {
            String currentFolderID = folderIDs.remove(0);
            String currentFolderName=folderNames.remove(0);

            Log.d(BeatPrompterApplication.TAG, "Getting list of everything in OneDrive folder.");
            IItemCollectionPage page = mOneDriveClient.getDrive().getItems(currentFolderID).getChildren().buildRequest().get();
            List<Item> items = new ArrayList<>();
            while (page != null) {
                List<Item> children = page.getCurrentPage();
                for (Item child : children) {
                    if (child.file != null) {
                        if ((child.name.toLowerCase().endsWith(".txt")) || (child.audio != null))
                            items.add(child);
                    } else if ((child.folder != null) && (includeSubfolders)){
                        Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                        folderIDs.add(child.id);
                        folderNames.add(child.name);
                    }
                }
                IItemCollectionRequestBuilder builder = page.getNextPage();
                if (builder != null)
                    page = builder.buildRequest().get();
                else
                    page = null;
            }

            for (Item item : items) {
                //if (!entry.isDeleted)
                {
                    String fileID = item.id;
                    Log.d(BeatPrompterApplication.TAG, "File ID: " + fileID);
                    String title = item.name;
                    String lowerCaseTitle = title.toLowerCase();
                    boolean audioFile = false;
                    for (String ext : AUDIO_FILE_EXTENSIONS)
                        if (lowerCaseTitle.endsWith(ext))
                            audioFile = true;
                    this.publishProgress(String.format(SongList.getContext().getString(R.string.checking), title));
                    String safeFilename = makeSafeFilename(title);
                    Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);
                    File existingLocalFile = existingCachedFiles.get(fileID);
                    boolean downloadRequired = true;
                    Date lastModified = item.lastModifiedDateTime.getTime();
                    if (existingLocalFile != null) {
                        Date localFileModified = new Date(existingLocalFile.lastModified());
                        Log.d(BeatPrompterApplication.TAG, "OneDrive File was last modified " + lastModified);
                        Log.d(BeatPrompterApplication.TAG, "Local File was last downloaded " + localFileModified);
                        if (localFileModified.after(lastModified)) {
                            Log.d(BeatPrompterApplication.TAG, "It hasn't changed since last download ... ignoring!");
                            downloadRequired = false;
                            existingCachedFiles.remove(fileID);
                        } else
                            Log.d(BeatPrompterApplication.TAG, "Looks like it has changed since last download ... re-downloading!");
                    } else
                        Log.d(BeatPrompterApplication.TAG, "Appears to be a file that I don't have yet... downloading!");

                    if (downloadRequired) {
                        Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
                        this.publishProgress(String.format(SongList.getContext().getString(R.string.downloading), title));
                        existingLocalFile = downloadOneDriveFile(item, safeFilename);
                        existingCachedFiles.remove(fileID);
                    }

                    if (!audioFile)
                        downloadedFiles.add(new DownloadedFile(existingLocalFile,fileID,lastModified,currentFolderName));
                    else
                        mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                }
            }
        }
    }

    boolean downloadFile(String fileID,int fileIndex) throws IOException
    {
        boolean noLongerExists=false;
        Item item=mOneDriveClient.getDrive().getItems(fileID).buildRequest().get();
        if((item!=null) && (item.file!=null))
        {
            String title = item.name;
            Log.d(BeatPrompterApplication.TAG, "File title: " + title);
            String lowerCaseTitle = title.toLowerCase();
            boolean audioFile = false;
            if(mUpdateType==CachedFileType.Song) {
                for (String ext : AUDIO_FILE_EXTENSIONS)
                    if (lowerCaseTitle.endsWith(ext))
                        audioFile = true;
            }
            this.publishProgress(String.format(SongList.getContext().getString(R.string.checking),title));
            String safeFilename = makeSafeFilename(title);
            Log.d(BeatPrompterApplication.TAG, "Safe filename: " + safeFilename);

            Log.d(BeatPrompterApplication.TAG, "Downloading now ...");
            this.publishProgress(String.format(SongList.getContext().getString(R.string.downloading),title));
            File existingLocalFile = downloadOneDriveFile(item, safeFilename);
            Date lastModified = item.lastModifiedDateTime.getTime();
            if(!onFileDownloaded(existingLocalFile,fileID,lastModified,audioFile))
                noLongerExists=true;
        } else if(fileIndex==0)
            noLongerExists = true;
        return noLongerExists;
    }

    private File downloadOneDriveFile(Item file, String filename) throws IOException
    {
        File localfile = new File(mTargetFolder, filename);
        FileOutputStream fos =null;
        InputStream inputStream=null;
        try {
            fos = new FileOutputStream(localfile);
            inputStream=mOneDriveClient.getDrive().getItems(file.id).getContent().buildRequest().get();
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
        return localfile;
    }

    String getCloudStorageName()
    {
        return SongList.getContext().getString(R.string.onedrive_string);
    }
}
