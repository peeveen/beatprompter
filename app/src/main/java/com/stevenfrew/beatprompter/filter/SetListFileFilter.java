package com.stevenfrew.beatprompter.filter;

import com.stevenfrew.beatprompter.cache.SetListFile;
import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetListFileFilter extends SetListFilter {
    public List<String> mMissingSongs;
    public SetListFile mSetListFile;
    public boolean mWarned;

    public SetListFileFilter(SetListFile file,List<SongFile> songs)
    {
        super(file.mSetTitle,getSongList(file.mSongTitles,songs));
        mMissingSongs=getMissingSongList(file.mSongTitles,songs);
        mWarned=(mMissingSongs.size()==0);
        mSetListFile=file;
    }

    private static List<SongFile> getSongList(List<String> titles,List<SongFile> songFiles)
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

    private static List<String> getMissingSongList(List<String> titles,List<SongFile> songFiles)
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
