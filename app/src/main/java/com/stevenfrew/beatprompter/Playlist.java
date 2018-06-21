package com.stevenfrew.beatprompter;

import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Playlist
{
    private List<PlaylistNode> mItems=new ArrayList<>();

    Playlist()
    {
        buildSongList(new ArrayList<>());
    }

    Playlist(List<SongFile> songs)
    {
        buildSongList(songs);
    }

    PlaylistNode getNodeAt(int position)
    {
        return mItems.get(position);
    }

    void sortByTitle(final String thePrefix)
    {
        ArrayList<SongFile> songs=getSongFiles();
        songs.sort((song1, song2) -> {
            String title1 = song1.mTitle.toLowerCase();
            String title2 = song2.mTitle.toLowerCase();
            if (title1.startsWith(thePrefix))
                title1 = title1.substring(thePrefix.length());
            if (title2.startsWith(thePrefix))
                title2 = title2.substring(thePrefix.length());
            return title1.compareTo(title2);
        });
        buildSongList(songs);
    }

    void sortByArtist(final String thePrefix)
    {
        ArrayList<SongFile> songs=getSongFiles();
        songs.sort((song1, song2) -> {
            String artist1 = song1.mArtist.toLowerCase();
            String artist2 = song2.mArtist.toLowerCase();
            if (artist1.startsWith(thePrefix))
                artist1 = artist1.substring(thePrefix.length());
            if (artist2.startsWith(thePrefix))
                artist2 = artist2.substring(thePrefix.length());
            return artist1.compareTo(artist2);
        });
        buildSongList(songs);
    }

    void sortByKey()
    {
        ArrayList<SongFile> songs=getSongFiles();
        songs.sort(Comparator.comparing(song -> (song.mKey == null ? "" : song.mKey)));
        buildSongList(songs);
    }

    void sortByDateModified()
    {
        ArrayList<SongFile> songs=getSongFiles();
        songs.sort((song1, song2) -> song2.mLastModified.compareTo(song1.mLastModified));
        buildSongList(songs);
    }

    private ArrayList<SongFile> getSongFiles()
    {
        ArrayList<SongFile> songs=new ArrayList<>();
        for(PlaylistNode node:mItems)
            songs.add(node.mSongFile);
        return songs;
    }

    List<PlaylistNode> getNodesAsArray()
    {
        return mItems;
    }

    private void buildSongList(List<SongFile> songs)
    {
        mItems.clear();
        PlaylistNode lastNode=null;
        for(SongFile sf:songs)
        {
            PlaylistNode node=new PlaylistNode(sf);
            node.mPrevNode=lastNode;
            if(lastNode!=null)
                lastNode.mNextNode=node;
            mItems.add(node);
            lastNode=node;
        }
    }
}
