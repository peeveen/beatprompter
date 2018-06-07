package com.stevenfrew.beatprompter;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.*;

class CachedCloudFileCollection
{
    private List<CachedCloudFile> mFiles=new ArrayList<>();

    void writeToXML(Document d,Element root)
    {
        for(CachedCloudFile ccf:mFiles)
            ccf.writeToXML(d,root);
    }

    void add(CachedCloudFile cachedFile)
    {
        for(int f=mFiles.size()-1;f>=0;--f)
        {
            CachedCloudFile existingFile=mFiles.get(f);
            if(cachedFile.mStorageID.equalsIgnoreCase(existingFile.mStorageID)) {
                mFiles.set(f, cachedFile);
                return;
            }
        }
        mFiles.add(cachedFile);
    }

    boolean hasLatestVersionOf(CloudFileInfo cloudFile)
    {
        return mFiles.stream().anyMatch(f->f.mStorageID.equalsIgnoreCase(cloudFile.mStorageID) && f.mLastModified.equals(cloudFile.mLastModified));
    }

    List<SongFile> getSongFiles()
    {
        return mFiles.stream().filter(f->f instanceof SongFile).map(f->(SongFile)f).collect(Collectors.toList());
    }

    List<SetListFile> getSetListFiles()
    {
        return mFiles.stream().filter(f->f instanceof SetListFile).map(f->(SetListFile)f).collect(Collectors.toList());
    }

    List<MIDIAliasCachedCloudFile> getMIDIAliasFiles()
    {
        return mFiles.stream().filter(f->f instanceof MIDIAliasCachedCloudFile).map(f->(MIDIAliasCachedCloudFile)f).collect(Collectors.toList());
    }

    List<AudioFile> getAudioFiles()
    {
        return mFiles.stream().filter(f->f instanceof AudioFile).map(f->(AudioFile)f).collect(Collectors.toList());
    }

    List<ImageFile> getImageFiles()
    {
        return mFiles.stream().filter(f->f instanceof ImageFile).map(f->(ImageFile)f).collect(Collectors.toList());
    }

    void removeNonExistent(Set<String> storageIDs)
    {
        List<CachedCloudFile> remainingFiles=mFiles.stream().filter(f->storageIDs.contains(f.mStorageID)).collect(Collectors.toList());
        List<CachedCloudFile> noLongerExistingFiles=mFiles.stream().filter(f->!storageIDs.contains(f.mStorageID)).collect(Collectors.toList());
        noLongerExistingFiles.forEach(f->{if(!f.mFile.delete())
            Log.e(BeatPrompterApplication.TAG, "Failed to delete file: "+f.mFile.getName());});
        mFiles=remainingFiles;
    }

    void clear()
    {
        mFiles.clear();
    }
}
