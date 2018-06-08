package com.stevenfrew.beatprompter.cloud;

import android.content.Context;
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

class CloudBrowserItemListAdapter extends ArrayAdapter<CloudBrowserItem> {

    CloudBrowserItemListAdapter(List<CloudBrowserItem> items) {
        super(SongList.getContext(), -1, items);
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) SongList.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView==null?inflater.inflate(R.layout.cloud_browser_item, parent, false):convertView;
        TextView textView = (TextView) rowView.findViewById(R.id.file_or_folder_name);
        ImageView imageView=(ImageView) rowView.findViewById(R.id.file_or_folder_icon);
        CloudBrowserItem folder=this.getItem(position);
        textView.setText(folder.mDisplayName);
        boolean isFolder=folder.mIsFolder;
        textView.setEnabled(isFolder);
        imageView.setEnabled(isFolder);
        //rowView.setEnabled(isFolder);
        if(isFolder)
            imageView.setImageResource(R.drawable.ic_folder);
        else
            imageView.setImageResource(R.drawable.ic_document);
        return rowView;
    }

    @Override
    public boolean isEnabled(int position)
    {
        return getItem(position).mIsFolder;
    }
}


