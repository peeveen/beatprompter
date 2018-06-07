package com.stevenfrew.beatprompter;

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

import java.util.ArrayList;
import java.util.List;

class MIDIAliasListAdapter extends ArrayAdapter<MIDIAliasCachedCloudFile> {
    private final Context context;
    private final List<MIDIAliasCachedCloudFile> values;
    private boolean mLargePrint;

    MIDIAliasListAdapter(Context context, List<MIDIAliasCachedCloudFile> fileList) {
        super(context, -1, fileList);
        SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(context);
        mLargePrint= sharedPref.getBoolean(context.getString(R.string.pref_largePrintList_key),Boolean.parseBoolean(context.getString(R.string.pref_largePrintList_defaultValue)));
        this.context = context;
        this.values = fileList;
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView==null?inflater.inflate(mLargePrint?R.layout.midi_alias_list_item_large:R.layout.midi_alias_list_item, parent, false):convertView;
        TextView titleView = (TextView) rowView.findViewById(R.id.alias_file_name);
        ImageView errorIcon=(ImageView) rowView.findViewById(R.id.erroricon);
        MIDIAliasCachedCloudFile maf=values.get(position);
        if(maf.getErrors().size()==0)
            errorIcon.setVisibility(View.GONE);
        titleView.setText(maf.getAliasSetName());
        return rowView;
    }
}


