package com.stevenfrew.beatprompter;

import android.util.Log;

import java.util.ArrayList;
import org.w3c.dom.*;

class CachedFileCollection
{
    public ArrayList<AudioFile> mAudioFiles=new ArrayList<>();
    public ArrayList<ImageFile> mImageFiles=new ArrayList<>();
    public ArrayList<SongFile> mSongs=new ArrayList<>();
    public ArrayList<MIDIAliasCachedFile> mMIDIAliasCachedFiles=new ArrayList<>();
    public ArrayList<SetListFile> mSets=new ArrayList<>();

    CachedFileCollection()
    {
    }

    CachedFileCollection(ArrayList<SongFile> songs,ArrayList<AudioFile> audioFiles, ArrayList<SetListFile> sets,ArrayList<MIDIAliasCachedFile> midiAliasFiles,ArrayList<ImageFile> imageFiles)
    {
        mSongs=songs;
        mAudioFiles=audioFiles;
        mSets=sets;
        mMIDIAliasCachedFiles=midiAliasFiles;
        mImageFiles=imageFiles;
    }

    void writeToXML(Document d,Element root)
    {
        for(SongFile s:mSongs)
            s.writeToXML(d,root);
        for(AudioFile a:mAudioFiles)
            a.writeToXML(d,root);
        for(ImageFile i:mImageFiles)
            i.writeToXML(d,root);
        for(SetListFile s:mSets)
            s.writeToXML(d,root);
        for(MIDIAliasCachedFile maf:mMIDIAliasCachedFiles)
            maf.writeToXML(d,root);
    }

    void add(CachedFile cachedFile)
    {
        ArrayList targetCollection=getTargetCollection(cachedFile.getFileType());
        insertItemIntoCollection(targetCollection,cachedFile);
    }

    void remove(CachedFile cachedFile)
    {
        ArrayList targetCollection=getTargetCollection(cachedFile.getFileType());
        removeItemFromCollection(targetCollection,cachedFile.mStorageName);
        if(!cachedFile.mFile.delete())
            Log.e(BeatPrompterApplication.TAG, "Failed to delete file.");
    }

    ArrayList getTargetCollection(CachedFileType fileType)
    {
        if(fileType==CachedFileType.Song)
            return mSongs;
        else if(fileType==CachedFileType.Audio)
            return mAudioFiles;
        else if(fileType==CachedFileType.SetList)
            return mSets;
        else if(fileType==CachedFileType.MIDIAliases)
            return mMIDIAliasCachedFiles;
        return mImageFiles;
    }

    void removeItemFromCollection(ArrayList targetCollection,String storageID)
    {
        for(int f=targetCollection.size()-1;f>=0;--f)
        {
            CachedFile cachedFile=(CachedFile)targetCollection.get(f);
            if(cachedFile.mStorageName.equalsIgnoreCase(storageID))
                targetCollection.remove(f);
        }
    }

    void insertItemIntoCollection(ArrayList targetCollection,CachedFile file)
    {
        for(int f=targetCollection.size()-1;f>=0;--f)
        {
            CachedFile cachedFile=(CachedFile)targetCollection.get(f);
            if(cachedFile.mStorageName.equalsIgnoreCase(file.mStorageName)) {
                targetCollection.set(f, file);
                return;
            }
        }
        targetCollection.add(file);
    }
}
