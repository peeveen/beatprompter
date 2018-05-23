package com.stevenfrew.beatprompter;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

abstract class CloudDownloadTask extends AsyncTask<String, String, Boolean>
{
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private Exception mCloudSyncException=null;
    private String mCloudPath;
    private boolean mIncludeSubFolders;
    private CachedFileCollection mCurrentCache;
    private String mSubfolderOrigin;
    private ArrayList<CachedFile> mFilesToUpdate;
    File mTargetFolder;
    private ArrayList<MIDIAlias> mDefaultMIDIAliases;
    private boolean mNoLongerExists=false;
    private CachedFile mUpdatedFile=null;
    CachedFileType mUpdateType;

    private ArrayList<SongFile> mDownloadedSongs=new ArrayList<>();
    private ArrayList<MIDIAliasCachedFile> mDownloadedMIDIAliasCachedFiles=new ArrayList<>();
    private ArrayList<SetListFile> mDownloadedSets=new ArrayList<>();
    ArrayList<AudioFile> mDownloadedAudioFiles=new ArrayList<>();
    ArrayList<ImageFile> mDownloadedImageFiles=new ArrayList<>();

    final static String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    final static String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    CloudDownloadTask(File targetFolder,Handler handler, String cloudPath, boolean includeSubFolders,CachedFileCollection currentCache,ArrayList<MIDIAlias> defaultMIDIAliases,ArrayList<CachedFile> filesToUpdate)
    {
        mTargetFolder=targetFolder;
        mHandler=handler;
        mCloudPath=cloudPath;
        mDefaultMIDIAliases=defaultMIDIAliases;
        mFilesToUpdate=filesToUpdate;
        mUpdateType = CachedFileType.None;
        mSubfolderOrigin=null;
        if((mFilesToUpdate!=null)&&(!mFilesToUpdate.isEmpty())) {
            CachedFile mainFile=mFilesToUpdate.get(0);
            mUpdateType = mainFile.getFileType();
            mSubfolderOrigin=mainFile.mSubfolder;
        }
        mIncludeSubFolders=includeSubFolders;
        mCurrentCache=currentCache;
    }

    @Override
    protected Boolean doInBackground(String... paramParams) {

        if(mUpdateType==CachedFileType.None)
            updateEntireCache();
        else
            updateSelectedFiles();
        return true;
    }

