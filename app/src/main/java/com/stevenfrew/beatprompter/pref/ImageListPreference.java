package com.stevenfrew.beatprompter.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.app.AlertDialog.Builder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.stevenfrew.beatprompter.ui.ImageArrayAdapter;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

public class ImageListPreference extends ListPreference {
    private int[] resourceIds = null;

    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference);

        int indexCount=typedArray.getIndexCount();
        Resources resources=context.getResources();
        String packageName=context.getPackageName();
        if(indexCount>0) {
            String[] imageNames = resources.getStringArray(
                    typedArray.getResourceId(indexCount - 1, -1));

            resourceIds = new int[imageNames.length];

            for (int i = 0; i < imageNames.length; i++) {
                String imageName = imageNames[i].substring(
                        imageNames[i].lastIndexOf('/') + 1,
                        imageNames[i].lastIndexOf('.'));

                resourceIds[i] = resources.getIdentifier(imageName,
                        "drawable", packageName);
            }

            typedArray.recycle();
        }
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        ImageView imageView = view.findViewById(R.id.iconImageView);
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(SongList.getContext());
        String value=sharedPrefs.getString(getKey(),"");

        if(value.equals(SongList.getContext().getString(R.string.googleDriveValue)))
            imageView.setImageResource(R.drawable.ic_google_drive);
        else if(value.equals(SongList.getContext().getString(R.string.dropboxValue)))
            imageView.setImageResource(R.drawable.ic_dropbox);
        else if(value.equals(SongList.getContext().getString(R.string.oneDriveValue)))
            imageView.setImageResource(R.drawable.ic_onedrive);
        else
            imageView.setImageResource(R.drawable.blank_icon);
    }

    private boolean inForceUpdate=false;
    public void forceUpdate()
    {
        if(!inForceUpdate) {
            try {

                inForceUpdate = true;
                notifyChanged();
            }
            finally
            {
                inForceUpdate=false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void onPrepareDialogBuilder(Builder builder) {
        int index = findIndexOfValue(getSharedPreferences().getString(
                getKey(), "1"));

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(),
                R.layout.imagelistitem, getEntries(), resourceIds, index);

        // Order matters.
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }
}
