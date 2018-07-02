package com.stevenfrew.beatprompter.pref;

import android.os.Message;

import com.stevenfrew.beatprompter.EventHandler;

public class SettingsEventHandler extends EventHandler {
    private SettingsFragment mFragment;

    SettingsEventHandler(SettingsFragment fragment)
    {
        mFragment=fragment;
    }

    public void handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case EventHandler.SET_CLOUD_PATH:
                mFragment.setCloudPath();
                break;
        }
    }

}
