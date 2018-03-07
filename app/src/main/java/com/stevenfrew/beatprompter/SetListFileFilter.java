package com.stevenfrew.beatprompter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class SetListFileFilter extends SetListFilter {
    ArrayList<String> mMissingSongs=new ArrayList<>();
    SetListFile mSetListFile;
    boolean mWarned=false;

    SetListFileFilter(SetListFile file,ArrayList<SongFile> songs)
    {
        super(file.mSetTitle,getSongList(file.mSongTitles,songs));
        mMissingSongs=getMissingSongList(file.mSongTitles,songs);
        mWarned=(mMissingSongs.size()==0);
        mSetListFile=file;
    }

    private static ArrayList<SongFile> getSongList(ArrayList<String> titles,ArrayList<SongFile> songFiles)
    {
        Map<String,SongFile> songsByTitle=new HashMap<>();
        for(SongFile sf:songFiles)
            songsByTitle.put(normalizeTitle(sf.mTitle),sf);
        ArrayList<SongFile> foundSongs=new ArrayList<>();
        for(String title:titles)
        {
            SongFile sf=songsByTitle.get(normalizeTitle(title));
            if(sf!=null)
                foundSongs.add(sf);
        }
        return foundSongs;
    }

    private static ArrayList<String> getMissingSongList(ArrayList<String> titles,ArrayList<SongFile> songFiles)
    {
        Map<String,SongFile> songsByTitle=new HashMap<>();
        for(SongFile sf:songFiles)
            songsByTitle.put(normalizeTitle(sf.mTitle),sf);
        ArrayList<String> missingSongs=new ArrayList<>();
        for(String title:titles)
        {
            SongFile sf=songsByTitle.get(normalizeTitle(title));
            if(sf==null)
                missingSongs.add(title);
        }
        return missingSongs;
    }

    private static String normalizeTitle(String title)
    {
        String normalized=title.replace('’','\'');
        normalized = normalized.replace("\uFEFF", "");
        return normalized.toLowerCase();
    }
}
