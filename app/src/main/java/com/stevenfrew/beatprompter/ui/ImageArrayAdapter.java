package com.stevenfrew.beatprompter.ui;

import android.app.Activity;

import android.content.Context;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.stevenfrew.beatprompter.R;

public class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
    private int index;
    private int[] resourceIds;

    public ImageArrayAdapter(Context context, int textViewResourceId,
                             CharSequence[] objects, int[] ids, int i) {
        super(context, textViewResourceId, objects);
        index = i;
        resourceIds = ids;
    }

    @Override @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
        View row = convertView==null?inflater.inflate(R.layout.imagelistitem, parent, false):convertView;

        ImageView imageView = row.findViewById(R.id.image);
        imageView.setImageResource(resourceIds[position]);

        CheckedTextView checkedTextView = row.findViewById(
                R.id.check);

        checkedTextView.setText(getItem(position));
        checkedTextView.setChecked(position == index);

        return row;
    }
}
