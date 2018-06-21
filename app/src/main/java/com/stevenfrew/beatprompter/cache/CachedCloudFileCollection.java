package com.stevenfrew.beatprompter.cache;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.*;

public class CachedCloudFileCollection
{
    private List<CachedCloudFile> mFiles=new ArrayList<>();

    public void writeToXML(Document d,Element root)
    {
        for(CachedCloudFile ccf:mFiles)
            ccf.writeToXML(d,root);
    }

    public void add(CachedCloudFile cachedFile)
    {
        for(int f=mFiles.size()-1;f>=0;--f)
        {
            CachedCloudFile existingFile=mFiles.get(f);
            if(cachedFile.mID.equalsIgnoreCase(existingFile.mID)) {
                mFiles.set(f, cachedFile);
                return;
            }
        }
        mFiles.add(cachedFile);
    }

    public void remove(CloudFileInfo cloudFile)
    {
        for(int f=mFiles.size()-1;f>=0;--f)
        {
            CachedCloudFile existingFile=mFiles.get(f);
            if(cloudFile.mID.equalsIgnoreCase(existingFile.mID))
                mFiles.remove(f);
        }
    }

    public boolean hasLatestVersionOf(CloudFileInfo cloudFile)
    {
        return mFiles.stream().anyMatch(f->f.mID.equalsIgnoreCase(cloudFile.mID) && f.mLastModified.equals(cloudFile.mLastModified));
    }

    public List<SongFile> getSongFiles()
    {
        return mFiles.stream().filter(f->f instanceof SongFile).map(f->(SongFile)f).collect(Collectors.toList());
    }

    public List<SetListFile> getSetListFiles()
    {
        return mFiles.stream().filter(f->f instanceof SetListFile).map(f->(SetListFile)f).collect(Collectors.toList());
    }

    public List<MIDIAliasFile> getMIDIAliasFiles()
    {
        return mFiles.stream().filter(f->f instanceof MIDIAliasFile).map(f->(MIDIAliasFile)f).collect(Collectors.toList());
    }

    public List<AudioFile> getAudioFiles()
    {
        return mFiles.stream().filter(f->f instanceof AudioFile).map(f->(AudioFile)f).collect(Collectors.toList());
    }

    public List<ImageFile> getImageFiles()
    {
        return mFiles.stream().filter(f->f instanceof ImageFile).map(f->(ImageFile)f).collect(Collectors.toList());
    }

    public void removeNonExistent(Set<String> storageIDs)
    {
        List<CachedCloudFile> remainingFiles=mFiles.stream().filter(f->storageIDs.contains(f.mID)).collect(Collectors.toList());
        List<CachedCloudFile> noLongerExistingFiles=mFiles.stream().filter(f->!storageIDs.contains(f.mID)).collect(Collectors.toList());
        noLongerExistingFiles.forEach(f->{if(!f.mFile.delete())
            Log.e(BeatPrompterApplication.TAG, "Failed to delete file: "+f.mFile.getName());});
        mFiles=remainingFiles;
    }

    public void clear()
    {
        mFiles.clear();
    }
}
