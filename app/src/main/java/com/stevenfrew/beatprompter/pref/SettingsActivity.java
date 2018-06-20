package com.stevenfrew.beatprompter.pref;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity
{
    static final int REQUEST_CODE_RESOLUTION = 1;
    static final int COMPLETE_AUTHORIZATION_REQUEST_CODE=3;
    public static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS=4;

    SettingsFragment mFragment=new SettingsFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mFragment)
                .commit();
    }
}
