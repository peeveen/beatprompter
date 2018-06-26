package com.stevenfrew.beatprompter.cloud;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;

import java.io.File;

public class CloudCacheFolder extends File {
    CloudCacheFolder(File parentFolder,String name)
    {
        super(parentFolder,name);
    }

    public void clear()
    {
        try {
            if (this.exists()) {
                File[] contents = this.listFiles();
                for (File f : contents) {
                    if(!f.isDirectory()) {
                        Log.d(BeatPrompterApplication.TAG, "Deleting " + f.getAbsolutePath());
                        if (!f.delete())
                            Log.e(BeatPrompterApplication.TAG, "Failed to delete " + f.getAbsolutePath());
                    }
                }
            }
        }
        catch(Exception e)
        {
            Log.e(BeatPrompterApplication.TAG,"Failed to clear cache folder.",e);
        }
    }
}
