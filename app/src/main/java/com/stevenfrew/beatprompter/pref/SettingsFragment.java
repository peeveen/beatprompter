package com.stevenfrew.beatprompter.pref;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.v2.DbxClientV2;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.OneDriveClient;
import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;
import com.stevenfrew.beatprompter.cloud.dropbox.DropboxCloudStorage;

import java.util.Arrays;

public class SettingsFragment extends PreferenceFragment implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG="beatprompter";

    IOneDriveClient mOneDriveClient;
    DbxClientV2 mDropboxClient=null;

    GoogleApiClient mGoogleAPIClient = null;
    private com.google.api.services.drive.Drive mGoogleDriveClient=null;
    String mDriveAccountName=null;

    private static final String[] SCOPES = { DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA };

    public Handler mSettingsHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BeatPrompterApplication.SET_CLOUD_PATH:
                    setCloudPath();
                    break;
            }
        }
    };

    SharedPreferences.OnSharedPreferenceChangeListener mCloudPathPrefListener = (sharedPreferences, key) -> {
        if(key.equals(getString(R.string.pref_cloudPath_key)))
            onCloudPathChanged(sharedPreferences.getString(key,null));
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((BeatPrompterApplication)getActivity().getApplicationContext()).setSettingsHandler(mSettingsHandler);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mCloudPathPrefListener);

        String clearCachePrefName=getString(R.string.pref_clearCache_key);
        Preference clearCachePref = findPreference(clearCachePrefName);
        if(clearCachePref!=null) {
            clearCachePref.setOnPreferenceClickListener(preference -> {
                BeatPrompterApplication.mSongListHandler.obtainMessage(BeatPrompterApplication.CLEAR_CACHE).sendToTarget();
                return true;
            });
        }

        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        final Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null) {
            cloudPathPref.setOnPreferenceClickListener(preference -> {
                BeatPrompterApplication.mSettingsHandler.obtainMessage(BeatPrompterApplication.SET_CLOUD_PATH).sendToTarget();
                return true;
            });
        }

        String cloudPrefName=getString(R.string.pref_cloudStorageSystem_key);
        final Preference cloudPref = findPreference(cloudPrefName);
        if(cloudPref!=null) {
            cloudPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,Object value) {
                    SongList.mSongListInstance.deleteAllFiles();
                    SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
                    sharedPref.edit().putString(getString(R.string.pref_cloudStorageSystem_key),value.toString()).apply();
                    getPreferenceManager().getSharedPreferences().edit().putString(getString(R.string.pref_cloudPath_key),null).apply();
                    getPreferenceManager().getSharedPreferences().edit().putString(getString(R.string.pref_cloudDisplayPath_key),null).apply();
                    ((ImageListPreference)cloudPref).forceUpdate();
                    return true;
                }
            });
        }

        String powerwashPrefName=getString(R.string.pref_powerwash_key);
        Preference powerwashPref = findPreference(powerwashPrefName);
        if(powerwashPref!=null) {
            powerwashPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(getString(R.string.pref_cloudStorageSystem_key),"demo");
                    editor.putString(getString(R.string.pref_cloudPath_key), null);
                    editor.putString(getString(R.string.pref_cloudDisplayPath_key), null);
                    editor.apply();

                    SharedPreferences privatePrefs = getActivity().getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
                    editor = privatePrefs.edit();
                    editor.putString(getString(R.string.pref_songSource_key), "");
                    editor.putBoolean(getString(R.string.pref_firstRun_key), true);
                    editor.putString(getString(R.string.pref_dropboxAccessToken_key), null);
                    editor.putBoolean(getString(R.string.pref_wasPowerwashed_key), true);
                    editor.apply();

                    BeatPrompterApplication.mSongListHandler.obtainMessage(BeatPrompterApplication.POWERWASH).sendToTarget();
                    onCloudPathChanged(null);
                    if(cloudPathPref!=null)
                        ((CloudPathPreference)cloudPathPref).forceUpdate();
                    if(cloudPref!=null)
                        ((ImageListPreference)cloudPref).forceUpdate();

                    mDropboxClient=null;
                    mGoogleAPIClient=null;
                    mGoogleDriveClient=null;
                    mOneDriveClient=null;
                    return true;
                }
            });
        }
    }

    @Override
    public void onDestroy()
    {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(mCloudPathPrefListener);
        super.onDestroy();
    }

    void onCloudPathChanged(Object newValue)
    {
        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        String cloudDisplayPathPrefName=getString(R.string.pref_cloudDisplayPath_key);
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
        String displayPath=sharedPrefs.getString(cloudDisplayPathPrefName,null);

        Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null)
            cloudPathPref.setSummary(newValue==null?getString(R.string.no_cloud_folder_currently_set):displayPath);
    }

    void setCloudPath()
    {
        CloudStorage cs=null;
        CloudType cloud=SongList.mSongListInstance.getCloud();
        if(cloud==CloudType.GoogleDrive)
            startGoogleDriveFolderBrowser();
        else if(cloud==CloudType.Dropbox)
            cs=new DropboxCloudStorage(getActivity());
        else if(cloud==CloudType.OneDrive)
        {
            if (mOneDriveClient != null)
                editOneDrivePath();
            else
                initializeOneDriveClient();
        }
        if(cs!=null)
        {
            cs.getFolderSelectionSource().subscribe(this::onCloudFolderSelected);
            cs.selectFolder(getActivity());
        }
        else
            Toast.makeText(getActivity(),getString(R.string.no_cloud_storage_system_set),Toast.LENGTH_LONG).show();
    }

    void initializeOneDriveClient()
    {
        if(mOneDriveClient==null)
        {
            final ICallback<IOneDriveClient> callback = new ICallback<IOneDriveClient>() {
                @Override
                public void success(final IOneDriveClient result) {
                    Log.v(TAG, "Signed in to OneDrive");
                    mOneDriveClient = result;
                    editOneDrivePath();
                }

                @Override
                public void failure(final ClientException error) {
                    mOneDriveClient = null;
                    Log.e(TAG, "Nae luck signing in to OneDrive");
                }
            };

            IClientConfig oneDriveConfig = DefaultClientConfig.
                    createWithAuthenticator(SongList.ONEDRIVE_MSA_AUTHENTICATOR);
            new OneDriveClient.Builder()
                    .fromConfig(oneDriveConfig)
                    .loginAndBuildClient(getActivity(), callback);
        }
        else
            editOneDrivePath();
    }

