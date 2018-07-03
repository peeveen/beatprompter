package com.stevenfrew.beatprompter.cache;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;

import java.io.File;
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

    public void readFromXML(Document xmlDoc)
    {
        clear();

        NodeList songFiles = xmlDoc.getElementsByTagName(SongFile.SONGFILE_ELEMENT_TAG_NAME);
        for (int f = 0; f < songFiles.getLength(); ++f) {
            Node n = songFiles.item(f);
            SongFile song = new SongFile((Element)n);
            add(song);
        }
        NodeList setFiles = xmlDoc.getElementsByTagName(SetListFile.SETLISTFILE_ELEMENT_TAG_NAME);
        for (int f = 0; f < setFiles.getLength(); ++f) {
            Node n = setFiles.item(f);
            try {
                SetListFile set = new SetListFile((Element) n);
                add(set);
            }
            catch(InvalidBeatPrompterFileException ibpfe)
            {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG,"Failed to parse set-list file.");
            }
        }
        NodeList imageFiles = xmlDoc.getElementsByTagName(ImageFile.IMAGEFILE_ELEMENT_TAG_NAME);
        for (int f = 0; f < imageFiles.getLength(); ++f) {
            Node n = imageFiles.item(f);
            ImageFile imageFile = new ImageFile((Element)n);
            add(imageFile);
        }
        NodeList audioFiles = xmlDoc.getElementsByTagName(AudioFile.AUDIOFILE_ELEMENT_TAG_NAME);
        for (int f = 0; f < audioFiles.getLength(); ++f) {
            Node n = audioFiles.item(f);
            AudioFile audioFile = new AudioFile((Element)n);
            add(audioFile);
        }
        NodeList aliasFiles = xmlDoc.getElementsByTagName(MIDIAliasFile.MIDIALIASFILE_ELEMENT_TAG_NAME);
        for (int f = 0; f < aliasFiles.getLength(); ++f) {
            Node n = aliasFiles.item(f);
            try{
                MIDIAliasFile midiAliasCachedCloudFile = new MIDIAliasFile((Element)n);
                add(midiAliasCachedCloudFile);
            }
            catch(InvalidBeatPrompterFileException ibpfe)
            {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG,"Failed to parse MIDI alias file.");
            }
        }
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

    private List<AudioFile> getAudioFiles()
    {
        return mFiles.stream().filter(f->f instanceof AudioFile).map(f->(AudioFile)f).collect(Collectors.toList());
    }

    private List<ImageFile> getImageFiles()
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

    public boolean isEmpty()
    {
        return mFiles.isEmpty();
    }

    public AudioFile getMappedAudioFilename(String in,ArrayList<AudioFile> tempAudioFileCollection)
    {
        if(in!=null) {
            for (AudioFile afm : getAudioFiles()) {
                String secondChance = in.replace('’', '\'');
                if ((afm.mName.equalsIgnoreCase(in)) || (afm.mName.equalsIgnoreCase(secondChance)))
                    return afm;
            }
            if(tempAudioFileCollection!=null)
                for (AudioFile afm : tempAudioFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((afm.mName.equalsIgnoreCase(in)) || (afm.mName.equalsIgnoreCase(secondChance)))
                        return afm;
                }
        }
        return null;
    }

    public ImageFile getMappedImageFilename(String in,ArrayList<ImageFile> tempImageFileCollection)
    {
        if(in!=null) {
            for (ImageFile ifm : getImageFiles()) {
                String secondChance = in.replace('’', '\'');
                if ((ifm.mName.equalsIgnoreCase(in)) || (ifm.mName.equalsIgnoreCase(secondChance)))
                    return ifm;
            }
            if(tempImageFileCollection!=null)
                for (ImageFile ifm : tempImageFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((ifm.mName.equalsIgnoreCase(in)) || (ifm.mName.equalsIgnoreCase(secondChance)))
                        return ifm;
                }
        }
        return null;
    }

    public ArrayList<CachedCloudFile> getFilesToRefresh(CachedCloudFile fileToRefresh, boolean includeDependencies)
    {
        ArrayList<CachedCloudFile> filesToRefresh=new ArrayList<>();
        if(fileToRefresh!=null)
        {
            filesToRefresh.add(fileToRefresh);
            if((fileToRefresh instanceof SongFile) && (includeDependencies))
            {
                SongFile song=(SongFile)fileToRefresh;
                if (song.mAudioFiles != null)
                    for (String audioFileName : song.mAudioFiles) {
                        AudioFile audioFile = getMappedAudioFilename(audioFileName, null);
                        File actualAudioFile = null;
                        if (audioFile != null)
                            actualAudioFile = new File(song.mFile.getParent(), audioFile.mFile.getName());
                        if ((actualAudioFile != null) && (actualAudioFile.exists()))
                            filesToRefresh.add(audioFile);
                    }
                if (song.mImageFiles != null)
                    for (String imageFileName : song.mImageFiles) {
                        ImageFile imageFile = getMappedImageFilename(imageFileName, null);
                        File actualImageFile = null;
                        if (imageFile != null)
                            actualImageFile = new File(song.mFile.getParent(), imageFile.mFile.getName());
                        if ((actualImageFile != null) && (actualImageFile.exists()))
                            filesToRefresh.add(imageFile);
                    }
            }
        }
        return filesToRefresh;
    }


}
