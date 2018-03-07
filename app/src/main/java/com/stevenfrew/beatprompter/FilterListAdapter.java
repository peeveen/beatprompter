package com.stevenfrew.beatprompter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class FilterListAdapter extends ArrayAdapter<Filter> {
    private final Context context;
    private final ArrayList<Filter> values;

    FilterListAdapter(Context context, ArrayList<Filter> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView==null?inflater.inflate(R.layout.filter_item_selected, parent, false):convertView;
        TextView titleView = (TextView) rowView.findViewById(R.id.filtertitleselected);
        Filter filter=values.get(position);
        titleView.setText(filter.mName);
        return rowView;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dropDownView = convertView==null?inflater.inflate(R.layout.filter_list_item, parent, false):convertView;
        TextView titleView = (TextView) dropDownView.findViewById(R.id.filtertitle);
        ImageView filterIcon=(ImageView) dropDownView.findViewById(R.id.filterIcon);
        Filter filter=values.get(position);
        if(filter instanceof TagFilter)
            filterIcon.setImageResource(R.drawable.tag);
        else if(filter instanceof TemporarySetListFilter)
            filterIcon.setImageResource(R.drawable.pencil);
        else if(filter instanceof SetListFilter)
            filterIcon.setImageResource(R.drawable.ic_document);
        else if(filter instanceof MIDIAliasFilesFilter)
            filterIcon.setImageResource(R.drawable.midi);
        else if(filter instanceof FolderFilter)
            filterIcon.setImageResource(R.drawable.ic_folder);
        else
            filterIcon.setImageResource(R.drawable.blank_icon);
        titleView.setText(filter.mName);
        return dropDownView;

    }
}


