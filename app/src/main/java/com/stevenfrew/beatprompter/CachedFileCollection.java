package com.stevenfrew.beatprompter;

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
}