    private void updateEntireCache()
    {
        Map<String,File> localCacheContentsByStorageID =new HashMap<>();
        ArrayList<CachedFile> cachedFiles=new ArrayList<>();
        cachedFiles.addAll(mCurrentCache.mSongs);
        cachedFiles.addAll(mCurrentCache.mMIDIAliasCachedFiles);
        cachedFiles.addAll(mCurrentCache.mSets);
        cachedFiles.addAll(mCurrentCache.mAudioFiles);
        cachedFiles.addAll(mCurrentCache.mImageFiles);
        for(CachedFile cf: cachedFiles) {
            Log.d(BeatPrompterApplication.TAG, "Existing file: " + cf.mStorageName+"="+cf.mFile.getAbsolutePath());
            localCacheContentsByStorageID.put(cf.mStorageName, cf.mFile);
        }

        ArrayList<DownloadedFile> downloadedFilesToParse=new ArrayList<>();
        try
        {
            downloadFiles(mCloudPath,mIncludeSubFolders,localCacheContentsByStorageID,downloadedFilesToParse);
        }
        catch(Exception e)
        {
            mCloudSyncException=e;
        }

        // If we didn't manage to download ANYTHING, then DON'T blast everything away.
        // Also check for whether there was nothing new to download, but maybe some files were removed?
        for (DownloadedFile downloadedFile : downloadedFilesToParse) {
            try {
                MIDIAliasCachedFile maf = new MIDIAliasCachedFile(SongList.getContext(), downloadedFile, mDefaultMIDIAliases);
                mDownloadedMIDIAliasCachedFiles.add(maf);
            } catch (InvalidBeatPrompterFileException ibpfe) {
                // Not a MIDI alias file. Might be a song file?
                try {
                    SongFile song = new SongFile(SongList.getContext(), downloadedFile, mDownloadedAudioFiles, mDownloadedImageFiles);
                    mDownloadedSongs.add(song);
                } catch (InvalidBeatPrompterFileException ibpfe2) {
                    // Not a song file ... might be a set file?
                    try {
                        SetListFile slf = new SetListFile(SongList.getContext(), downloadedFile);
                        mDownloadedSets.add(slf);
                    } catch (InvalidBeatPrompterFileException ibpfe3) {
                        Log.e(BeatPrompterApplication.TAG, ibpfe2.getMessage());
                    }
                } catch (IOException ioe) {
                    // Failed to read the file
                    Log.e(BeatPrompterApplication.TAG, ioe.getMessage());
                }
            }
        }
        // Don't clear out the "leftovers" if there was a sync error ... we simply might not have reached those files yet before
        // the sync failed.
        if(mCloudSyncException==null) {
            // Any entries left in the map did not exist on the Drive. So delete them
            for (File fileToDelete : localCacheContentsByStorageID.values()) {
                Log.d(BeatPrompterApplication.TAG, "Deleting local file that doesn't match anything on cloud: " + fileToDelete.getAbsolutePath());
                this.publishProgress(String.format(SongList.getContext().getString(R.string.deleting), fileToDelete.getName()));
                if (!fileToDelete.delete())
                    Log.e(BeatPrompterApplication.TAG, "Failed to delete file.");
            }
        }
        else {
            // Better stick all the files from the previous cache back into the new cache.
            // To do this, we need to treat them as "downloaded".
            // Watch out in case we DID download a new version of them. Don't add them twice.
            for(CachedFile macf: mCurrentCache.mMIDIAliasCachedFiles) {
                boolean found=false;
                for (CachedFile macf2 : mDownloadedMIDIAliasCachedFiles) {
                    if (macf2.mStorageName.equals(macf.mStorageName)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    mDownloadedMIDIAliasCachedFiles.add((MIDIAliasCachedFile)macf);
            }
            for(CachedFile macf: mCurrentCache.mSets) {
                boolean found=false;
                for (CachedFile macf2 : mDownloadedSets) {
                    if (macf2.mStorageName.equals(macf.mStorageName)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    mDownloadedSets.add((SetListFile)macf);
            }
            for(CachedFile macf: mCurrentCache.mSongs) {
                boolean found=false;
                for (CachedFile macf2 : mDownloadedSongs) {
                    if (macf2.mStorageName.equals(macf.mStorageName)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    mDownloadedSongs.add((SongFile)macf);
            }
            for(CachedFile macf: mCurrentCache.mAudioFiles) {
                boolean found=false;
                for (CachedFile macf2 : mDownloadedAudioFiles) {
                    if (macf2.mStorageName.equals(macf.mStorageName)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    mDownloadedAudioFiles.add((AudioFile)macf);
            }
            for(CachedFile macf: mCurrentCache.mImageFiles) {
                boolean found=false;
                for (CachedFile macf2 : mDownloadedImageFiles) {
                    if (macf2.mStorageName.equals(macf.mStorageName)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    mDownloadedImageFiles.add((ImageFile)macf);
            }

            mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, mCloudSyncException.getMessage()).sendToTarget();
        }
    }

    private void updateSelectedFiles()
    {
        for(int f=0;(f<mFilesToUpdate.size()) && (!mNoLongerExists);++f) {
            try {
                CachedFile fileToUpdate = mFilesToUpdate.get(f);
                String fileID=fileToUpdate.mStorageName;
                Log.d(BeatPrompterApplication.TAG, "File ID: " + fileID);
                mNoLongerExists = downloadFile(fileID,f);
            }
            catch (Exception ee) {
                Log.d(BeatPrompterApplication.TAG, "An error occurred: " + ee);
                if(f==0)
                    mNoLongerExists = true;
                mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR,ee.getMessage()).sendToTarget();
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mProgressDialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean b) {
        super.onPostExecute(b);
        if (mProgressDialog!=null)
            mProgressDialog.dismiss();

        CachedFileCollection returnCache;
        if(mUpdateType==CachedFileType.None) {
            // TODO: Callback?
            returnCache = new CachedFileCollection(mDownloadedSongs, mDownloadedAudioFiles, mDownloadedSets, mDownloadedMIDIAliasCachedFiles, mDownloadedImageFiles);
        }
        else{
            if(mUpdateType==CachedFileType.Song) {
                for (int f = mCurrentCache.mSongs.size() - 1; f >= 0; --f) {
                    SongFile sf = mCurrentCache.mSongs.get(f);
                    if ((mUpdatedFile != null) && (sf.mStorageName.equals(mUpdatedFile.mStorageName))) {
                        mCurrentCache.mSongs.set(f, (SongFile)mUpdatedFile);
                        break;
                    }
                    if ((mNoLongerExists) && (sf.mStorageName.equals(mFilesToUpdate.get(0).mStorageName))) {
                        mCurrentCache.mSongs.remove(f);
                    }
                }
            }
            else if(mUpdateType==CachedFileType.SetList)
            {
                for (int f = mCurrentCache.mSets.size() - 1; f >= 0; --f) {
                    SetListFile sf =  mCurrentCache.mSets.get(f);
                    if ((mUpdatedFile != null) && (sf.mStorageName.equals(mUpdatedFile.mStorageName))) {
                        mCurrentCache.mSets.set(f, (SetListFile)mUpdatedFile);
                        break;
                    }
                    if ((mNoLongerExists) && (sf.mStorageName.equals(mFilesToUpdate.get(0).mStorageName))) {
                        mCurrentCache.mSets.remove(f);
                    }
                }
            }
            else if(mUpdateType==CachedFileType.MIDIAliases)
            {
                for (int f = mCurrentCache.mMIDIAliasCachedFiles.size() - 1; f >= 0; --f) {
                    MIDIAliasCachedFile maf = mCurrentCache.mMIDIAliasCachedFiles.get(f);
                    if ((mUpdatedFile != null) && (maf.mStorageName.equals(mUpdatedFile.mStorageName))) {
                        mCurrentCache.mMIDIAliasCachedFiles.set(f,(MIDIAliasCachedFile) mUpdatedFile);
                        break;
                    }
                    if ((mNoLongerExists) && (maf.mStorageName.equals(mFilesToUpdate.get(0).mStorageName))) {
                        mCurrentCache.mMIDIAliasCachedFiles.remove(f);
                    }
                }
            }
            returnCache=mCurrentCache;
        }

        mHandler.obtainMessage(BeatPrompterApplication.CACHE_UPDATED,returnCache).sendToTarget();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(SongList.getContext());
        mProgressDialog.setTitle(SongList.getContext().getString(R.string.downloadingFiles));
        mProgressDialog.setMessage(String.format(SongList.getContext().getString(R.string.accessingCloudStorage),getCloudStorageName()));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    void updateDownloadProgress(String filename)
    {
        publishProgress(String.format(SongList.getContext().getString(R.string.downloading),filename));
    }

    private static final String ReservedChars = "|\\?*<\":>+[]/'";
    String makeSafeFilename(String str)
    {
        StringBuilder builder=new StringBuilder();
        for(char c:str.toCharArray())
            if(ReservedChars.contains(""+c))
                builder.append("_");
            else
                builder.append(c);
        return builder.toString();
    }

    boolean onFileDownloaded(File file, String fileID, Date lastModified, boolean dependencyFile)
    {
        if(mUpdateType==CachedFileType.Song)
        {
            if (dependencyFile)
                return true;
            try {
                mUpdatedFile = new SongFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin), null,null);
                return true;
            } catch (Exception ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        else if(mUpdateType==CachedFileType.SetList) {
            try {
                mUpdatedFile = new SetListFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin));
                return true;
            } catch (InvalidBeatPrompterFileException ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        else if(mUpdateType==CachedFileType.MIDIAliases)
        {
            try {
                mUpdatedFile = new MIDIAliasCachedFile(SongList.getContext(), new DownloadedFile(file, fileID, lastModified,mSubfolderOrigin),mDefaultMIDIAliases);
                return true;
            } catch (InvalidBeatPrompterFileException ibpfe) {
                Log.e(BeatPrompterApplication.TAG, ibpfe.getMessage());
            }
        }
        return false;
    }

    abstract boolean downloadFile(String fileID,int fileIndex) throws IOException;

    abstract String getCloudStorageName();

    abstract void downloadFiles(String folderID,boolean includeSubfolders,Map<String,File> existingCachedFiles,ArrayList<DownloadedFile> downloadedFiles) throws IOException;
}

