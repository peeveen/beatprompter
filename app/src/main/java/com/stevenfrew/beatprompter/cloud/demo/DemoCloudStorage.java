package com.stevenfrew.beatprompter.cloud.demo;

import android.app.Activity;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudItemInfo;
import com.stevenfrew.beatprompter.cloud.CloudListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import io.reactivex.subjects.PublishSubject;

public class DemoCloudStorage extends CloudStorage {

    private final static String DEMO_SONG_TEXT_ID="demoSongText";
    private final static String DEMO_SONG_AUDIO_ID="demoSongAudio";
    private final static String DEMO_SONG_FILENAME="demo_song.txt";
    private final static String DEMO_SONG_AUDIO_FILENAME="demo_song.mp3";

    public DemoCloudStorage(Activity parentActivity)
    {
        super(parentActivity,"demo");

    }
    @Override
    public String getCloudStorageName() {
        // No need for region strings here.
        return "demo";
    }

    @Override
    public String getDirectorySeparator() {
        return "/";
    }

    @Override
    public int getCloudIconResourceId() {
        // Nobody will ever see this. Any icon will do.
        return R.drawable.ic_dropbox;
    }

    @Override
    protected void getRootPath(CloudListener listener, PublishSubject<CloudFolderInfo> rootPathSource) {
        rootPathSource.onNext(new CloudFolderInfo(null,"/","",""));
    }

    @Override
    protected void downloadFiles(List<CloudFileInfo> filesToRefresh, CloudListener cloudListener, PublishSubject<CloudDownloadResult> itemSource, PublishSubject<String> messageSource) {
        for(CloudFileInfo cloudFile:filesToRefresh)
        {
            if(cloudFile.mID.equalsIgnoreCase(DEMO_SONG_TEXT_ID)) {
                messageSource.onNext(SongList.mSongListInstance.getString(R.string.downloading, DEMO_SONG_FILENAME));
                itemSource.onNext(new CloudDownloadResult(cloudFile, createDemoSongTextFile()));
            }
            else if(cloudFile.mID.equalsIgnoreCase(DEMO_SONG_AUDIO_ID))
                try {
                    messageSource.onNext(SongList.mSongListInstance.getString(R.string.downloading, DEMO_SONG_AUDIO_FILENAME));
                    itemSource.onNext(new CloudDownloadResult(cloudFile, createDemoSongAudioFile()));
                }
                catch(IOException ioe)
                {
                    itemSource.onError(ioe);
                }
        }
        itemSource.onComplete();
    }

    @Override
    protected void readFolderContents(CloudFolderInfo folder, CloudListener listener, PublishSubject<CloudItemInfo> itemSource, boolean includeSubfolders, boolean returnFolders) {
        itemSource.onNext(new CloudFileInfo(DEMO_SONG_TEXT_ID,DEMO_SONG_FILENAME, new Date(),""));
        itemSource.onNext(new CloudFileInfo(DEMO_SONG_AUDIO_ID,DEMO_SONG_AUDIO_FILENAME, new Date(),""));
        itemSource.onComplete();
    }

    private File createDemoSongTextFile()
    {
        String demoFileText= SongList.mSongListInstance.getString(R.string.demo_song);
        File destinationSongFile = new File(mCloudCacheFolder, DEMO_SONG_FILENAME);
        BufferedWriter bw=null;
        try
        {
            bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationSongFile)));
            bw.write(demoFileText);
        }
        catch(Exception e)
        {
            Log.d(BeatPrompterApplication.TAG,"Failed to create demo file",e);
            destinationSongFile=null;
        }
        finally
        {
            if(bw!=null)
                try
                {
                    bw.close();
                }
                catch(Exception e)
                {
                    Log.d(BeatPrompterApplication.TAG,"Failed to close demo file",e);
                }
        }
        return destinationSongFile;
    }

    private File createDemoSongAudioFile() throws IOException
    {
        File destinationAudioFile = new File(mCloudCacheFolder, DEMO_SONG_AUDIO_FILENAME);
        SongList.copyAssetsFileToLocalFolder(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile);
        return destinationAudioFile;
    }
}