package com.stevenfrew.beatprompter;

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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case COMPLETE_AUTHORIZATION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mFragment.onCompleteAuthorizationRequestCode();
                }
                break;
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    mFragment.onRequestCodeResolution();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mFragment.onGoogleDrivePermissionGranted();
                }
            }
        }
    }

}
