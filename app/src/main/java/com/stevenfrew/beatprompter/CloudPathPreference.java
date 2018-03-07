package com.stevenfrew.beatprompter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class CloudPathPreference extends Preference
{
    private Context mContext;

    public CloudPathPreference(Context context, AttributeSet attrs) {
        super(context,attrs);
        mContext=context;
    }
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        ImageView imageView = (ImageView) view.findViewById(R.id.iconImageView);
        imageView.setImageResource(R.drawable.blank_icon);
        TextView textView = (TextView) view.findViewById(android.R.id.summary);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(mContext);
        String path=prefs.getString(mContext.getString(R.string.pref_cloudPath_key),null);
        String displayPath=prefs.getString(mContext.getString(R.string.pref_cloudDisplayPath_key),null);
        if(path==null)
            displayPath=mContext.getString(R.string.no_cloud_folder_currently_set);
        textView.setText(displayPath);
    }

    private boolean inForceUpdate=false;
    void forceUpdate()
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
