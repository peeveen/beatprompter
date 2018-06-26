package com.stevenfrew.beatprompter.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import java.util.List;

public class MIDIAliasListAdapter extends ArrayAdapter<com.stevenfrew.beatprompter.cache.MIDIAliasFile> {
    private final List<com.stevenfrew.beatprompter.cache.MIDIAliasFile> values;
    private boolean mLargePrint;

    public MIDIAliasListAdapter(List<com.stevenfrew.beatprompter.cache.MIDIAliasFile> fileList) {
        super(SongList.mSongListInstance,-1, fileList);
        SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(SongList.mSongListInstance);
        mLargePrint= sharedPref.getBoolean(SongList.mSongListInstance.getString(R.string.pref_largePrintList_key),Boolean.parseBoolean(SongList.mSongListInstance.getString(R.string.pref_largePrintList_defaultValue)));
        this.values = fileList;
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) SongList.mSongListInstance
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView==null?inflater.inflate(mLargePrint?R.layout.midi_alias_list_item_large:R.layout.midi_alias_list_item, parent, false):convertView;
        TextView titleView = rowView.findViewById(R.id.alias_file_name);
        ImageView errorIcon=rowView.findViewById(R.id.erroricon);
        com.stevenfrew.beatprompter.cache.MIDIAliasFile maf=values.get(position);
        if(maf.getErrors().size()==0)
            errorIcon.setVisibility(View.GONE);
        titleView.setText(maf.getAliasSetName());
        return rowView;
    }
}


