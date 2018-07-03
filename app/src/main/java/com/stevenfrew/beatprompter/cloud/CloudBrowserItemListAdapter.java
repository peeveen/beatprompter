package com.stevenfrew.beatprompter.cloud;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;

import java.util.List;

class CloudBrowserItemListAdapter extends ArrayAdapter<CloudItemInfo> {

    CloudBrowserItemListAdapter(List<CloudItemInfo> items) {
        super(BeatPrompterApplication.getContext(), -1, items);
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) BeatPrompterApplication.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView==null?inflater.inflate(R.layout.cloud_browser_item, parent, false):convertView;
        TextView textView =  rowView.findViewById(R.id.file_or_folder_name);
        ImageView imageView=rowView.findViewById(R.id.file_or_folder_icon);
        CloudItemInfo cloudItem=this.getItem(position);
        if(cloudItem!=null) {
            textView.setText(cloudItem.mName);
            boolean isFolder = cloudItem instanceof CloudFolderInfo;
            textView.setEnabled(isFolder);
            imageView.setEnabled(isFolder);
            //rowView.setEnabled(isFolder);
            if (isFolder)
                imageView.setImageResource(R.drawable.ic_folder);
            else
                imageView.setImageResource(R.drawable.ic_document);
        }
        return rowView;
    }

    @Override
    public boolean isEnabled(int position)
    {
        return getItem(position) instanceof CloudFolderInfo;
    }
}


