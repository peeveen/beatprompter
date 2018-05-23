package com.stevenfrew.beatprompter;

import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

import java.util.Arrays;

class GoogleDriveWrapper {

    static final int REQUEST_CODE_RESOLUTION = 1;
    static final int COMPLETE_AUTHORIZATION_REQUEST_CODE=2;
    private static final String[] SCOPES = { DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA };

    private static com.google.api.services.drive.Drive mGoogleDriveService = null;
    private static GoogleApiClient mGoogleAPIClient = null;
    private static Context mApplicationContext;
    private static GoogleDriveConnectionListener mConnectionListener=new GoogleDriveConnectionListener();

    static class GoogleDriveConnectionListener implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener
    {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            // Called whenever the API client fails to connect.
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection failed: " + result.toString());
            if (!result.hasResolution()) {
                // show the localized error dialog.
                GoogleApiAvailability.getInstance().getErrorDialog(SongList.mSongListInstance, result.getErrorCode(), 0).show();
                return;
            }
            // The failure has a resolution. Resolve it.
            // Called typically when the app is not yet authorized, and an
            // authorization
            // dialog is displayed to the user.
            try {
                Log.i(BeatPrompterApplication.TAG, "GoogleApiClient starting connection resolution ...");
                result.startResolutionForResult(SongList.mSongListInstance, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                Log.e(BeatPrompterApplication.TAG, "Exception while starting resolution activity", e);
            }
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.i(BeatPrompterApplication.TAG, "API client connected.");

            if (SongList.mSongListInstance.wasPowerwashed()) {
                mGoogleAPIClient.clearDefaultAccountAndReconnect();
                return;
            }

            try {
                SongList.mSongListInstance.performCloudSync();
            }
            catch(Exception se)
            {
                Toast.makeText(SongList.mSongListInstance,se.getMessage(),Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection suspended");
        }
    }

    static void initialize(Context applicationContext)
    {
        mApplicationContext=applicationContext;

        mGoogleAPIClient = new GoogleApiClient.Builder(applicationContext)
                .addApi(Drive.API)
                .addApi(Plus.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(mConnectionListener)
                .addOnConnectionFailedListener(mConnectionListener)
                .build();

    }

    static void connectClient()
    {
        if (mGoogleAPIClient != null)
            mGoogleAPIClient.connect();
    }

    static void disconnectClient()
    {
        if (mGoogleAPIClient != null)
            mGoogleAPIClient.disconnect();
    }

    static boolean isConnected()
    {
        return mGoogleAPIClient.isConnected();
    }

    static com.google.api.services.drive.Drive getGoogleDriveService()
    {
        if(mGoogleDriveService==null) {
            String accountName=Plus.AccountApi.getAccountName(mGoogleAPIClient);
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    mApplicationContext, Arrays.asList(SCOPES))
                    .setSelectedAccountName(accountName)
                    .setBackOff(new ExponentialBackOff());
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mGoogleDriveService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(BeatPrompterApplication.APP_NAME)
                    .build();
        }
        return mGoogleDriveService;
    }

    static void recoverAuthorization(UserRecoverableAuthIOException uraioe)
    {
        SongList.mSongListInstance.startActivityForResult(uraioe.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
    }
}
