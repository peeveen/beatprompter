package com.stevenfrew.beatprompter;

import android.os.Handler;
import android.util.Log;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

class DropboxDownloadTask extends CloudDownloadTask
{
    private DbxClientV2 mDropboxAPI;

    DropboxDownloadTask(DbxClientV2 dropboxAPI,File targetFolder, Handler handler, String cloudPath, boolean includeSubFolders, CachedFileCollection currentCache, ArrayList<MIDIAlias> defaultMIDIAliases,ArrayList<CachedFile> filesToUpdate)
    {
        super(targetFolder,handler,cloudPath,includeSubFolders,currentCache,defaultMIDIAliases,filesToUpdate);
        mDropboxAPI=dropboxAPI;
    }
    void downloadFiles(String folderID, boolean includeSubfolders, Map<String,File> existingCachedFiles, ArrayList<DownloadedFile> downloadedFiles) throws IOException
    {
        List<String> folderIDs=new ArrayList<>();
        folderIDs.add(folderID);
        List<String> folderNames=new ArrayList<>();
        folderNames.add("");

        while(!folderIDs.isEmpty())
        {
            String currentFolderID=folderIDs.remove(0);
            String currentFolderName=folderNames.remove(0);
            try
            {
                Log.d(BeatPrompterApplication.TAG, "Getting list of everything in Dropbox folder.");
                String[] extsToLookFor = new String[]{".txt", ".mp3", ".wav", ".m4a", ".aac", ".ogg"};
                List<FileMetadata> results = new ArrayList<>();
                ListFolderResult listResult = mDropboxAPI.files().listFolder(currentFolderID);
                while(listResult!=null)
                {
                    List<Metadata> entries=listResult.getEntries();
                    for(Metadata mdata:entries)
                    {
                        if(mdata instanceof FileMetadata)
                        {
                            FileMetadata fmdata=(FileMetadata)mdata;
                            String filename=fmdata.getName().toLowerCase();
                            boolean isSuitableFile=false;
                            for(String ext:extsToLookFor)
                                if(filename.endsWith(ext))
                                {
                                    isSuitableFile = true;
                                    break;
                                }
                            if(isSuitableFile)
                                results.add(fmdata);
                        }
                        else if((mdata instanceof FolderMetadata) && (includeSubfolders))
                        {
                            Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...");
                            folderIDs.add(((FolderMetadata) mdata).getPathLower());
                            folderNames.add(mdata.getName());
                        }
                    }
                    if(listResult.getHasMore())
                        listResult=mDropboxAPI.files().listFolderContinue(listResult.getCursor());
                    else
                        listResult=null;
                }

                for (FileMetadata entry : results) {
                    //if (!entry.isDeleted)
                    {
                        String fileID = entry.getId();
                        Log.d(BeatPrompterApplication.TAG, "File ID: " + fileID);
                        String title = entry.getName();
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
                        Date lastModified = entry.getServerModified();
                        if (existingLocalFile != null) {
                            Date localFileModified = new Date(existingLocalFile.lastModified());
                            Log.d(BeatPrompterApplication.TAG, "Dropbox File was last modified " + lastModified);
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
                            existingLocalFile = downloadDropboxFile(entry, safeFilename);
                            existingCachedFiles.remove(fileID);
                        }

                        if (!audioFile)
                            downloadedFiles.add(new DownloadedFile(existingLocalFile,fileID,lastModified,currentFolderName));
                        else
                            mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                    }
                }
            }
            catch(DbxException de)
            {
                throw new IOException(de.getMessage(),de);
            }
        }
    }

    boolean downloadFile(String fileID,int fileIndex) throws IOException
    {
        boolean noLongerExists=false;
        try {
            Metadata mdata=mDropboxAPI.files().getMetadata(fileID);
            //if ((!entry.isDeleted)&&(!entry.isDir))
            if((mdata!=null) && (mdata instanceof FileMetadata))
            {
                FileMetadata fmdata=(FileMetadata)mdata;
                String title = fmdata.getName();
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
                File existingLocalFile = downloadDropboxFile(fmdata, safeFilename);
                Date lastModified = fmdata.getServerModified();
                if(!onFileDownloaded(existingLocalFile,fileID,lastModified,audioFile))
                    noLongerExists=true;
            } else if(fileIndex==0)
                noLongerExists = true;
        } catch (DbxException ee) {
            throw new IOException(ee.getMessage(),ee);
        }
        return noLongerExists;
    }

    private File downloadDropboxFile(FileMetadata file, String filename) throws IOException, DbxException
    {
        File localfile = new File(mTargetFolder, filename);
        FileOutputStream fos =null;
        try {
            fos = new FileOutputStream(localfile);
            DbxDownloader<FileMetadata> downloader=mDropboxAPI.files().download(file.getId());
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

    String getCloudStorageName()
    {
        return SongList.getContext().getString(R.string.dropbox_string);
    }
}


