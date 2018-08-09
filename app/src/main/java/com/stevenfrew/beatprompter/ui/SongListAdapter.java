package com.stevenfrew.beatprompter.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.PlaylistNode;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.cache.SongFile;

import java.util.List;

public class SongListAdapter extends ArrayAdapter<PlaylistNode> {
    private final List<PlaylistNode> values;
    private boolean mLargePrint;
    private SharedPreferences sharedPref;

    public SongListAdapter(List<PlaylistNode> playlist) {
        super(BeatPrompterApplication.getContext(), -1, playlist);
        sharedPref= BeatPrompterApplication.getPreferences();
        mLargePrint= sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_key),Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_largePrintList_defaultValue)));
        this.values = playlist;
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) BeatPrompterApplication.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressWarnings("ConstantConditions")
        View rowView = convertView==null?inflater.inflate(mLargePrint?R.layout.song_list_item_large:R.layout.song_list_item, parent, false):convertView;
        TextView artistView = rowView.findViewById(R.id.songartist);
        TextView titleView = rowView.findViewById(R.id.songtitle);
        ImageView beatIcon= rowView.findViewById(R.id.beaticon);
        ImageView docIcon= rowView.findViewById(R.id.smoothicon);
        ImageView notesIcon= rowView.findViewById(R.id.musicicon);
        SongFile song=values.get(position).mSongFile;
        boolean showBeatIcons=sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showBeatStyleIcons_defaultValue)));
        boolean showKey=sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showKeyInList_defaultValue)));
        boolean showMusicIcon=sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showMusicIcon_defaultValue)));
        if((song.mAudioFiles.size()==0)||(!showMusicIcon))
        {
            notesIcon.setVisibility(View.GONE);
//            RelativeLayout.LayoutParams docIconLayoutParams=(RelativeLayout.LayoutParams)docIcon.getLayoutParams();
//            docIconLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//            docIcon.setLayoutParams(docIconLayoutParams);
        }
        else
            notesIcon.setVisibility(View.VISIBLE);
        if((!song.isSmoothScrollable())||(!showBeatIcons))
        {
            docIcon.setVisibility(View.GONE);
//            RelativeLayout.LayoutParams beatIconLayoutParams=(RelativeLayout.LayoutParams)beatIcon.getLayoutParams();
//            beatIconLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//            beatIcon.setLayoutParams(beatIconLayoutParams);
        }
        else
            docIcon.setVisibility(View.VISIBLE);
        if((!song.isBeatScrollable())||(!showBeatIcons)) {
            beatIcon.setVisibility(View.GONE);
        }
        else
            beatIcon.setVisibility(View.VISIBLE);
        titleView.setText(song.mTitle);
        String artist=song.mArtist;
        if(showKey)
        {
            String key=song.mKey;
            if((key!=null)&&(key.length()>0))
                artist+=" - "+key;
        }
        artistView.setText(artist);
        return rowView;
    }
}


