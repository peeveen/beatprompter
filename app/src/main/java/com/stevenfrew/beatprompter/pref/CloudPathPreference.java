package com.stevenfrew.beatprompter.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

public class CloudPathPreference extends Preference
{
    public CloudPathPreference(Context context, AttributeSet attrs) {
        super(context,attrs);
     }
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        ImageView imageView = view.findViewById(R.id.iconImageView);
        imageView.setImageResource(R.drawable.blank_icon);
        TextView textView = view.findViewById(android.R.id.summary);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(SongList.getContext());
        String path=prefs.getString(SongList.getContext().getString(R.string.pref_cloudPath_key),null);
        String displayPath=prefs.getString(SongList.getContext().getString(R.string.pref_cloudDisplayPath_key),null);
        if(path==null)
            displayPath=SongList.getContext().getString(R.string.no_cloud_folder_currently_set);
        textView.setText(displayPath);
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
}