/*    @Override
    public void onResume()
    {
        super.onResume();
        if(mAuthorizingDropbox)
        {
            mAuthorizingDropbox=false;
            String accessToken= Auth.getOAuth2Token();
            if (accessToken != null)
            {
                SharedPreferences prefs = getActivity().getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID,Context.MODE_PRIVATE);
                prefs.edit().putString(getString(R.string.pref_dropboxAccessToken_key), accessToken).apply();
                //initializeDropboxClient();
            }
        }
    }*/

    void editOneDrivePath()
    {
        //new OneDriveChooseFolderDialog(getActivity(),getActivity(), getString(R.string.pref_cloudPath_key), getString(R.string.pref_cloudDisplayPath_key), mOneDriveClient).showDialog();
    }

    @Override
    public void onStop() {
        if(mGoogleAPIClient!=null)
            mGoogleAPIClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    SettingsActivity.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
        }
        else
        {
            if (SongList.mSongListInstance.wasPowerwashed()) {
                mGoogleAPIClient.clearDefaultAccountAndReconnect();
                return;
            }

            mDriveAccountName = Plus.AccountApi.getAccountName(mGoogleAPIClient);

            if(getGoogleDriveService()!=null) {
                editGoogleDrivePath();
            }
        }
    }

    private com.google.api.services.drive.Drive getGoogleDriveService()
    {
        if((mGoogleDriveClient==null)&&(mDriveAccountName!=null)) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    getActivity().getApplicationContext(), Arrays.asList(SCOPES))
                    .setSelectedAccountName(mDriveAccountName)
                    .setBackOff(new ExponentialBackOff());
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mGoogleDriveClient = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(BeatPrompterApplication.APP_NAME)
                    .build();
        }
        return mGoogleDriveClient;
    }

    void initializeGoogleAPIClient()
    {
        if(mGoogleAPIClient==null)
            mGoogleAPIClient = new GoogleApiClient.Builder(SongList.mSongListInstance)
                    .addApi(Drive.API)
                    .addApi(Plus.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        if(!mGoogleAPIClient.isConnected())
            mGoogleAPIClient.connect();
        else
        {
            if(getGoogleDriveService()!=null) {
                editGoogleDrivePath();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Called whenever the API client fails to connect.
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(getActivity(), SettingsActivity.REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
        }
    }

    void editGoogleDrivePath()
    {
        if (SongList.mSongListInstance.wasPowerwashed()) {
            mGoogleAPIClient.clearDefaultAccountAndReconnect();
            return;
        }
        //GoogleDriveChooseFolderDialog gdcfd=new GoogleDriveChooseFolderDialog(getActivity(),getActivity(), getString(R.string.pref_cloudPath_key), getString(R.string.pref_cloudDisplayPath_key), getGoogleDriveService());
        //gdcfd.showDialog();
    }

    void onCompleteAuthorizationRequestCode()
    {
        if((mGoogleAPIClient!=null)&&(!mGoogleAPIClient.isConnected()))
            mGoogleAPIClient.connect();
        else
            editGoogleDrivePath();
    }

    void onRequestCodeResolution()
    {
        mGoogleAPIClient.connect();
    }

    void onGoogleDrivePermissionGranted()
    {
        startGoogleDriveFolderBrowser();
    }

    void startGoogleDriveFolderBrowser()
    {
        // For some reason, requesting permissions disconnects google drive. Nice!
        if ((mGoogleAPIClient != null) && (mGoogleAPIClient.isConnected())) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.GET_ACCOUNTS)
                    == PackageManager.PERMISSION_GRANTED) {
                mDriveAccountName = Plus.AccountApi.getAccountName(mGoogleAPIClient);
                editGoogleDrivePath();
            }
        }
        else
            initializeGoogleAPIClient();
    }

    void onCloudFolderSelected(CloudFolderInfo folderInfo)
    {
        getPreferenceManager()
                .getSharedPreferences()
                .edit()
                .putString(getString(R.string.pref_cloudPath_key),folderInfo.mID)
                .putString(getString(R.string.pref_cloudDisplayPath_key),folderInfo.mName)
                .apply();
    }
}