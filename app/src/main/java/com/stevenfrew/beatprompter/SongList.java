package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.vending.billing.IInAppBillingService;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.*;
import com.google.api.services.drive.model.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
import com.onedrive.sdk.extensions.IItemCollectionPage;
import com.onedrive.sdk.extensions.IItemCollectionRequestBuilder;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;
import com.onedrive.sdk.extensions.OneDriveClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK;

public class SongList extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener,AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    boolean mFullVersionUnlocked=true;
    public GoogleApiClient mGoogleApiClient = null;
    private boolean mMidiUsbRegistered=false;
    private Context mContext;
    private com.google.api.services.drive.Drive mGoogleDriveService = null;
    String mDriveAccountName=null;
    boolean mFetchDependenciesToo=false;
    boolean mSongListActive=false;
    public static boolean mSongEndedNaturally=false;
    SongFile mSongToRefresh=null;
    Menu mMenu=null;
    public static SongList mSongListInstance=null;
    SetListFile mSetToRefresh=null;
    MIDIAliasCachedFile mMIDIAliasCachedFileToRefresh=null;
    Filter mSelectedFilter=null;
    SortingPreference mSortingPreference=SortingPreference.TITLE;
    public static ArrayList<AudioFile> mAudioFiles=new ArrayList<>();
    public static ArrayList<ImageFile> mImageFiles=new ArrayList<>();
    ArrayList<SongFile> mSongs=new ArrayList<>();
    static ArrayList<MIDIAlias> mDefaultAliases;
    ArrayList<MIDIAliasCachedFile> mMIDIAliasCachedFiles=new ArrayList<>();
    ArrayList<SetListFile> mSets=new ArrayList<>();
    Playlist mPlaylist=new Playlist();
    PlaylistNode mNowPlayingNode=null;
    ArrayList<Filter> mFilters=new ArrayList<>();
    TemporarySetListFilter mTemporarySetListFilter=null;
    BaseAdapter mListAdapter=null;
    private DbxClientV2 mDropboxAPI;
    IOneDriveClient mOneDriveClient;

    UsbManager mUsbManager;
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;
    LoadingSongFile mSongToLoadOnResume=null;

    final static String GOOGLE_DRIVE_FOLDER_MIMETYPE="application/vnd.google-apps.folder";
    final static String ONEDRIVE_CLIENT_ID = "dc584873-700c-4377-98da-d088cca5c1f5"; //This is your client ID
    final static MSAAuthenticator ONEDRIVE_MSA_AUTHENTICATOR = new MSAAuthenticator()
    {
        @Override
        public String getClientId() {
            return ONEDRIVE_CLIENT_ID;
        }

        @Override
        public String[] getScopes() {
            return new String[] { "onedrive.readonly","wl.offline_access" };
        }
    };

    /* Configurations */

    public Handler mSongListHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BeatPrompterApplication.BLUETOOTH_MESSAGE_RECEIVED:
                    processBluetoothMessage((BluetoothMessage)msg.obj);
                    break;
                case BeatPrompterApplication.CLIENT_CONNECTED:
                    Toast.makeText(SongList.this,msg.obj+" "+getString(R.string.hasConnected),Toast.LENGTH_LONG).show();
                    updateBluetoothIcon();
                    break;
                case BeatPrompterApplication.CLIENT_DISCONNECTED:
                    Toast.makeText(SongList.this,msg.obj+" "+getString(R.string.hasDisconnected),Toast.LENGTH_LONG).show();
                    updateBluetoothIcon();
                    break;
                case BeatPrompterApplication.SERVER_DISCONNECTED:
                    Toast.makeText(SongList.this,getString(R.string.disconnectedFromBandLeader)+" "+msg.obj,Toast.LENGTH_LONG).show();
                    updateBluetoothIcon();
                    break;
                case BeatPrompterApplication.SERVER_CONNECTED:
                    Toast.makeText(SongList.this,getString(R.string.connectedToBandLeader)+" "+msg.obj,Toast.LENGTH_LONG).show();
                    updateBluetoothIcon();
                    break;
                case BeatPrompterApplication.CLOUD_SYNC_ERROR:
                    AlertDialog.Builder adb = new AlertDialog.Builder(SongList.this);
                    adb.setMessage(String.format(getString(R.string.cloudSyncErrorMessage),(String)msg.obj));
                    adb.setTitle(getString(R.string.cloudSyncErrorTitle));
                    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }});
                    AlertDialog ad=adb.create();
                    ad.setCanceledOnTouchOutside(true);
                    ad.show();
//                    Toast.makeText(SongList.this,msg.obj.toString(),Toast.LENGTH_LONG).show();
                    break;
                case BeatPrompterApplication.SONG_LOAD_FAILED:
                    Toast.makeText(SongList.this,msg.obj.toString(),Toast.LENGTH_LONG).show();
                    break;
                case BeatPrompterApplication.MIDI_LSB_BANK_SELECT:
                    BeatPrompterApplication.mMidiBankLSBs[msg.arg1]=(byte)msg.arg2;
                    break;
                case BeatPrompterApplication.MIDI_MSB_BANK_SELECT:
                    BeatPrompterApplication.mMidiBankMSBs[msg.arg1]=(byte)msg.arg2;
                    break;
                case BeatPrompterApplication.MIDI_PROGRAM_CHANGE:
                    startSongViaMidiProgramChange(BeatPrompterApplication.mMidiBankMSBs[msg.arg1],BeatPrompterApplication.mMidiBankLSBs[msg.arg1],(byte)msg.arg2,(byte)msg.arg1);
                    break;
                case BeatPrompterApplication.MIDI_SONG_SELECT:
                    startSongViaMidiSongSelect((byte)msg.arg1);
                    break;
                case BeatPrompterApplication.SONG_LOAD_COMPLETED:
                    startSongActivity();
                    break;
                case BeatPrompterApplication.POWERWASH:
                    powerwash();
                    break;
                case BeatPrompterApplication.CLEAR_CACHE:
                    clearCache();
                    break;
            }
        }
    };

    MIDIUSBInTask mMidiUsbInTask=null;
    MIDIUSBOutTask mMidiUsbOutTask=new MIDIUSBOutTask();
    MIDIInTask mMidiInTask=new MIDIInTask(mSongListHandler);
    MIDISongDisplayInTask mMidiSongDisplayInTask=new MIDISongDisplayInTask();
    SongLoaderTask mSongLoaderTask=null;
    SongLoadTask mSongLoadTask=null;
    Object mSongLoadSyncObject=new Object();
    Thread mMidiUsbInTaskThread=null;
    Thread mMidiUsbOutTaskThread=new Thread(mMidiUsbOutTask);
    Thread mMidiInTaskThread=new Thread(mMidiInTask);
    Thread mMidiSongDisplayInTaskThread=new Thread(mMidiSongDisplayInTask);
    Thread mSongLoaderTaskThread=null;

    final static private String FULL_VERSION_SKU_NAME="full_version";

    private final static String DEMO_SONG_FILENAME="demo_song.txt";
    private final static String DEMO_SONG_AUDIO_FILENAME="demo_song.mp3";

    private final static String DEFAULT_MIDI_ALIASES_FILENAME="default_midi_aliases.txt";

    private static UsbInterface getDeviceMidiInterface(UsbDevice device) {
        int interfacecount = device.getInterfaceCount();
        UsbInterface fallbackInterface = null;
        for (int h = 0; h < interfacecount; ++h) {
            UsbInterface face = device.getInterface(h);
            int mainclass = face.getInterfaceClass();
            int subclass = face.getInterfaceSubclass();
            // Oh you f***in beauty, we've got a perfect compliant MIDI interface!
            if ((mainclass == 1) && (subclass == 3))
                return face;
                // Aw bollocks, we've got some vendor-specific pish.
                // Still worth trying.
            else if ((mainclass == 255) && (fallbackInterface == null)) {
                // Basically, go with this if:
                // It has all endpoints of type "bulk transfer"
                // and
                // The endpoints have a max packet size that is a mult of 4.
                int endPointCount = face.getEndpointCount();
                boolean allEndpointsCheckout = true;
                for (int g = 0; g < endPointCount; ++g) {
                    UsbEndpoint ep = face.getEndpoint(g);
                    int maxPacket = ep.getMaxPacketSize();
                    int type = ep.getType();
                    allEndpointsCheckout &= ((type == USB_ENDPOINT_XFER_BULK) && ((maxPacket & 3) == 0));
                }
                if (allEndpointsCheckout)
                    fallbackInterface = face;
            }
        }
        return fallbackInterface;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                attemptUsbMidiConnection();
            }
            else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                mMidiUsbOutTask.setConnection(null,null);
                Task.stopTask(mMidiUsbInTask,mMidiUsbInTaskThread);
                mMidiUsbInTask=null;
                mMidiUsbInTaskThread=null;
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbInterface midiInterface=getDeviceMidiInterface(device);
                            if(midiInterface!=null)
                            {
                                UsbDeviceConnection conn = mUsbManager.openDevice(device);
                                if (conn != null)
                                {
                                    if (conn.claimInterface(midiInterface, true))
                                    {
                                        int endpointCount = midiInterface.getEndpointCount();
                                        for (int f = 0; f < endpointCount; ++f) {
                                            UsbEndpoint endPoint = midiInterface.getEndpoint(f);
                                            if (endPoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                                mMidiUsbOutTask.setConnection(conn,endPoint);
                                            } else if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                                if (mMidiUsbInTask == null) {
                                                    mMidiUsbInTask = new MIDIUSBInTask(conn, endPoint);
                                                    (mMidiUsbInTaskThread = new Thread(mMidiUsbInTask)).start();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if((mSelectedFilter!=null)&&(mSelectedFilter instanceof MIDIAliasFilesFilter))
        {
            final MIDIAliasCachedFile maf=mMIDIAliasCachedFiles.get(position);
            if(maf.getErrors().size()>0)
                showMIDIAliasErrors(maf.getErrors());
        }
        else {
            // Don't allow another song to be started from the song list (by clicking)
            // if one is already loading. The only circumstances this is allowed is via
            // MIDI triggers.
            if(!songCurrentlyLoading()) {
                PlaylistNode selectedNode = mPlaylist.getNodeAt(position);
                playPlaylistNode(selectedNode, false);
            }
        }
    }

    boolean songCurrentlyLoading()
    {
        synchronized(mSongLoadSyncObject)
        {
            return mSongLoadTask!=null;
        }
    }

    void startSongActivity() {
        Intent i = new Intent(getApplicationContext(), SongDisplayActivity.class);
        i.putExtra("registered", fullVersionUnlocked());
        startActivityForResult(i, PLAY_SONG_REQUEST_CODE);
    }

    void startSongViaMidiProgramChange(byte bankMSB,byte bankLSB,byte program, byte channel)
    {
        startSongViaMidiSongTrigger(new MIDISongTrigger(bankMSB,bankLSB,program,false,channel));
    }

    void startSongViaMidiSongSelect(byte song)
    {
        startSongViaMidiSongTrigger(new MIDISongTrigger((byte)0,(byte)0,song,true,(byte)0));
    }

    void startSongViaMidiSongTrigger(MIDISongTrigger mst)
    {
        for(PlaylistNode node:mPlaylist.getNodesAsArray())
            if(node.mSongFile.matchesTrigger(mst)) {
                playPlaylistNode(node,true);
                return;
            }
        // Otherwise, it might be a song that is not currently onscreen.
        // Still play it though!
        for(SongFile sf:mSongs)
            if(sf.matchesTrigger(mst)) {
                playSongFile(sf, null, true);
            }
    }

    public void playPlaylistNode(PlaylistNode node,boolean startedByMidiTrigger)
    {
        SongFile selectedSong=node.mSongFile;
        playSongFile(selectedSong,node,startedByMidiTrigger);
    }

    void playSongFile(SongFile selectedSong,PlaylistNode node,boolean startedByMidiTrigger)
    {
        String trackName=null;
        if((selectedSong.mAudioFiles!=null)&&(selectedSong.mAudioFiles.size()>0))
            trackName=selectedSong.mAudioFiles.get(0);
        boolean beatScroll=selectedSong.isBeatScrollable();
        boolean smoothScroll=selectedSong.isSmoothScrollable();
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        boolean manualMode=sharedPrefs.getBoolean(getString(R.string.pref_manualMode_key), false);
        if(manualMode) {
            beatScroll = smoothScroll = false;
            trackName = null;
        }
        ScrollingMode scrollingMode=beatScroll?ScrollingMode.Beat:(smoothScroll?ScrollingMode.Smooth:ScrollingMode.Manual);
        SongDisplaySettings sds=getSongDisplaySettings(scrollingMode);
        playSong(node,selectedSong, trackName,scrollingMode,false,startedByMidiTrigger,sds,sds);
    }

    private boolean shouldPlayNextSong()
    {
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        String playNextSongPref=sharedPrefs.getString(getString(R.string.pref_automaticallyPlayNextSong_key),getString(R.string.pref_automaticallyPlayNextSong_defaultValue));
        boolean playNextSong=false;
        if(playNextSongPref.equals(getString(R.string.playNextSongAlwaysValue)))
            playNextSong=true;
        else if(playNextSongPref.equals(getString(R.string.playNextSongSetListsOnlyValue)))
            playNextSong=(mSelectedFilter!=null)&&(mSelectedFilter instanceof SetListFilter);
        return playNextSong;
    }

    boolean isDemoSong(SongFile songFile)
    {
        return (songFile!=null) && (mSongs.size()==1) &&
                (
                        songFile.mTitle.equalsIgnoreCase("BeatPrompter Demo Song") ||
                                songFile.mTitle.equalsIgnoreCase("BeatPrompter Demo-Song") ||
                                songFile.mTitle.equalsIgnoreCase("BeatPrompter Canción De Demo") ||
                                songFile.mTitle.equalsIgnoreCase("BeatPrompter Morceau De Démo") ||
                                songFile.mTitle.equalsIgnoreCase("BeatPrompter Demo Song") ||
                                songFile.mTitle.equalsIgnoreCase("BeatPrompter Música De Demo"));
    }

    SongDisplaySettings getSongDisplaySettings(ScrollingMode scrollMode)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean onlyUseBeatFontSizes = sharedPref.getBoolean(getString(R.string.pref_alwaysUseBeatFontPrefs_key), Boolean.parseBoolean(getString(R.string.pref_alwaysUseBeatFontPrefs_defaultValue)));

        int fontSizeMin = Integer.parseInt(getString(R.string.fontSizeMin));
        int minimumFontSizeBeat = sharedPref.getInt(getString(R.string.pref_minFontSize_key), Integer.parseInt(getString(R.string.pref_minFontSize_default)));
        minimumFontSizeBeat += fontSizeMin;
        int maximumFontSizeBeat = sharedPref.getInt(getString(R.string.pref_maxFontSize_key), Integer.parseInt(getString(R.string.pref_maxFontSize_default)));
        maximumFontSizeBeat += fontSizeMin;
        int minimumFontSizeSmooth = sharedPref.getInt(getString(R.string.pref_minFontSizeSmooth_key), Integer.parseInt(getString(R.string.pref_minFontSizeSmooth_default)));
        minimumFontSizeSmooth += fontSizeMin;
        int maximumFontSizeSmooth = sharedPref.getInt(getString(R.string.pref_maxFontSizeSmooth_key), Integer.parseInt(getString(R.string.pref_maxFontSizeSmooth_default)));
        maximumFontSizeSmooth += fontSizeMin;
        int minimumFontSizeManual = sharedPref.getInt(getString(R.string.pref_minFontSizeManual_key), Integer.parseInt(getString(R.string.pref_minFontSizeManual_default)));
        minimumFontSizeManual += fontSizeMin;
        int maximumFontSizeManual = sharedPref.getInt(getString(R.string.pref_maxFontSizeManual_key), Integer.parseInt(getString(R.string.pref_maxFontSizeManual_default)));
        maximumFontSizeManual += fontSizeMin;

        if (onlyUseBeatFontSizes) {
            minimumFontSizeSmooth = minimumFontSizeManual=minimumFontSizeBeat;
            maximumFontSizeSmooth = maximumFontSizeManual=maximumFontSizeBeat;
        }

        int minimumFontSize,maximumFontSize;
        if (scrollMode == ScrollingMode.Beat) {
            minimumFontSize = minimumFontSizeBeat;
            maximumFontSize = maximumFontSizeBeat;
        }
        else if (scrollMode == ScrollingMode.Smooth) {
            minimumFontSize = minimumFontSizeSmooth;
            maximumFontSize = maximumFontSizeSmooth;
        }
        else
        {
            minimumFontSize = minimumFontSizeManual;
            maximumFontSize = maximumFontSizeManual;
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return new SongDisplaySettings(getResources().getConfiguration().orientation,minimumFontSize,maximumFontSize,size.x,size.y);
    }

    private void playSong(PlaylistNode selectedNode,SongFile selectedSong,String trackName, ScrollingMode scrollMode,boolean startedByBandLeader,boolean startedByMidiTrigger,SongDisplaySettings nativeSettings,SongDisplaySettings sourceSettings)
    {
        mNowPlayingNode=selectedNode;

        String nextSong=null;
        if((selectedNode!=null)&&(selectedNode.mNextNode!=null)&&(shouldPlayNextSong()))
            nextSong=selectedNode.mNextNode.mSongFile.mTitle;

        LoadingSongFile lsf=new LoadingSongFile(selectedSong,trackName,scrollMode,nextSong,startedByBandLeader,startedByMidiTrigger,isDemoSong(selectedSong),nativeSettings,sourceSettings);
        startSong(lsf);
    }

    void startSong(LoadingSongFile lsf)
    {
        synchronized (mSongLoadSyncObject) {
            mSongLoadTask = new SongLoadTask(lsf, mSongListHandler);
        }
        mSongLoadTask.loadSong();
    }

    void clearTemporarySetList()
    {
        mTemporarySetListFilter=null;
        buildFilterList();
    }

    void addToTemporarySet(SongFile selectedSong)
    {
        if(mTemporarySetListFilter==null)
        {
            ArrayList<SongFile> songs=new ArrayList<>();
            songs.add(selectedSong);
            mTemporarySetListFilter = new TemporarySetListFilter(getString(R.string.temporary), songs);
        }
        else
            mTemporarySetListFilter.addSong(selectedSong);
        buildFilterList();
    }

    void onSongListLongClick(int position)
    {
        final PlaylistNode selectedNode=mPlaylist.getNodeAt(position);
        final SongFile selectedSong = selectedNode.mSongFile;
        final SetListFile selectedSet = (mSelectedFilter!=null && mSelectedFilter instanceof SetListFileFilter)?((SetListFileFilter)mSelectedFilter).mSetListFile:null;
        final ArrayList<String> trackNames=new ArrayList<>();
        trackNames.add(getString(R.string.no_audio));
        trackNames.addAll(selectedSong.mAudioFiles);
        boolean addAllowed=false;
        if(mTemporarySetListFilter!=null) {
            if (mSelectedFilter != mTemporarySetListFilter)
                if (!mTemporarySetListFilter.containsSong(selectedSong))
                    addAllowed = true;
        }
        else
            addAllowed=true;
        final boolean includeRefreshSet=selectedSet!=null;
        final boolean includeClearSet=selectedSet==null && mSelectedFilter==mTemporarySetListFilter;
        final Activity activity=this;

        int arrayID;
        if(includeRefreshSet) {
            if(addAllowed)
                arrayID=R.array.song_options_array_with_refresh_and_add;
            else
                arrayID=R.array.song_options_array_with_refresh;
        }
        else if(includeClearSet) {
            arrayID=R.array.song_options_array_with_clear;
        }
        else {
            if (addAllowed)
                arrayID = R.array.song_options_array_with_add;
            else
                arrayID = R.array.song_options_array;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.song_options)
                .setItems(arrayID, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1)
                            forceSongDownload(selectedSong, false);
                        else if (which == 2)
                            forceSongDownload(selectedSong, true);
                        else if (which == 3)
                        {
                            if(includeRefreshSet)
                                forceSetDownload(selectedSet);
                            else if(includeClearSet)
                                clearTemporarySetList();
                            else
                                addToTemporarySet(selectedSong);
                        }
                        else if(which==4)
                            addToTemporarySet(selectedSong);
                        else if (which == 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            // Get the layout inflater
                            LayoutInflater inflater = activity.getLayoutInflater();

                            View view = inflater.inflate(R.layout.songlist_long_press_dialog, null);

                            final Spinner audioSpinner = (Spinner) view
                                    .findViewById(R.id.audioSpinner);
                            ArrayAdapter<String> audioSpinnerAdapter = new ArrayAdapter<>(activity,
                                    android.R.layout.simple_spinner_item, trackNames);
                            audioSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            audioSpinner.setAdapter(audioSpinnerAdapter);
                            if (trackNames.size() > 1)
                                audioSpinner.setSelection(1);

                            boolean beatScrollable=selectedSong.isBeatScrollable();
                            boolean smoothScrollable=selectedSong.isSmoothScrollable();
                            final ToggleButton beatButton = (ToggleButton) view
                                    .findViewById(R.id.toggleButton_beat);
                            final ToggleButton smoothButton = (ToggleButton) view
                                    .findViewById(R.id.toggleButton_smooth);
                            final ToggleButton manualButton = (ToggleButton) view
                                    .findViewById(R.id.toggleButton_manual);
                            if(!smoothScrollable)
                            {
                                ViewGroup layout = (ViewGroup) smoothButton.getParent();
                                if(null!=layout)
                                    layout.removeView(smoothButton);
                            }
                            if(!beatScrollable)
                            {
                                ViewGroup layout = (ViewGroup) beatButton.getParent();
                                if(null!=layout)
                                    layout.removeView(beatButton);
                            }
                            if(beatScrollable) {
                                beatButton.setChecked(true);
                                beatButton.setEnabled(false);
                            }
                            else if(smoothScrollable) {
                                smoothButton.setChecked(true);
                                smoothButton.setEnabled(false);
                            }
                            else
                            {
                                manualButton.setChecked(true);
                                manualButton.setEnabled(false);
                            }

                            beatButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    if(isChecked)
                                    {
                                        smoothButton.setChecked(false);
                                        manualButton.setChecked(false);
                                        smoothButton.setEnabled(true);
                                        manualButton.setEnabled(true);
                                        beatButton.setEnabled(false);
                                    }
                                }
                            });

                            smoothButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    if(isChecked)
                                    {
                                        beatButton.setChecked(false);
                                        manualButton.setChecked(false);
                                        beatButton.setEnabled(true);
                                        manualButton.setEnabled(true);
                                        smoothButton.setEnabled(false);
                                    }
                                }
                            });

                            manualButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                    if(isChecked)
                                    {
                                        beatButton.setChecked(false);
                                        smoothButton.setChecked(false);
                                        smoothButton.setEnabled(true);
                                        beatButton.setEnabled(true);
                                        manualButton.setEnabled(false);
                                    }
                                }
                            });

                            // Inflate and set the layout for the dialog
                            // Pass null as the parent view because its going in the dialog layout
                            builder.setView(view)
                                    // Add action buttons
                                    .setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            // sign in the user ...
                                            String selectedTrack = (String) (audioSpinner.getSelectedItem());
                                            ScrollingMode mode=beatButton.isChecked()?ScrollingMode.Beat:(smoothButton.isChecked()?ScrollingMode.Smooth:ScrollingMode.Manual);
                                            if (audioSpinner.getSelectedItemPosition() == 0)
                                                selectedTrack = null;
                                            SongDisplaySettings sds=getSongDisplaySettings(mode);
                                            playSong(selectedNode, selectedSong, selectedTrack,mode,false,false,sds,sds);
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                            AlertDialog customAD = builder.create();
                            customAD.setCanceledOnTouchOutside(true);
                            customAD.show();
                        }
                    }
                });
        AlertDialog al = builder.create();
        al.setCanceledOnTouchOutside(true);
        al.show();
    }

    void onMIDIAliasListLongClick(int position)
    {
        final MIDIAliasCachedFile maf=mMIDIAliasCachedFiles.get(position);
        final boolean showErrors=maf.getErrors().size()>0;

        int arrayID=R.array.midi_alias_options_array;
        if(showErrors)
            arrayID=R.array.midi_alias_options_array_with_show_errors;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.midi_alias_list_options)
                .setItems(arrayID, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0)
                            forceMIDIAliasCachedFileDownload(maf);
                        else if (which == 1)
                            showMIDIAliasErrors(maf.getErrors());
                    }
                });
        AlertDialog al = builder.create();
        al.setCanceledOnTouchOutside(true);
        al.show();
    }

    void showMIDIAliasErrors(ArrayList<FileParseError> errors)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.parse_errors_dialog, null);
        builder.setView(view);
        TextView tv=(TextView)view.findViewById(R.id.errors);
        String str="";
        for(FileParseError fpe:errors)
            str+=fpe.getErrorMessage()+"\n";
        tv.setText(str.trim());
        AlertDialog customAD = builder.create();
        customAD.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        customAD.setTitle(getString(R.string.midi_alias_file_errors));
        customAD.setCanceledOnTouchOutside(true);
        customAD.show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        if((mSelectedFilter!=null)&&(mSelectedFilter instanceof MIDIAliasFilesFilter))
            onMIDIAliasListLongClick(position);
        else
            onSongListLongClick(position);
        return true;
    }

    private void forceSongDownload(SongFile selectedSong,boolean dependenciesToo)
    {
        mSongToRefresh=selectedSong;
        mFetchDependenciesToo=dependenciesToo;
        Cloud cloud=getCloud();
        if(cloud==Cloud.GoogleDrive)
            fetchFilesFromGoogleDrive();
        else if(cloud==Cloud.OneDrive)
            fetchFilesFromOneDrive();
        else if(cloud==Cloud.Dropbox)
            fetchFilesFromDropbox();
    }

    private void forceMIDIAliasCachedFileDownload(MIDIAliasCachedFile selectedFile)
    {
        mMIDIAliasCachedFileToRefresh=selectedFile;
        Cloud cloud=getCloud();
        if(cloud==Cloud.GoogleDrive)
            fetchFilesFromGoogleDrive();
        else if(cloud==Cloud.OneDrive)
            fetchFilesFromOneDrive();
        else if(cloud==Cloud.Dropbox)
            fetchFilesFromDropbox();
    }

    private void forceSetDownload(SetListFile selectedSet)
    {
        mSetToRefresh=selectedSet;
        Cloud cloud=getCloud();
        if(cloud==Cloud.GoogleDrive)
            fetchFilesFromGoogleDrive();
        else if(cloud==Cloud.OneDrive)
            fetchFilesFromOneDrive();
        else if(cloud==Cloud.Dropbox)
            fetchFilesFromDropbox();
    }

    enum SortingPreference
    {
        TITLE,
        ARTIST,
        KEY,
        DATE
    }

    public static File mBeatPrompterDataFolder;
    public static File mBeatPrompterSongFilesFolder;
    public static final String TAG = "beatprompter";
    public static final String MIDI_TAG = "midi";
    public static final String AUTOLOAD_TAG = "autoload";

    private static File mGoogleDriveFolder;
    private static File mDropboxFolder;
    private static File mOneDriveFolder;
    private static File mDemoFolder;

    private static final String XML_DATABASE_FILE_NAME="bpdb.xml";
    private static final String XML_DATABASE_FILE_ROOT_ELEMENT_TAG="beatprompterDatabase";

    private static final String DROPBOX_CACHE_FOLDER_NAME="dropbox";
    private static final String ONEDRIVE_CACHE_FOLDER_NAME="onedrive";
    private static final String GOOGLE_DRIVE_CACHE_FOLDER_NAME="google_drive";
    private static final String DEMO_CACHE_FOLDER_NAME="demo";

    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final int COMPLETE_AUTHORIZATION_REQUEST_CODE=2;
    private static final int PLAY_SONG_REQUEST_CODE=3;
    private static final int GOOGLE_PLAY_TRANSACTION_FINISHED=4;
//    private static final int REQUEST_CODE_GOOGLE_DRIVE_FILE_SELECTED = 2;

    private final static String[] AUDIO_FILE_EXTENSIONS=new String[]{"mp3","wav","m4a","wma","ogg","aac"};
    private final static String[] IMAGE_FILE_EXTENSIONS=new String[]{"jpg","png","jpeg","bmp","tif","tiff"};

    SharedPreferences.OnSharedPreferenceChangeListener mStorageLocationPrefListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    if((key.equals(getString(R.string.pref_storageLocation_key)))||(key.equals(getString(R.string.pref_useExternalStorage_key))))
                        setBeatPrompterFolder();
                    else if(key.equals(getString(R.string.pref_largePrintList_key)))
                        buildList();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        mSongListInstance=this;
        parseDefaultAliasFile();

        ((BeatPrompterApplication)this.getApplicationContext()).setSongListHandler(mSongListHandler);

        mMidiInTaskThread.start();
        Task.resumeTask(mMidiInTask);
        mMidiUsbOutTaskThread.start();
        Task.resumeTask(mMidiUsbOutTask);

        mSongLoaderTask=new SongLoaderTask(this);
        mSongLoaderTaskThread=new Thread(mSongLoaderTask);
        mSongLoaderTaskThread.start();
        Task.resumeTask(mSongLoaderTask);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mStorageLocationPrefListener);
        //SharedPreferences sharedPrefs =getPreferences(Context.MODE_PRIVATE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addApi(Plus.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        mMidiUsbRegistered = true;

        attemptUsbMidiConnection();

        // Set font stuff first.
        DisplayMetrics metrics=getResources().getDisplayMetrics();
        Utils.FONT_SCALING=metrics.density;
        Utils.MAXIMUM_FONT_SIZE=Integer.parseInt(getString(R.string.fontSizeMax));
        Utils.MINIMUM_FONT_SIZE=Integer.parseInt(getString(R.string.fontSizeMin));
        FontSizePreference.FONT_SIZE_MAX=Utils.MAXIMUM_FONT_SIZE-Utils.MINIMUM_FONT_SIZE;
        FontSizePreference.FONT_SIZE_MIN=0;
        FontSizePreference.FONT_SIZE_OFFSET=Utils.MINIMUM_FONT_SIZE;

        setBeatPrompterFolder();

        setContentView(R.layout.activity_song_list);

        if(isFirstRun()) {
            showFirstRunMessages();
            createDemoFile();
        }

        initialiseList();

        ((BeatPrompterApplication)this.getApplicationContext()).startBluetooth();
    }

    void initializeOneDriveAPI()
    {
        if(mOneDriveClient==null)
        {
            final ICallback<IOneDriveClient> callback = new ICallback<IOneDriveClient>() {
                @Override
                public void success(final IOneDriveClient result) {
                    Log.v(TAG, "Signed in to OneDrive");
                    mOneDriveClient=result;
                    doOneDriveSync();
                }

                @Override
                public void failure(final ClientException error) {
                    mOneDriveClient=null;
                    Log.e(TAG, "Nae luck signing in to OneDrive");
                }
            };

            IClientConfig oneDriveConfig = DefaultClientConfig.
                    createWithAuthenticator(ONEDRIVE_MSA_AUTHENTICATOR);
            new OneDriveClient.Builder()
                    .fromConfig(oneDriveConfig)
                    .loginAndBuildClient(SongList.this,callback);
        }

    }

    void initializeDropboxAPI()
    {
        if(mDropboxAPI==null) {
            SharedPreferences sharedPrefs = getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID,Context.MODE_PRIVATE);
            String storedAccessToken = sharedPrefs.getString(getString(R.string.pref_dropboxAccessToken_key), null);
            if (storedAccessToken != null) {
                DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(BeatPrompterApplication.APP_NAME)
                        .build();
                mDropboxAPI = new DbxClientV2(requestConfig, storedAccessToken);
                doDropBoxSync();
            }
        }
    }

    void initialiseList()
    {
        try {
            mSortingPreference=getSortingPreference();
            readDatabase();
            buildFilterList();
            sortSongList();
            buildList();
        }
        catch(Exception e)
        {
            Log.e(TAG,e.getMessage());
        }
    }

    void attemptUsbMidiConnection()
    {
        HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if ((list != null) && (list.size() > 0)) {
            Object[] devObjs = list.values().toArray();
            for (Object devObj : devObjs) {
                UsbDevice dev = (UsbDevice) devObj;
                if (getDeviceMidiInterface(dev) != null) {
                    mUsbManager.requestPermission(dev, mPermissionIntent);
                    break;
                }
            }
        }
    }

    void setBeatPrompterFolder()
    {
        File previousSongFilesFolder=mBeatPrompterSongFilesFolder;

        String s = getPackageName();
        try {
            PackageManager m=getPackageManager();
            PackageInfo p = m.getPackageInfo(s, 0);
            mBeatPrompterDataFolder = new File(p.applicationInfo.dataDir);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found ", e);
        }

        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        String songFilesFolder;
        boolean useExternalStorage=sharedPrefs.getBoolean(getString(R.string.pref_useExternalStorage_key), false);
        File externalFilesDir=getExternalFilesDir(null);
        if(useExternalStorage && externalFilesDir!=null)
            songFilesFolder=externalFilesDir.getAbsolutePath();
        else
            songFilesFolder=mBeatPrompterDataFolder.getAbsolutePath();

        mBeatPrompterSongFilesFolder=(songFilesFolder.length()==0?mBeatPrompterDataFolder:new File(songFilesFolder));
        if(!mBeatPrompterSongFilesFolder.exists())
            if(!mBeatPrompterSongFilesFolder.mkdir())
                Log.e(TAG,"Failed to create song files folder.");

        if(!mBeatPrompterSongFilesFolder.exists())
            mBeatPrompterSongFilesFolder=mBeatPrompterDataFolder;

        if(previousSongFilesFolder!=null)
            if(!previousSongFilesFolder.equals(mBeatPrompterSongFilesFolder))
                // Song file storage folder has changed. We need to clear the cache.
                deleteAllFiles();

        mGoogleDriveFolder=new File(mBeatPrompterSongFilesFolder,GOOGLE_DRIVE_CACHE_FOLDER_NAME);
        mDropboxFolder=new File(mBeatPrompterSongFilesFolder,DROPBOX_CACHE_FOLDER_NAME);
        mOneDriveFolder=new File(mBeatPrompterSongFilesFolder,ONEDRIVE_CACHE_FOLDER_NAME);
        mDemoFolder=new File(mBeatPrompterSongFilesFolder,DEMO_CACHE_FOLDER_NAME);
        if(!mGoogleDriveFolder.exists())
            if(!mGoogleDriveFolder.mkdir())
                Log.e(TAG,"Failed to create Google Drive sync folder.");
        if(!mOneDriveFolder.exists())
            if(!mOneDriveFolder.mkdir())
                Log.e(TAG,"Failed to create OneDrive sync folder.");
        if(!mDropboxFolder.exists())
            if(!mDropboxFolder.mkdir())
                Log.e(TAG,"Failed to create Dropbox sync folder.");
        if(!mDemoFolder.exists())
            if(!mDemoFolder.mkdir())
                Log.e(TAG,"Failed to create Demo folder.");
    }

    IInAppBillingService mIAPService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIAPService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mIAPService = IInAppBillingService.Stub.asInterface(service);
            fullVersionUnlocked();
        }
    };

    @Override
    public void onDestroy()
    {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mStorageLocationPrefListener);
        super.onDestroy();

        Task.stopTask(mMidiInTask,mMidiInTaskThread);
        Task.stopTask(mMidiSongDisplayInTask,mMidiSongDisplayInTaskThread);
        Task.stopTask(mMidiUsbInTask,mMidiUsbInTaskThread);
        Task.stopTask(mMidiUsbOutTask,mMidiUsbOutTaskThread);
        Task.stopTask(mSongLoaderTask,mSongLoaderTaskThread);

        if (mServiceConn != null)
            unbindService(mServiceConn);
        if(mMidiUsbRegistered)
            unregisterReceiver(mUsbReceiver);
        // Unregister broadcast listeners
    }

    public static AudioFile getMappedAudioFilename(String in,ArrayList<AudioFile> tempAudioFileCollection)
    {
        if(in!=null) {
            for (AudioFile afm : mAudioFiles) {
                String secondChance = in.replace('’', '\'');
                if ((afm.mTitle.equalsIgnoreCase(in)) || (afm.mTitle.equalsIgnoreCase(secondChance)))
                    return afm;
            }
            if(tempAudioFileCollection!=null)
                for (AudioFile afm : tempAudioFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((afm.mTitle.equalsIgnoreCase(in)) || (afm.mTitle.equalsIgnoreCase(secondChance)))
                        return afm;
                }
        }
        return null;
    }

    public static ImageFile getMappedImageFilename(String in,ArrayList<ImageFile> tempImageFileCollection)
    {
        if(in!=null) {
            for (ImageFile ifm : mImageFiles) {
                String secondChance = in.replace('’', '\'');
                if ((ifm.mTitle.equalsIgnoreCase(in)) || (ifm.mTitle.equalsIgnoreCase(secondChance)))
                    return ifm;
            }
            if(tempImageFileCollection!=null)
                for (ImageFile ifm : tempImageFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((ifm.mTitle.equalsIgnoreCase(in)) || (ifm.mTitle.equalsIgnoreCase(secondChance)))
                        return ifm;
                }
        }
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_song_list);
        buildList();
    }

    @Override
    protected void onResume()
    {
        mSongListActive=true;
        super.onResume();
        Task.resumeTask(mMidiSongDisplayInTask);

        updateBluetoothIcon();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            if(mSongEndedNaturally)
                if (startNextSong())
                    return;

        if(mListAdapter!=null)
            mListAdapter.notifyDataSetChanged();

        if(mSongToLoadOnResume!=null)
        {
            try
            {
                startSong(mSongToLoadOnResume);
            }
            finally
            {
                mSongToLoadOnResume=null;
            }
        }
    }

    @Override
    protected void onPause() {
        mSongListActive=false;
        Task.pauseTask(mMidiSongDisplayInTask,mMidiSongDisplayInTaskThread);
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            Log.i(TAG, "GoogleApiClient starting connection resolution ...");
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case COMPLETE_AUTHORIZATION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    if((mSongToRefresh==null)&&(mSetToRefresh==null)&&(mMIDIAliasCachedFileToRefresh==null))
                        new GoogleDriveDownloadTask(mSongListHandler).execute();
                    else if(mSongToRefresh!=null)
                        forceSongDownload();
                    else if(mSetToRefresh!=null)
                        forceSetDownload();
                    else
                        forceMIDIAliasCachedFileDownload();
                } else {
                    // User denied access, show him the account chooser again
                    Log.d(TAG,"User was denied access.");
                }
                break;
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Resolved! Attempting connection again ...");
                    mGoogleApiClient.connect();
                } else
                    Log.d(TAG, "Resolution failed: result code = " + resultCode);
                break;
            case GOOGLE_PLAY_TRANSACTION_FINISHED:
                //int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                //String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

                if (resultCode == RESULT_OK) {
                    try {
                        JSONObject jo = new JSONObject(purchaseData);
                        String sku = jo.getString("productId");
                        mFullVersionUnlocked|=(sku.equalsIgnoreCase(FULL_VERSION_SKU_NAME));
                        Toast.makeText(SongList.this,getString(R.string.thankyou),Toast.LENGTH_LONG).show();
                    }
                    catch (JSONException e)
                    {
                        Log.e(TAG,"JSON exception during purchase.");
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case PLAY_SONG_REQUEST_CODE:
                if(resultCode==RESULT_OK)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                       startNextSong();
                break;
        }
    }

    private boolean startNextSong()
    {
        mSongEndedNaturally=false;
        if((mNowPlayingNode!=null)&&(mNowPlayingNode.mNextNode!=null)&&(shouldPlayNextSong())) {
            playPlaylistNode(mNowPlayingNode.mNextNode,false);
            return true;
        }
        mNowPlayingNode=null;
        return false;
    }

    private static final String[] SCOPES = { DriveScopes.DRIVE_READONLY, DriveScopes.DRIVE_METADATA };

    private com.google.api.services.drive.Drive getGoogleDriveService()
    {
        if(mGoogleDriveService==null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setSelectedAccountName(mDriveAccountName)
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

    private void forceSongDownload()
    {
        Cloud cloud=getCloud();
        if(cloud==Cloud.None)
            return;
        if(cloud==Cloud.GoogleDrive)
            getGoogleDriveService();
        SongFile song=mSongToRefresh;
        mSongToRefresh=null;
        ArrayList<String> filesToRefresh=new ArrayList<>();
        filesToRefresh.add(song.mStorageName);
        String subfolderOrigin=song.mSubfolder;
        if(mFetchDependenciesToo) {
            if (song.mAudioFiles != null)
                for (String audioFileName : song.mAudioFiles) {
                    AudioFile audioFile = getMappedAudioFilename(audioFileName, null);
                    File actualAudioFile = null;
                    if (audioFile != null)
                        actualAudioFile = new File(song.mFile.getParent(), audioFile.mFile.getName());
                    if ((actualAudioFile != null) && (actualAudioFile.exists()))
                        filesToRefresh.add(audioFile.mStorageName);
                }
            if (song.mImageFiles != null)
                for (String imageFileName : song.mImageFiles) {
                    ImageFile imageFile = getMappedImageFilename(imageFileName, null);
                    File actualImageFile = null;
                    if (imageFile != null)
                        actualImageFile = new File(song.mFile.getParent(), imageFile.mFile.getName());
                    if ((actualImageFile != null) && (actualImageFile.exists()))
                        filesToRefresh.add(imageFile.mStorageName);
                }
        }
        if(cloud==Cloud.Dropbox)
            new DropboxFileDownloadTask(CachedFileType.Song,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.GoogleDrive)
            new GoogleDriveFileDownloadTask(CachedFileType.Song,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.OneDrive)
            new OneDriveFileDownloadTask(CachedFileType.Song,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
    }

    private void forceSetDownload()
    {
        Cloud cloud=getCloud();
        if(cloud==Cloud.None)
            return;
        if(cloud==Cloud.GoogleDrive)
            getGoogleDriveService();
        SetListFile set=mSetToRefresh;
        mSetToRefresh=null;
        ArrayList<String> filesToRefresh=new ArrayList<>();
        filesToRefresh.add(set.mStorageName);
        String subfolderOrigin=set.mSubfolder;
        if(cloud==Cloud.Dropbox)
            new DropboxFileDownloadTask(CachedFileType.SetList,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.GoogleDrive)
            new GoogleDriveFileDownloadTask(CachedFileType.SetList,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.OneDrive)
            new OneDriveFileDownloadTask(CachedFileType.SetList,subfolderOrigin,mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
    }

    private void forceMIDIAliasCachedFileDownload()
    {
        Cloud cloud=getCloud();
        if(cloud==Cloud.None)
            return;
        if(cloud==Cloud.GoogleDrive)
            getGoogleDriveService();
        MIDIAliasCachedFile MIDIAliasCachedFile=mMIDIAliasCachedFileToRefresh;
        mMIDIAliasCachedFileToRefresh=null;
        ArrayList<String> filesToRefresh=new ArrayList<>();
        String subfolderOrigin=MIDIAliasCachedFile.mSubfolder;
        filesToRefresh.add(MIDIAliasCachedFile.mStorageName);
        if(cloud==Cloud.Dropbox)
            new DropboxFileDownloadTask(CachedFileType.MIDIAliases,subfolderOrigin, mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.GoogleDrive)
            new GoogleDriveFileDownloadTask(CachedFileType.MIDIAliases,subfolderOrigin, mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
        else if(cloud==Cloud.OneDrive)
            new OneDriveFileDownloadTask(CachedFileType.MIDIAliases,subfolderOrigin, mSongListHandler).execute(filesToRefresh.toArray(new String[filesToRefresh.size()]));
    }

    private InputStream getDriveFileInputStream(com.google.api.services.drive.model.File file) {
        try {
            boolean isGoogleDoc = file.getMimeType().startsWith("application/vnd.google-apps.");
            if (isGoogleDoc) {
                boolean isGoogleTextDoc = file.getMimeType().equals("application/vnd.google-apps.document");
                if(isGoogleTextDoc)
                    return getGoogleDriveService().files().export(file.getId(), "text/plain").executeMediaAsInputStream();
                // Ignore spreadsheets, drawings, etc.
            } else
                // Binary files.
                return getGoogleDriveService().files().get(file.getId()).executeMediaAsInputStream();
        }
        catch(IOException ioe)
        {
            // An error occurred.
            ioe.printStackTrace();
        }
        return null;
    }

    private void streamToStream(InputStream is,OutputStream os) throws IOException
    {
        byte[] buffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1)
            os.write(buffer, 0, bytesRead);
    }

    private File downloadDropboxFile(FileMetadata file, String filename) throws IOException, DbxException
    {
        File localfile = new File(mDropboxFolder, filename);
        FileOutputStream fos =null;
        try {
            fos = new FileOutputStream(localfile);
            DbxDownloader<FileMetadata> downloader=mDropboxAPI.files().download(file.getId());
            downloader.download(fos);
        }
        finally
        {
            if(fos!=null)
                try {
                    fos.close();
                }
                catch(Exception eee)
                {
                    Log.e(TAG,"Failed to close file",eee);
                }
        }
        return localfile;
    }

    private File downloadOneDriveFile(Item file, String filename) throws IOException, DbxException
    {
        File localfile = new File(mDropboxFolder, filename);
        FileOutputStream fos =null;
        InputStream inputStream=null;
        try {
            fos = new FileOutputStream(localfile);
            inputStream=mOneDriveClient.getDrive().getItems(file.id).getContent().buildRequest().get();
            streamToStream(inputStream,fos);
        }
        finally
        {
            if(fos!=null)
                try {
                    fos.close();
                }
                catch(Exception eee)
                {
                    Log.e(TAG,"Failed to close file",eee);
                }
            if(inputStream!=null)
                try {
                    inputStream.close();
                }
                catch(Exception eee)
                {
                    Log.e(TAG,"Failed to close input stream.",eee);
                }
        }
        return localfile;
    }

    private File downloadGoogleDriveFile(com.google.api.services.drive.model.File file, String filename) throws IOException
    {
        File localfile = new File(mGoogleDriveFolder, filename);
        InputStream inputStream=getDriveFileInputStream(file);
        FileOutputStream fos =null;
        if(inputStream!=null){
            try {
                Log.d(TAG, "Creating new local file, " + localfile.getAbsolutePath());
                fos=new FileOutputStream(localfile);
                streamToStream(inputStream,fos);
            }
            finally
            {
                if(fos!=null)
                    try {
                        fos.close();
                    }
                    catch(Exception eee)
                    {
                        Log.e(TAG,"Failed to close file output stream.",eee);
                    }
                try {
                    inputStream.close();
                }
                catch(Exception eee)
                {
                    Log.e(TAG,"Failed to close input stream.",eee);
                }
            }
        }
        return localfile;
    }

    private static final String ReservedChars = "|\\?*<\":>+[]/'";
    private String makeSafeFilename(String str)
    {
        String strOut="";
        for(char c:str.toCharArray())
        {
            if(ReservedChars.contains(""+c))
                strOut+="_";
            else
                strOut+=c;
        }
        return strOut;
    }

    abstract class CloudDownloadTask extends AsyncTask<String, String, Boolean>
    {
        CloudDownloadTask(Handler handler)
        {
            mHandler=handler;
        }
        Handler mHandler;
        ProgressDialog mProgressDialog;
        ArrayList<SongFile> mDownloadedSongs=new ArrayList<>();
        ArrayList<MIDIAliasCachedFile> mDownloadedMIDIAliasCachedFiles=new ArrayList<>();
        ArrayList<SetListFile> mDownloadedSets=new ArrayList<>();
        ArrayList<AudioFile> mDownloadedAudioFiles=new ArrayList<>();
        ArrayList<ImageFile> mDownloadedImageFiles=new ArrayList<>();
        Exception mCloudSyncException=null;

        @Override
        protected Boolean doInBackground(String... paramParams) {
            String folderIDString=getCloudPath();
            boolean includeSubfolders=getIncludeSubfolders();

            Map<String,File> localCacheContentsByStorageID =new HashMap<>();
            ArrayList<CachedFile> cachedFiles=new ArrayList<>();
            cachedFiles.addAll(mSongs);
            cachedFiles.addAll(mMIDIAliasCachedFiles);
            cachedFiles.addAll(mSets);
            cachedFiles.addAll(mAudioFiles);
            cachedFiles.addAll(mImageFiles);
            for(CachedFile cf: cachedFiles) {
                Log.d(TAG, "Existing file: " + cf.mStorageName+"="+cf.mFile.getAbsolutePath());
                localCacheContentsByStorageID.put(cf.mStorageName, cf.mFile);
            }

            ArrayList<DownloadedFile> downloadedFilesToParse=new ArrayList<>();
            try
            {
                downloadFiles(folderIDString,includeSubfolders,localCacheContentsByStorageID,downloadedFilesToParse);
            }
            catch(Exception e)
            {
                mCloudSyncException=e;
            }

            // If we didn't manage to download ANYTHING, then DON'T blast everything away.
            // Also check for whether there was nothing new to download, but maybe some files were removed?
            for (DownloadedFile downloadedFile : downloadedFilesToParse) {
                try {
                    MIDIAliasCachedFile maf = new MIDIAliasCachedFile(SongList.this, downloadedFile, mDefaultAliases);
                    mDownloadedMIDIAliasCachedFiles.add(maf);
                } catch (InvalidBeatPrompterFileException ibpfe) {
                    // Not a MIDI alias file. Might be a song file?
                    try {
                        SongFile song = new SongFile(SongList.this, downloadedFile, mDownloadedAudioFiles, mDownloadedImageFiles);
                        mDownloadedSongs.add(song);
                    } catch (InvalidBeatPrompterFileException ibpfe2) {
                        // Not a song file ... might be a set file?
                        try {
                            SetListFile slf = new SetListFile(mContext, downloadedFile);
                            mDownloadedSets.add(slf);
                        } catch (InvalidBeatPrompterFileException ibpfe3) {
                            Log.e(TAG, ibpfe2.getMessage());
                        }
                    } catch (IOException ioe) {
                        // Failed to read the file
                        Log.e(TAG, ioe.getMessage());
                    }
                }
            }
            // Don't clear out the "leftovers" if there was a sync error ... we simply might not have reached those files yet before
            // the sync failed.
            if(mCloudSyncException==null) {
                // Any entries left in the map did not exist on the Drive. So delete them
                for (File fileToDelete : localCacheContentsByStorageID.values()) {
                    Log.d(TAG, "Deleting local file that doesn't match anything on cloud: " + fileToDelete.getAbsolutePath());
                    this.publishProgress(String.format(SongList.this.getString(R.string.deleting), fileToDelete.getName()));
                    if (!fileToDelete.delete())
                        Log.e(TAG, "Failed to delete file.");
                }
            }
            else {
                // Better stick all the files from the previous cache back into the new cache.
                // To do this, we need to treat them as "downloaded".
                // Watch out in case we DID download a new version of them. Don't add them twice.
                for(CachedFile macf: mMIDIAliasCachedFiles) {
                    boolean found=false;
                    for (CachedFile macf2 : mDownloadedMIDIAliasCachedFiles) {
                        if (macf2.mStorageName.equals(macf.mStorageName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        mDownloadedMIDIAliasCachedFiles.add((MIDIAliasCachedFile)macf);
                }
                for(CachedFile macf: mSets) {
                    boolean found=false;
                    for (CachedFile macf2 : mDownloadedSets) {
                        if (macf2.mStorageName.equals(macf.mStorageName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        mDownloadedSets.add((SetListFile)macf);
                }
                for(CachedFile macf: mSongs) {
                    boolean found=false;
                    for (CachedFile macf2 : mDownloadedSongs) {
                        if (macf2.mStorageName.equals(macf.mStorageName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        mDownloadedSongs.add((SongFile)macf);
                }
                for(CachedFile macf: mAudioFiles) {
                    boolean found=false;
                    for (CachedFile macf2 : mDownloadedAudioFiles) {
                        if (macf2.mStorageName.equals(macf.mStorageName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        mDownloadedAudioFiles.add((AudioFile)macf);
                }
                for(CachedFile macf: mImageFiles) {
                    boolean found=false;
                    for (CachedFile macf2 : mDownloadedImageFiles) {
                        if (macf2.mStorageName.equals(macf.mStorageName)) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        mDownloadedImageFiles.add((ImageFile)macf);
                }

                mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR, mCloudSyncException.getMessage()).sendToTarget();
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (mProgressDialog!=null) {
                mProgressDialog.dismiss();}
            mSongs=mDownloadedSongs;
            mSets=mDownloadedSets;
            mMIDIAliasCachedFiles=mDownloadedMIDIAliasCachedFiles;
            mAudioFiles=mDownloadedAudioFiles;
            mImageFiles=mDownloadedImageFiles;
            try
            {
                writeDatabase();
                buildFilterList();
            }
            catch(Exception ioe)
            {
                Log.e(TAG,ioe.getMessage());
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle(SongList.this.getString(R.string.downloadingFiles));
            mProgressDialog.setMessage(String.format(SongList.this.getString(R.string.accessingCloudStorage),getCloudStorageName()));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.show();
        }

        void updateDownloadProgress(String filename)
        {
            publishProgress(String.format(SongList.this.getString(R.string.downloading),filename));
        }

        abstract String getCloudStorageName();

        abstract void downloadFiles(String folderID,boolean includeSubfolders,Map<String,File> existingCachedFiles,ArrayList<DownloadedFile> downloadedFiles) throws IOException;
    }

    private class GoogleDriveDownloadTask extends CloudDownloadTask
    {
        GoogleDriveDownloadTask(Handler handler)
        {
            super(handler);
        }
        void downloadFiles(String folderID,boolean includeSubfolders,Map<String,File> existingCachedFiles,ArrayList<DownloadedFile> downloadedFiles) throws IOException
        {
            List<String> foldersToQuery=new ArrayList<>();
            foldersToQuery.add(folderID);
            List<String> folderNames=new ArrayList<>();
            folderNames.add("");

            while(!foldersToQuery.isEmpty())
            {
                String currentFolderID=foldersToQuery.remove(0);
                String currentFolderName=folderNames.remove(0);
                String queryString = "trashed=false and '" + currentFolderID + "' in parents";
                if (!includeSubfolders)
                    queryString += " and mimeType != '" + GOOGLE_DRIVE_FOLDER_MIMETYPE + "'";
                com.google.api.services.drive.Drive.Files.List request = getGoogleDriveService().files().list().setQ(queryString).setFields("nextPageToken,files(id,name,mimeType,modifiedTime)");
                do {
                    try {
                        FileList children = request.execute();

                        Log.d(TAG, "Iterating through contents, seeing what needs updated/downloaded/deleted ...");

                        for (com.google.api.services.drive.model.File child : children.getFiles()) {
                            String fileID = child.getId();
                            Log.d(TAG, "File ID: " + fileID);
                            if (includeSubfolders) {
                                String mimeType = child.getMimeType();
                                if (GOOGLE_DRIVE_FOLDER_MIMETYPE.equals(mimeType)) {
                                    Log.d(TAG, "Adding folder to list of folders to query ...");
                                    foldersToQuery.add(fileID);
                                    folderNames.add(child.getName());
                                    continue;
                                }
                            }

                            String title = child.getName();
                            Log.d(TAG, "File title: " + title);
                            String lowerCaseTitle = title.toLowerCase();
                            boolean audioFile = false;
                            boolean imageFile = false;
                            for (String ext : AUDIO_FILE_EXTENSIONS)
                                if (lowerCaseTitle.endsWith(ext))
                                    audioFile = true;
                            for (String ext : IMAGE_FILE_EXTENSIONS)
                                if (lowerCaseTitle.endsWith(ext))
                                    imageFile = true;
                            this.publishProgress(String.format(SongList.this.getString(R.string.checking), title));
                            String safeFilename = makeSafeFilename(fileID);
                            Log.d(TAG, "Safe filename: " + safeFilename);
                            File existingLocalFile = existingCachedFiles.get(fileID);
                            boolean downloadRequired = true;
                            Date lastModified = new Date(child.getModifiedTime().getValue());
                            if (existingLocalFile != null) {
                                Date localFileModified = new Date(existingLocalFile.lastModified());
                                Log.d(TAG, "Drive File was last modified " + lastModified);
                                Log.d(TAG, "Local File was last downloaded " + localFileModified);
                                if (localFileModified.after(lastModified)) {
                                    Log.d(TAG, "It hasn't changed since last download ... ignoring!");
                                    downloadRequired = false;
                                    existingCachedFiles.remove(fileID);
                                } else
                                    Log.d(TAG, "Looks like it has changed since last download ... re-downloading!");
                            } else
                                Log.d(TAG, "Appears to be a file that I don't have yet... downloading!");

                            if (downloadRequired) {
                                Log.d(TAG, "Downloading now ...");
                                updateDownloadProgress(title);
                                existingLocalFile = downloadGoogleDriveFile(child, safeFilename);
                                existingCachedFiles.remove(fileID);
                            }

                            if (audioFile)
                                mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                            else if (imageFile)
                                mDownloadedImageFiles.add(new ImageFile(title, existingLocalFile, fileID, lastModified));
                            else
                                downloadedFiles.add(new DownloadedFile(existingLocalFile,fileID,lastModified,currentFolderName));
                        }
                        request.setPageToken(children.getNextPageToken());
                    } catch (UserRecoverableAuthIOException uraioe) {
                        startActivityForResult(uraioe.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
                    }
                } while (request.getPageToken() != null &&
                        request.getPageToken().length() > 0);
            }
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.google_drive_string);
        }
    }

    private class DropboxDownloadTask extends CloudDownloadTask
    {
        DropboxDownloadTask(Handler handler)
        {
            super(handler);
        }
        void downloadFiles(String folderID,boolean includeSubfolders,Map<String,File> existingCachedFiles,ArrayList<DownloadedFile> downloadedFiles) throws IOException
        {
            List<String> folderIDs=new ArrayList<>();
            folderIDs.add(folderID);
            List<String> folderNames=new ArrayList<>();
            folderNames.add("");

            while(!folderIDs.isEmpty())
            {
                String currentFolderID=folderIDs.remove(0);
                String currentFolderName=folderNames.remove(0);
                try
                {
                    Log.d(TAG, "Getting list of everything in Dropbox folder.");
                    String[] extsToLookFor = new String[]{".txt", ".mp3", ".wav", ".m4a", ".aac", ".ogg"};
                    List<FileMetadata> results = new ArrayList<>();
                    ListFolderResult listResult = mDropboxAPI.files().listFolder(currentFolderID);
                    while(listResult!=null)
                    {
                        List<Metadata> entries=listResult.getEntries();
                        for(Metadata mdata:entries)
                        {
                            if(mdata instanceof FileMetadata)
                            {
                                FileMetadata fmdata=(FileMetadata)mdata;
                                String filename=fmdata.getName().toLowerCase();
                                boolean isSuitableFile=false;
                                for(String ext:extsToLookFor)
                                    if(filename.endsWith(ext))
                                    {
                                        isSuitableFile = true;
                                        break;
                                    }
                                if(isSuitableFile)
                                    results.add(fmdata);
                            }
                            else if((mdata instanceof FolderMetadata) && (includeSubfolders))
                            {
                                Log.d(TAG, "Adding folder to list of folders to query ...");
                                folderIDs.add(((FolderMetadata) mdata).getPathLower());
                                folderNames.add(mdata.getName());
                            }
                        }
                        if(listResult.getHasMore())
                            listResult=mDropboxAPI.files().listFolderContinue(listResult.getCursor());
                        else
                            listResult=null;
                    }

                    for (FileMetadata entry : results) {
                        //if (!entry.isDeleted)
                        {
                            String fileID = entry.getId();
                            Log.d(TAG, "File ID: " + fileID);
                            String title = entry.getName();
                            String lowerCaseTitle = title.toLowerCase();
                            boolean audioFile = false;
                            for (String ext : AUDIO_FILE_EXTENSIONS)
                                if (lowerCaseTitle.endsWith(ext))
                                    audioFile = true;
                            this.publishProgress(String.format(SongList.this.getString(R.string.checking), title));
                            String safeFilename = makeSafeFilename(title);
                            Log.d(TAG, "Safe filename: " + safeFilename);
                            File existingLocalFile = existingCachedFiles.get(fileID);
                            boolean downloadRequired = true;
                            Date lastModified = entry.getServerModified();
                            if (existingLocalFile != null) {
                                Date localFileModified = new Date(existingLocalFile.lastModified());
                                Log.d(TAG, "Dropbox File was last modified " + lastModified);
                                Log.d(TAG, "Local File was last downloaded " + localFileModified);
                                if (localFileModified.after(lastModified)) {
                                    Log.d(TAG, "It hasn't changed since last download ... ignoring!");
                                    downloadRequired = false;
                                    existingCachedFiles.remove(fileID);
                                } else
                                    Log.d(TAG, "Looks like it has changed since last download ... re-downloading!");
                            } else
                                Log.d(TAG, "Appears to be a file that I don't have yet... downloading!");

                            if (downloadRequired) {
                                Log.d(TAG, "Downloading now ...");
                                this.publishProgress(String.format(SongList.this.getString(R.string.downloading), title));
                                existingLocalFile = downloadDropboxFile(entry, safeFilename);
                                existingCachedFiles.remove(fileID);
                            }

                            if (!audioFile)
                                downloadedFiles.add(new DownloadedFile(existingLocalFile,fileID,lastModified,currentFolderName));
                            else
                                mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                        }
                    }
                }
                catch(DbxException de)
                {
                    throw new IOException(de.getMessage(),de);
                }
            }
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.dropbox_string);
        }
    }

    private class OneDriveDownloadTask extends CloudDownloadTask
    {
        OneDriveDownloadTask(Handler handler)
        {
            super(handler);
        }
        void downloadFiles(String folderID,boolean includeSubfolders,Map<String,File> existingCachedFiles,ArrayList<DownloadedFile> downloadedFiles) throws IOException
        {
            List<String> folderIDs=new ArrayList<>();
            folderIDs.add(folderID);
            List<String> folderNames=new ArrayList<>();
            folderNames.add("");

            while(!folderIDs.isEmpty())
            {
                String currentFolderID = folderIDs.remove(0);
                String currentFolderName=folderNames.remove(0);
                try {
                    Log.d(TAG, "Getting list of everything in OneDrive folder.");
                    IItemCollectionPage page = mOneDriveClient.getDrive().getItems(currentFolderID).getChildren().buildRequest().get();
                    List<Item> items = new ArrayList<>();
                    while (page != null) {
                        List<Item> children = page.getCurrentPage();
                        for (Item child : children) {
                            if (child.file != null) {
                                if ((child.name.toLowerCase().endsWith(".txt")) || (child.audio != null))
                                    items.add(child);
                            } else if ((child.folder != null) && (includeSubfolders)){
                                Log.d(TAG, "Adding folder to list of folders to query ...");
                                folderIDs.add(child.id);
                                folderNames.add(child.name);
                            }
                        }
                        IItemCollectionRequestBuilder builder = page.getNextPage();
                        if (builder != null)
                            page = builder.buildRequest().get();
                        else
                            page = null;
                    }

                    for (Item item : items) {
                        //if (!entry.isDeleted)
                        {
                            String fileID = item.id;
                            Log.d(TAG, "File ID: " + fileID);
                            String title = item.name;
                            String lowerCaseTitle = title.toLowerCase();
                            boolean audioFile = false;
                            for (String ext : AUDIO_FILE_EXTENSIONS)
                                if (lowerCaseTitle.endsWith(ext))
                                    audioFile = true;
                            this.publishProgress(String.format(SongList.this.getString(R.string.checking), title));
                            String safeFilename = makeSafeFilename(title);
                            Log.d(TAG, "Safe filename: " + safeFilename);
                            File existingLocalFile = existingCachedFiles.get(fileID);
                            boolean downloadRequired = true;
                            Date lastModified = item.lastModifiedDateTime.getTime();
                            if (existingLocalFile != null) {
                                Date localFileModified = new Date(existingLocalFile.lastModified());
                                Log.d(TAG, "OneDrive File was last modified " + lastModified);
                                Log.d(TAG, "Local File was last downloaded " + localFileModified);
                                if (localFileModified.after(lastModified)) {
                                    Log.d(TAG, "It hasn't changed since last download ... ignoring!");
                                    downloadRequired = false;
                                    existingCachedFiles.remove(fileID);
                                } else
                                    Log.d(TAG, "Looks like it has changed since last download ... re-downloading!");
                            } else
                                Log.d(TAG, "Appears to be a file that I don't have yet... downloading!");

                            if (downloadRequired) {
                                Log.d(TAG, "Downloading now ...");
                                this.publishProgress(String.format(SongList.this.getString(R.string.downloading), title));
                                existingLocalFile = downloadOneDriveFile(item, safeFilename);
                                existingCachedFiles.remove(fileID);
                            }

                            if (!audioFile)
                                downloadedFiles.add(new DownloadedFile(existingLocalFile,fileID,lastModified,currentFolderName));
                            else
                                mDownloadedAudioFiles.add(new AudioFile(title, existingLocalFile, fileID, lastModified));
                        }
                    }
                } catch (DbxException de) {
                    throw new IOException(de.getMessage(), de);
                }
            }
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.onedrive_string);
        }
    }

    abstract class CloudStorageFileDownloadTask extends AsyncTask<String, String, Boolean>
    {
        CloudStorageFileDownloadTask(CachedFileType updateType, String subfolderOrigin, Handler handler)
        {
            mUpdateType=updateType;
            mSubfolderOrigin=subfolderOrigin;
            mHandler=handler;
        }
        Handler mHandler;
        String mSubfolderOrigin;
        ProgressDialog mProgressDialog;
        CachedFileType mUpdateType;
        SongFile updatedSong=null;
        SetListFile updatedSet=null;
        MIDIAliasCachedFile updatedMIDIAlias=null;
        private String mReqFileID=null;
        private boolean mNoLongerExists=false;

        @Override
        protected Boolean doInBackground(String... paramParams) {
            for(int f=0;(f<paramParams.length) && (!mNoLongerExists);++f) {
                try {
                    mReqFileID = paramParams[f];
                    Log.d(TAG, "File ID: " + mReqFileID);
                    mNoLongerExists = downloadFile(mReqFileID,f);
                 }
                 catch (Exception ee) {
                    Log.d(TAG, "An error occurred: " + ee);
                    if(f==0)
                        mNoLongerExists = true;
                    mHandler.obtainMessage(BeatPrompterApplication.CLOUD_SYNC_ERROR,ee.getMessage()).sendToTarget();
                }
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (mProgressDialog!=null) {
                mProgressDialog.dismiss();}
            if(mUpdateType==CachedFileType.Song) {
                for (int f = mSongs.size() - 1; f >= 0; --f) {
                    SongFile sf = mSongs.get(f);
                    if ((updatedSong != null) && (sf.mStorageName.equals(updatedSong.mStorageName))) {
                        mSongs.set(f, updatedSong);
                        break;
                    }
                    if ((mNoLongerExists) && (sf.mStorageName.equals(mReqFileID))) {
                        mSongs.remove(f);
                    }
                }
            }
            else if(mUpdateType==CachedFileType.SetList)
            {
                for (int f = mSets.size() - 1; f >= 0; --f) {
                    SetListFile sf = mSets.get(f);
                    if ((updatedSet != null) && (sf.mStorageName.equals(updatedSet.mStorageName))) {
                        mSets.set(f, updatedSet);
                        break;
                    }
                    if ((mNoLongerExists) && (sf.mStorageName.equals(mReqFileID))) {
                        mSets.remove(f);
                    }
                }
            }
            else if(mUpdateType==CachedFileType.MIDIAliases)
            {
                for (int f = mMIDIAliasCachedFiles.size() - 1; f >= 0; --f) {
                    MIDIAliasCachedFile maf = mMIDIAliasCachedFiles.get(f);
                    if ((updatedMIDIAlias != null) && (maf.mStorageName.equals(updatedMIDIAlias.mStorageName))) {
                        mMIDIAliasCachedFiles.set(f, updatedMIDIAlias);
                        break;
                    }
                    if ((mNoLongerExists) && (maf.mStorageName.equals(mReqFileID))) {
                        mMIDIAliasCachedFiles.remove(f);
                    }
                }
            }
            try {
                writeDatabase();
                buildFilterList();
            } catch (Exception ioe) {
                Log.e(TAG, ioe.getMessage());
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle(SongList.this.getString(R.string.downloadingFiles));
            mProgressDialog.setMessage(String.format(SongList.this.getString(R.string.accessingCloudStorage),getCloudStorageName()));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.show();
        }

        boolean onFileDownloaded(File file,Date lastModified,boolean dependencyFile)
        {
            if(mUpdateType==CachedFileType.Song)
            {
                if (dependencyFile)
                    return true;
                try {
                    updatedSong = new SongFile(SongList.this, new DownloadedFile(file, mReqFileID, lastModified,mSubfolderOrigin), null,null);
                    return true;
                } catch (Exception ibpfe) {
                    Log.e(TAG, ibpfe.getMessage());
                }
            }
            else if(mUpdateType==CachedFileType.SetList) {
                try {
                    updatedSet = new SetListFile(SongList.this, new DownloadedFile(file, mReqFileID, lastModified,mSubfolderOrigin));
                    return true;
                } catch (InvalidBeatPrompterFileException ibpfe) {
                    Log.e(TAG, ibpfe.getMessage());
                }
            }
            else if(mUpdateType==CachedFileType.MIDIAliases)
            {
                try {
                    updatedMIDIAlias = new MIDIAliasCachedFile(SongList.this, new DownloadedFile(file, mReqFileID, lastModified,mSubfolderOrigin),mDefaultAliases);
                    return true;
                } catch (InvalidBeatPrompterFileException ibpfe) {
                    Log.e(TAG, ibpfe.getMessage());
                }
            }
            return false;
        }

        abstract boolean downloadFile(String fileID,int fileIndex) throws IOException;

        abstract String getCloudStorageName();
    }

    private class GoogleDriveFileDownloadTask extends CloudStorageFileDownloadTask
    {
        GoogleDriveFileDownloadTask(CachedFileType updateType, String subfolderOrigin, Handler handler)
        {
            super(updateType,subfolderOrigin, handler);
        }
        boolean downloadFile(String fileID,int fileIndex) throws IOException
        {
            boolean noLongerExists=false;
            try
            {
                com.google.api.services.drive.model.File file = getGoogleDriveService().files().get(fileID).setFields("id,name,mimeType,trashed,modifiedTime").execute();
                if (!file.getTrashed()) {
                    String title = file.getName();
                    Log.d(TAG, "File title: " + title);
                    String lowerCaseTitle = title.toLowerCase();
                    boolean dependencyFile = false;
                    if(mUpdateType==CachedFileType.Song)
                    {
                        for (String ext : AUDIO_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                dependencyFile = true;
                        for (String ext : IMAGE_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                dependencyFile = true;
                    }
                    this.publishProgress(String.format(SongList.this.getString(R.string.checking),title));
                    String safeFilename = makeSafeFilename(fileID);
                    Log.d(TAG, "Safe filename: " + safeFilename);

                    Log.d(TAG, "Downloading now ...");
                    this.publishProgress(String.format(SongList.this.getString(R.string.downloading),title));
                    File existingLocalFile = SongList.this.downloadGoogleDriveFile(file, safeFilename);
                    Date lastModified = new Date(file.getModifiedTime().getValue());
                    if(!onFileDownloaded(existingLocalFile,lastModified,dependencyFile))
                        noLongerExists=true;
                } else if(fileIndex==0)
                    noLongerExists = true;

            } catch (UserRecoverableAuthIOException uraioe) {
                startActivityForResult(uraioe.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
            }
            return noLongerExists;
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.google_drive_string);
        }
    }

    private class DropboxFileDownloadTask extends CloudStorageFileDownloadTask
    {
        DropboxFileDownloadTask(CachedFileType updateType, String subfolderOrigin, Handler handler)
        {
            super(updateType,subfolderOrigin,handler);
        }
        boolean downloadFile(String fileID,int fileIndex) throws IOException
        {
            boolean noLongerExists=false;
            try {
                Metadata mdata=mDropboxAPI.files().getMetadata(fileID);
                //if ((!entry.isDeleted)&&(!entry.isDir))
                if((mdata!=null) && (mdata instanceof FileMetadata))
                {
                    FileMetadata fmdata=(FileMetadata)mdata;
                    String title = fmdata.getName();
                    Log.d(TAG, "File title: " + title);
                    String lowerCaseTitle = title.toLowerCase();
                    boolean audioFile = false;
                    if(mUpdateType==CachedFileType.Song) {
                        for (String ext : AUDIO_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                audioFile = true;
                    }
                    this.publishProgress(String.format(SongList.this.getString(R.string.checking),title));
                    String safeFilename = makeSafeFilename(title);
                    Log.d(TAG, "Safe filename: " + safeFilename);

                    Log.d(TAG, "Downloading now ...");
                    this.publishProgress(String.format(SongList.this.getString(R.string.downloading),title));
                    File existingLocalFile = downloadDropboxFile(fmdata, safeFilename);
                    Date lastModified = fmdata.getServerModified();
                    if(!onFileDownloaded(existingLocalFile,lastModified,audioFile))
                        noLongerExists=true;
                } else if(fileIndex==0)
                    noLongerExists = true;
            } catch (DbxException ee) {
                throw new IOException(ee.getMessage(),ee);
            }
            return noLongerExists;
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.dropbox_string);
        }
    }

    private class OneDriveFileDownloadTask extends CloudStorageFileDownloadTask
    {
        OneDriveFileDownloadTask(CachedFileType updateType, String subfolderOrigin, Handler handler)
        {
            super(updateType,subfolderOrigin,handler);
        }
        boolean downloadFile(String fileID,int fileIndex) throws IOException
        {
            boolean noLongerExists=false;
            try {
                Item item=mOneDriveClient.getDrive().getItems(fileID).buildRequest().get();
                if((item!=null) && (item.file!=null))
                {
                    String title = item.name;
                    Log.d(TAG, "File title: " + title);
                    String lowerCaseTitle = title.toLowerCase();
                    boolean audioFile = false;
                    if(mUpdateType==CachedFileType.Song) {
                        for (String ext : AUDIO_FILE_EXTENSIONS)
                            if (lowerCaseTitle.endsWith(ext))
                                audioFile = true;
                    }
                    this.publishProgress(String.format(SongList.this.getString(R.string.checking),title));
                    String safeFilename = makeSafeFilename(title);
                    Log.d(TAG, "Safe filename: " + safeFilename);

                    Log.d(TAG, "Downloading now ...");
                    this.publishProgress(String.format(SongList.this.getString(R.string.downloading),title));
                    File existingLocalFile = downloadOneDriveFile(item, safeFilename);
                    Date lastModified = item.lastModifiedDateTime.getTime();
                    if(!onFileDownloaded(existingLocalFile,lastModified,audioFile))
                        noLongerExists=true;
                } else if(fileIndex==0)
                    noLongerExists = true;
            } catch (DbxException ee) {
                throw new IOException(ee.getMessage(),ee);
            }
            return noLongerExists;
        }

        String getCloudStorageName()
        {
            return SongList.this.getString(R.string.onedrive_string);
        }
    }

    boolean wasPowerwashed()
    {
        SharedPreferences sharedPrefs = getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID,Context.MODE_PRIVATE);
        boolean powerwashed = sharedPrefs.getBoolean(getString(R.string.pref_wasPowerwashed_key), false);
        sharedPrefs.edit().putBoolean(getString(R.string.pref_wasPowerwashed_key), false).apply();
        return powerwashed;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");

        if (wasPowerwashed()) {
            mGoogleApiClient.clearDefaultAccountAndReconnect();
            return;
        }

        try {
            doGoogleDriveSync();
        }
        catch(Exception se)
        {
            Toast.makeText(this,se.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void doGoogleDriveSync() throws SecurityException
    {
        if(mGoogleApiClient.isConnected())
        {
            mDriveAccountName = Plus.AccountApi.getAccountName(mGoogleApiClient);

            if(mSongToRefresh!=null) {
                try {
                    forceSongDownload();
                } finally {
                    mGoogleApiClient.disconnect();
                }
            } else if(mSetToRefresh!=null){
                try {
                    forceSetDownload();
                } finally {
                    mGoogleApiClient.disconnect();
                }
            } else if(mMIDIAliasCachedFileToRefresh!=null) {
                try {
                    forceMIDIAliasCachedFileDownload();
                } finally {
                    mGoogleApiClient.disconnect();
                }
            }
            else {
                try {
                    CloudDownloadTask cdt=new GoogleDriveDownloadTask(mSongListHandler);
                    cdt.execute();
                } finally {
                    mGoogleApiClient.disconnect();
                }
            }
        }
        else
            Toast.makeText(this,R.string.google_api_not_connected,Toast.LENGTH_LONG).show();
    }

    private void doOneDriveSync() throws SecurityException
    {
        if(mOneDriveClient!=null)
        {
            if(mSongToRefresh!=null)
                forceSongDownload();
            else if(mSetToRefresh!=null)
                forceSetDownload();
            else if(mMIDIAliasCachedFileToRefresh!=null)
                forceMIDIAliasCachedFileDownload();
            else {
                CloudDownloadTask cdt=new OneDriveDownloadTask(mSongListHandler);
                cdt.execute();
            }
        }
        else
            initializeOneDriveAPI();
    }

    private void doDropBoxSync() throws SecurityException
    {
        if(mDropboxAPI!=null)
        {
            if(mSongToRefresh!=null)
                forceSongDownload();
            else if(mSetToRefresh!=null)
                forceSetDownload();
            else if(mMIDIAliasCachedFileToRefresh!=null)
                forceMIDIAliasCachedFileDownload();
            else {
                CloudDownloadTask cdt=new DropboxDownloadTask(mSongListHandler);
                cdt.execute();
            }
        }
        else
            initializeDropboxAPI();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    private void clearCacheFolder(File folder)
    {
        try {
            if (folder.exists()) {
                File[] contents = folder.listFiles();
                for (File f : contents) {
                    if(!f.isDirectory()) {
                        Log.d(TAG, "Deleting " + f.getAbsolutePath());
                        if (!f.delete())
                            Log.e(TAG, "Failed to delete " + f.getAbsolutePath());
                    }
                }
            }
        }
        catch(Exception e)
        {
            Log.e(TAG,"Failed to clear cache folder.",e);
        }
    }

    private void sortSongList()
    {
        if((mSelectedFilter==null)||(mSelectedFilter.mCanSort)) {
            switch (mSortingPreference) {
                case DATE:
                    sortSongsByDateModified();
                    return;
                case ARTIST:
                    sortSongsByArtist();
                    return;
                case TITLE:
                    sortSongsByTitle();
                    return;
                case KEY:
                    sortSongsByKey();
            }
        }
    }

    private void sortSongsByTitle()
    {
        mPlaylist.sortByTitle(getString(R.string.lowerCaseThe)+" ");
    }

    private void sortSongsByArtist()
    {
        mPlaylist.sortByArtist(getString(R.string.lowerCaseThe)+" ");
    }

    private void sortSongsByDateModified()
    {
        mPlaylist.sortByDateModified();
    }

    private void sortSongsByKey()
    {
        mPlaylist.sortByKey();
    }

    private void buildList()
    {
        if((mSelectedFilter!=null)&&(mSelectedFilter instanceof MIDIAliasFilesFilter))
            mListAdapter=new MIDIAliasListAdapter(this,mMIDIAliasCachedFiles);
        else
            mListAdapter = new SongListAdapter(this, mPlaylist.getNodesAsArray());

        ListView listView = (ListView) findViewById(R.id.listView);

        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setAdapter(mListAdapter);

        listView.setSelectionFromTop(index, top);
    }

    private void readDatabase() throws IOException, ParserConfigurationException, SAXException
    {
        File bpdb=new File(mBeatPrompterDataFolder,XML_DATABASE_FILE_NAME);
        if(bpdb.exists())
        {
            clearCachedFileArrays();

            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xmlDoc = docBuilder.parse(bpdb);
            NodeList songFiles = xmlDoc.getElementsByTagName(SongFile.SONGFILE_ELEMENT_TAG_NAME);
            for (int f = 0; f < songFiles.getLength(); ++f) {
                Node n = songFiles.item(f);
                SongFile song = SongFile.readFromXMLElement(this, (Element)n);
                mSongs.add(song);
            }
            NodeList setFiles = xmlDoc.getElementsByTagName(SetListFile.SETLISTFILE_ELEMENT_TAG_NAME);
            for (int f = 0; f < setFiles.getLength(); ++f) {
                Node n = setFiles.item(f);
                SetListFile set = SetListFile.readFromXMLElement(this, (Element)n);
                mSets.add(set);
            }
            NodeList imageFiles = xmlDoc.getElementsByTagName(ImageFile.IMAGEFILE_ELEMENT_TAG_NAME);
            for (int f = 0; f < imageFiles.getLength(); ++f) {
                Node n = imageFiles.item(f);
                ImageFile imageFile = ImageFile.readFromXMLElement((Element)n);
                mImageFiles.add(imageFile);
            }
            NodeList audioFiles = xmlDoc.getElementsByTagName(AudioFile.AUDIOFILE_ELEMENT_TAG_NAME);
            for (int f = 0; f < audioFiles.getLength(); ++f) {
                Node n = audioFiles.item(f);
                AudioFile audioFile = AudioFile.readFromXMLElement((Element)n);
                mAudioFiles.add(audioFile);
            }
            NodeList aliasFiles = xmlDoc.getElementsByTagName(MIDIAliasCachedFile.MIDIALIASFILE_ELEMENT_TAG_NAME);
            for (int f = 0; f < aliasFiles.getLength(); ++f) {
                Node n = aliasFiles.item(f);
                MIDIAliasCachedFile midiAliasCachedFile = MIDIAliasCachedFile.readFromXMLElement(this,(Element)n,mDefaultAliases);
                mMIDIAliasCachedFiles.add(midiAliasCachedFile);
            }
            buildFilterList();
        }
    }

    private void writeDatabase() throws IOException, ParserConfigurationException, SAXException, TransformerException
    {
        File bpdb=new File(mBeatPrompterDataFolder,XML_DATABASE_FILE_NAME);
        if(!bpdb.delete())
            Log.e(TAG,"Failed to delete database file.");
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d=docBuilder.newDocument();
        Element root=d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG);
        d.appendChild(root);
        for(SongFile s:mSongs)
            s.writeToXML(d,root);
        for(AudioFile a:mAudioFiles)
            a.writeToXML(d,root);
        for(ImageFile i:mImageFiles)
            i.writeToXML(d,root);
        for(SetListFile s:mSets)
            s.writeToXML(d,root);
        for(MIDIAliasCachedFile maf:mMIDIAliasCachedFiles)
            maf.writeToXML(d,root);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(bpdb);
        Source input = new DOMSource(d);
        transformer.transform(input, output);
    }

    private void setSortingPreference(SortingPreference pref)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putInt("pref_sorting",pref==SortingPreference.TITLE?0:pref==SortingPreference.ARTIST?1:pref==SortingPreference.DATE?2:3).apply();
        mSortingPreference=pref;
        sortSongList();
    }

    private void buildFilterList() {
        Log.d(TAG,"Building taglist ...");
        mFilters=new ArrayList<>();
        Filter mOldSelectedFilter=mSelectedFilter;
        mSelectedFilter=null;
        Map<String,ArrayList<SongFile>> tagDicts=new HashMap<>();
        Map<String,ArrayList<SongFile>> folderDicts=new HashMap<>();
        for(SongFile song: mSongs)
        {
            for(String tag: song.mTags)
            {
                ArrayList<SongFile> songs=tagDicts.get(tag);
                if(songs==null)
                {
                    songs = new ArrayList<>();
                    tagDicts.put(tag,songs);
                }
                songs.add(song);
            }
            if(song.mSubfolder!=null && song.mSubfolder.length()>0)
            {
                ArrayList<SongFile> songs=folderDicts.get(song.mSubfolder);
                if(songs==null)
                {
                    songs = new ArrayList<>();
                    folderDicts.put(song.mSubfolder,songs);
                }
                songs.add(song);
            }
        }

        for(String key:tagDicts.keySet())
        {
            ArrayList<SongFile> songs=tagDicts.get(key);
            TagFilter tf=new TagFilter(key,songs);
            if(tf.equals(mOldSelectedFilter))
                mSelectedFilter=tf;
            mFilters.add(tf);
        }

        for(String key:folderDicts.keySet())
        {
            ArrayList<SongFile> songs=folderDicts.get(key);
            FolderFilter ff=new FolderFilter(key,songs);
            if(ff.equals(mOldSelectedFilter))
                mSelectedFilter=ff;
            mFilters.add(ff);
        }

        for(SetListFile slf:mSets)
        {
            SetListFileFilter filter=new SetListFileFilter(slf,mSongs);
            mFilters.add(filter);
        }

        Collections.sort(mFilters, new Comparator<Filter>() {
            @Override
            public int compare(Filter f1, Filter f2) {
                String tag1 = f1.mName.toLowerCase();
                String tag2 = f2.mName.toLowerCase();
                return tag1.compareTo(tag2);
            }
        });

        if(mTemporarySetListFilter!=null)
            mFilters.add(0, mTemporarySetListFilter);

        Filter allSongsFilter=new AllSongsFilter(getString(R.string.no_tag_selected),mSongs);
        mFilters.add(0, allSongsFilter);

        if(!mMIDIAliasCachedFiles.isEmpty())
        {
            MIDIAliasFilesFilter filter=new MIDIAliasFilesFilter(getString(R.string.midi_alias_files));
            mFilters.add(filter);
        }

        if(mSelectedFilter==null)
            mSelectedFilter=allSongsFilter;

        applyFileFilter(mSelectedFilter);

        invalidateOptionsMenu();
    }

    private SortingPreference getSortingPreference()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int pref=sharedPref.getInt("pref_sorting",0);
        return pref==0?SortingPreference.TITLE:(pref==1?SortingPreference.ARTIST:(pref==2?SortingPreference.DATE:SortingPreference.KEY));
    }

    public boolean fullVersionUnlocked()
    {
        if(mFullVersionUnlocked)
            return true;
        try
        {
            if(mIAPService!=null) {
                Bundle ownedItems = mIAPService.getPurchases(3, getPackageName(), "inapp", null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    if (ownedSkus != null)
                        for (String sku : ownedSkus)
                            mFullVersionUnlocked |= sku.equalsIgnoreCase(FULL_VERSION_SKU_NAME);
                }
            }
        }
        catch(Exception e)
        {
            Log.e(TAG,"Failed to check for purchased version.",e);
        }
        return mFullVersionUnlocked;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.sort_songs);

        if ((item != null)&&(mSelectedFilter!=null)) {
            item.setEnabled(mSelectedFilter.mCanSort);
        }
        if(fullVersionUnlocked())
        {
            item = menu.findItem(R.id.buy_full_version);

            if (item != null) {
                item.setVisible(false);
            }
        }
        item = menu.findItem(R.id.synchronize);
        item.setEnabled(canSynchronizeFiles());
//        item = menu.findItem(R.id.connect_to_leader);
//        item.setEnabled(((BeatPrompterApplication)SongList.this.getApplicationContext()).getBluetoothMode()==BluetoothMode.client);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu=menu;
        getMenuInflater().inflate(R.menu.songlistmenu, menu);
        Spinner spinner = (Spinner) menu.findItem(R.id.tagspinner).getActionView();
        spinner.setOnItemSelectedListener(this);
        FilterListAdapter filterListAdapter = new FilterListAdapter(this, mFilters);
        spinner.setAdapter(filterListAdapter);

        updateBluetoothIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.synchronize:
                synchronizeFiles();
                return true;
            case R.id.sort_songs:
                if(mSelectedFilter.mCanSort) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    CharSequence items[] = new CharSequence[]{getString(R.string.byTitle), getString(R.string.byArtist), getString(R.string.byDate),getString(R.string.byKey)};
                    adb.setItems(items, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface d, int n) {
                            if (n == 0) {
                                d.dismiss();
                                setSortingPreference(SortingPreference.TITLE);
                                buildList();
                            } else if (n == 1) {
                                d.dismiss();
                                setSortingPreference(SortingPreference.ARTIST);
                                buildList();
                            } else if (n == 2) {
                                d.dismiss();
                                setSortingPreference(SortingPreference.DATE);
                                buildList();
                            } else if (n == 3) {
                                d.dismiss();
                                setSortingPreference(SortingPreference.KEY);
                                buildList();
                            }
                        }

                    });
                    adb.setTitle(getString(R.string.sortSongs));
                    AlertDialog ad=adb.create();
                    ad.setCanceledOnTouchOutside(true);
                    ad.show();
                }
                return true;
//            case R.id.connect_to_leader:
//                ((BeatPrompterApplication)this.getApplicationContext()).connectToLeader();
//                return true;
            case R.id.settings:
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.buy_full_version:
                buyFullVersion();
                return true;
            case R.id.about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        applyFileFilter(mFilters.get(position));
    }

    public void buyFullVersion()
    {
        try
        {
            Bundle buyIntentBundle = mIAPService.getBuyIntent(3, getPackageName(),
                    FULL_VERSION_SKU_NAME, "inapp", "");
            int response = buyIntentBundle.getInt("RESPONSE_CODE");
            if (response == 0)
            {
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                if(pendingIntent!=null)
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            GOOGLE_PLAY_TRANSACTION_FINISHED, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                            Integer.valueOf(0));
            }
        }
        catch(Exception e)
        {
            Log.e(TAG,"Failed to buy full version.",e);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        applyFileFilter(null);
    }

    private void applyFileFilter(Filter filter) {
        mSelectedFilter = filter;
        if(filter instanceof SongFilter)
            mPlaylist = new Playlist(((SongFilter)filter).mSongs);
        else
            mPlaylist=new Playlist();
        sortSongList();
        buildList();
        showSetListMissingSongs();
    }

    private void showSetListMissingSongs()
    {
        if(mSelectedFilter instanceof SetListFileFilter)
        {
            SetListFileFilter slf=(SetListFileFilter)mSelectedFilter;
            ArrayList<String> missing=slf.mMissingSongs;
            if((missing.size()>0)&&(!slf.mWarned))
            {
                slf.mWarned=true;
                String message=String.format(getString(R.string.missing_songs_message),missing.size());
                message+="\n\n";
                for(int f=0;f<Math.min(missing.size(),3);++f)
                {
                    message+=missing.get(f);
                    message+="\n";
                }
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(R.string.missing_songs_dialog_title);
                alertDialog.setMessage(message);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
    }

    private void fetchFilesFromGoogleDrive()
    {
        mGoogleApiClient.connect();
    }
    private void fetchFilesFromDropbox()
    {
        doDropBoxSync();
    }
    private void fetchFilesFromOneDrive() { doOneDriveSync();}

    private void showAboutDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.about_dialog, null);
        builder.setView(view);
        AlertDialog customAD = builder.create();
        customAD.setCanceledOnTouchOutside(true);
        customAD.show();
    }

   /* private Date getLastSyncDate()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return new Date(sharedPref.getLong("pref_lastSyncDate",0));
    }*/

    private void setLastSyncDate(Date date)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putLong("pref_lastSyncDate",date.getTime()).apply();
    }

    private void clearCachedFileArrays()
    {
        mPlaylist=new Playlist();
        mSongs=new ArrayList<>();
        mSets=new ArrayList<>();
        mMIDIAliasCachedFiles=new ArrayList<>();
    }

    void deleteAllFiles()
    {
        // Clear both cache folders
        setLastSyncDate(new Date(0));
        clearCacheFolder(mDemoFolder);
        clearCacheFolder(mDropboxFolder);
        clearCacheFolder(mOneDriveFolder);
        clearCacheFolder(mGoogleDriveFolder);
        clearCachedFileArrays();
        buildFilterList();
        try {
            writeDatabase();
//            buildList();
        }
        catch(Exception ioe)
        {
            Log.e(TAG,ioe.getMessage());
        }
    }

    void clearCache()
    {
        deleteAllFiles();
        Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_LONG).show();
    }

    public void processBluetoothMessage(BluetoothMessage btm)
    {
        if(btm instanceof ChooseSongMessage)
        {
            ChooseSongMessage csm = (ChooseSongMessage) btm;
            String title = csm.mTitle;
            String track = csm.mTrack.length() == 0 ? null : csm.mTrack;

            boolean beat = csm.mBeatScroll;
            boolean smooth = csm.mSmoothScroll;
            ScrollingMode scrollingMode=beat?ScrollingMode.Beat:(smooth?ScrollingMode.Smooth:ScrollingMode.Manual);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SongList.this);
            String prefName=getString(R.string.pref_mimicBandLeaderDisplay_key);
            boolean mimicDisplay=(scrollingMode==ScrollingMode.Manual?sharedPrefs.getBoolean(prefName, true):false);

            // Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
            // Also, beat and smooth scrolling should never mimic.
            SongDisplaySettings nativeSettings=getSongDisplaySettings(scrollingMode);
            SongDisplaySettings sourceSettings=mimicDisplay?new SongDisplaySettings(csm):nativeSettings;

            for (SongFile sf : mSongs)
                if (sf.mTitle.equals(title))
                {
                    if(mSongListActive)
                        playSong(null, sf, track, scrollingMode,true,false,nativeSettings,sourceSettings);
                    else
                        mSongToLoadOnResume=new LoadingSongFile(sf,track,scrollingMode,null,true,
                                false,isDemoSong(sf),nativeSettings,sourceSettings);
                    break;
                }
        }
    }

    class MIDIUSBOutTask extends MIDIUSBTask
    {
        MIDIUSBOutTask()
        {
            super(null,null,true);
        }
        public void doWork()
        {
            MIDIMessage message;
            try {
                while (((message = BeatPrompterApplication.mMIDIOutQueue.take()) != null) && (!getShouldStop()))
                {
                    UsbDeviceConnection connection=getConnection();
                    UsbEndpoint endpoint=getEndpoint();
                    if((connection!=null)&&(endpoint!=null))
                        connection.bulkTransfer(endpoint, message.mMessageBytes, message.mMessageBytes.length, 60000);
                }
            } catch (InterruptedException ie) {
                Log.d(TAG, "Interrupted while attempting to retrieve MIDI out message.", ie);
            }
        }
        void cleanup()
        {
            BeatPrompterApplication.mMIDIOutQueue.clear();
        }
    }

    class MIDIUSBInTask extends MIDIUSBTask
    {
        byte[] mBuffer=null;
        int mBufferSize;

        MIDIUSBInTask(UsbDeviceConnection connection,UsbEndpoint endpoint)
        {
            super(connection,endpoint,true);
            mBuffer = new byte[mBufferSize = endpoint.getMaxPacketSize()];
            // Read all incoming data until there is none left. Basically, clear anything that
            // has been buffered before we accepted the connection.
            // On the offchance that there is a neverending barrage of data coming in at a ridiculous
            // speed, let's say 1K of data, max, to prevent lockup.
            int bufferClear=0;
            int dataRead;
            do
            {
                dataRead=connection.bulkTransfer(endpoint,mBuffer,mBufferSize,250);
                if(dataRead>0)
                    bufferClear+=dataRead;
            }
            while((dataRead>0)&&(dataRead==mBufferSize)&&(!getShouldStop())&&(bufferClear<1024));
        }

        public void doWork()
        {
            boolean inSysEx=false;
            int dataRead;
            byte[] preBuffer=null;
            UsbEndpoint endpoint=getEndpoint();
            UsbDeviceConnection connection=getConnection();

            while(((dataRead=connection.bulkTransfer(endpoint,mBuffer,mBufferSize,1000))>0)&&(!getShouldStop()))
            {
                byte[] workBuffer;
                if(preBuffer==null)
                    workBuffer=mBuffer;
                else
                {
                    workBuffer=new byte[preBuffer.length+dataRead];
                    System.arraycopy(preBuffer,0,workBuffer,0,preBuffer.length);
                    System.arraycopy(mBuffer,0,workBuffer,preBuffer.length,dataRead);
                    dataRead+=preBuffer.length;
                    preBuffer=null;
                }
                for(int f=0;f<dataRead;++f)
                {
                    byte messageByte=workBuffer[f];
                    // All interesting MIDI signals have the top bit set.
                    if((messageByte&0x80)!=0) {
                        if (!inSysEx) {
                            if ((messageByte == MIDIMessage.MIDI_START_BYTE) || (messageByte == MIDIMessage.MIDI_CONTINUE_BYTE) || (messageByte == MIDIMessage.MIDI_STOP_BYTE)) {
                                // These are single byte messages.
                                try {
                                    BeatPrompterApplication.mMIDISongDisplayInQueue.put(new MIDIIncomingMessage(new byte[]{messageByte}));
                                    if(messageByte==MIDIMessage.MIDI_START_BYTE)
                                        Log.d(MIDI_TAG, "Received MIDI Start message.");
                                    else if(messageByte==MIDIMessage.MIDI_CONTINUE_BYTE)
                                        Log.d(MIDI_TAG, "Received MIDI Continue message.");
                                    if(messageByte==MIDIMessage.MIDI_STOP_BYTE)
                                        Log.d(MIDI_TAG, "Received MIDI Stop message.");
                                } catch (InterruptedException ie) {
                                    Log.d(TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                }
                            } else if(messageByte==MIDIMessage.MIDI_SONG_POSITION_POINTER_BYTE)
                            {
                                // This message requires two additional bytes.
                                if (f < dataRead - 2)
                                    try {
                                        MIDIIncomingMessage msg = new MIDIIncomingSongPositionPointerMessage(new byte[]{messageByte, workBuffer[++f], workBuffer[++f]});
                                        Log.d(MIDI_TAG, "Received MIDI Song Position Pointer message: " + msg.toString());
                                        BeatPrompterApplication.mMIDISongDisplayInQueue.put(msg);
                                    } catch (InterruptedException ie) {
                                        Log.d(TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                    }
                                else if (f < dataRead - 1)
                                    preBuffer = new byte[]{messageByte, workBuffer[++f]};
                                else
                                    preBuffer = new byte[]{messageByte};
                            }
                            else if (messageByte == MIDIMessage.MIDI_SONG_SELECT_BYTE) {
                                if (f < dataRead - 1)
                                    try {
                                        MIDIIncomingMessage msg = new MIDIIncomingMessage(new byte[]{messageByte, workBuffer[++f]});
                                        Log.d(MIDI_TAG, "Received MIDI SongSelect message: " + msg.toString());
                                        BeatPrompterApplication.mMIDISongListInQueue.put(msg);
                                    } catch (InterruptedException ie) {
                                        Log.d(TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                    }
                                else
                                    preBuffer = new byte[]{workBuffer[f]};
                            } else if (messageByte == MIDIMessage.MIDI_SYSEX_START_BYTE) {
                                Log.d(MIDI_TAG, "Received MIDI SysEx start message.");
                                inSysEx = true;
                            } else {
                                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SongList.this);
                                int channelsToListenTo = sharedPrefs.getInt(getString(R.string.pref_midiIncomingChannels_key), 65535);
                                byte messageByteWithoutChannel = (byte) (messageByte & 0xF0);
                                // System messages start with 0xF0, we're past caring about those.
                                if(messageByteWithoutChannel!=(byte)0xF0) {
                                    byte channel = (byte) (messageByte & 0x0F);
                                    if ((channelsToListenTo & (1 << channel)) != 0) {
                                        if (messageByteWithoutChannel == MIDIMessage.MIDI_PROGRAM_CHANGE_BYTE) {
                                            if (f < dataRead - 1)
                                                try {
                                                    MIDIIncomingMessage msg = new MIDIIncomingMessage(new byte[]{messageByte, workBuffer[++f]});
                                                    Log.d(MIDI_TAG, "Received MIDI ProgramChange message: " + msg.toString());
                                                    BeatPrompterApplication.mMIDISongListInQueue.put(msg);
                                                } catch (InterruptedException ie) {
                                                    Log.d(TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                                }
                                            else
                                                preBuffer = new byte[]{messageByte};
                                        } else if (messageByteWithoutChannel == MIDIMessage.MIDI_CONTROL_CHANGE_BYTE) {
                                            // This message requires two additional bytes.
                                            if (f < dataRead - 2)
                                                try {
                                                    MIDIIncomingMessage msg = new MIDIIncomingMessage(new byte[]{messageByte, workBuffer[++f], workBuffer[++f]});
                                                    if ((msg.isLSBBankSelect()) || (msg.isMSBBankSelect())) {
                                                        Log.d(MIDI_TAG, "Received MIDI Control Change message: " + msg.toString());
                                                        BeatPrompterApplication.mMIDISongListInQueue.put(msg);
                                                    }
                                                } catch (InterruptedException ie) {
                                                    Log.d(TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                                }
                                            else if (f < dataRead - 1)
                                                preBuffer = new byte[]{messageByte, workBuffer[++f]};
                                            else
                                                preBuffer = new byte[]{messageByte};
                                        }
                                    }
                                }
                            }
                        } else if (messageByte == MIDIMessage.MIDI_SYSEX_END_BYTE) {
                            Log.d(MIDI_TAG, "Received MIDI SysEx end message.");
                            inSysEx = false;
                        }
                    }
                }
            }
        }
    }

    class MIDIInTask extends Task
    {
        Handler mHandler;
        MIDIInTask(Handler handler)
        {
            super(false);
            mHandler=handler;
        }
        public void doWork()
        {
            MIDIIncomingMessage message;
            try {
                while (((message = BeatPrompterApplication.mMIDISongListInQueue.take()) != null) && (!getShouldStop())) {
                    if(message.isMSBBankSelect())
                        mHandler.obtainMessage(BeatPrompterApplication.MIDI_MSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue()).sendToTarget();
                    else if(message.isLSBBankSelect())
                        mHandler.obtainMessage(BeatPrompterApplication.MIDI_LSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue()).sendToTarget();
                    else if(message.isProgramChange())
                        mHandler.obtainMessage(BeatPrompterApplication.MIDI_PROGRAM_CHANGE,message.getMIDIChannel(),message.getProgramChangeValue()).sendToTarget();
                    else if(message.isSongSelect())
                        mHandler.obtainMessage(BeatPrompterApplication.MIDI_SONG_SELECT,message.getSongSelectValue()).sendToTarget();
                }
            } catch (InterruptedException ie) {
                Log.d(TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
            }
        }
    }

    class MIDISongDisplayInTask extends Task
    {
        MIDISongDisplayInTask()
        {
            super(false);
        }
        public void doWork()
        {
            try {
                while ((BeatPrompterApplication.mMIDISongDisplayInQueue.take() != null) && (!getShouldStop())) {
                    Log.d(MIDI_TAG,"Discarding message intended for song display mode");
                    // Do nothing. These messages aren't meant for this activity.
                }
            } catch (InterruptedException ie) {
                Log.d(TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
            }
        }
    }

    class SongLoaderTask extends Task {
        Context mContext;

        CancelEvent mCancelEvent=null;
        LoadingSongFile mLoadingSongFile=null;

        Handler mSongLoadHandler=null;
        final Object mSongLoadHandlerSync = new Object();
        final Object mLoadingSongFileSync = new Object();
        final Object mCancelEventSync = new Object();

        private Handler getSongLoadHandler()
        {
            synchronized (mSongLoadHandlerSync)
            {
                return mSongLoadHandler;
            }
        }
        private void setSongLoadHandler(Handler handler)
        {
            synchronized (mSongLoadHandlerSync)
            {
                mSongLoadHandler=handler;
            }
        }
        private LoadingSongFile getLoadingSongFile()
        {
            synchronized (mLoadingSongFileSync)
            {
                LoadingSongFile result=mLoadingSongFile;
                mLoadingSongFile=null;
                return result;
            }
        }
        private void setLoadingSongFile(LoadingSongFile lsf,CancelEvent cancelEvent)
        {
            synchronized (mLoadingSongFileSync)
            {
                CancelEvent existingCancelEvent=getCancelEvent();
                if(existingCancelEvent!=null)
                    existingCancelEvent.set();
                setCancelEvent(cancelEvent);
                mLoadingSongFile=lsf;
            }
        }
        private CancelEvent getCancelEvent()
        {
            synchronized (mCancelEventSync)
            {
                return mCancelEvent;
            }
        }
        private void setCancelEvent(CancelEvent cancelEvent)
        {
            synchronized (mCancelEventSync)
            {
                mCancelEvent=cancelEvent;
            }
        }

        SongLoaderTask(Context context)
        {
            super(true);
            mContext=context;
        }
        void doWork()
        {
            LoadingSongFile lsf=getLoadingSongFile();
            if(lsf!=null) {
                System.gc();
                Handler songLoadHandler=getSongLoadHandler();
                CancelEvent cancelEvent = getCancelEvent();
                try {
                    Song loadingSong = lsf.load(mContext, mFullVersionUnlocked, cancelEvent, songLoadHandler,getMIDIAliases());
                    if (cancelEvent.isCancelled())
                        songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_CANCELLED).sendToTarget();
                    else {
                        BeatPrompterApplication.setCurrentSong(loadingSong);
                        songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_COMPLETED).sendToTarget();
                    }
                }
                catch(IOException ioe)
                {
                    songLoadHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_FAILED,ioe.getMessage()).sendToTarget();
                }
                System.gc();
            }
            else
                try {
                    // Nothing to do, wait a bit
                    Thread.sleep(250);
                }
                catch(InterruptedException ie)
                {
                }
        }
        void setSongToLoad(LoadingSongFile lsf,Handler handler,CancelEvent cancelEvent)
        {
            setSongLoadHandler(handler);
            setLoadingSongFile(lsf,cancelEvent);
        }
    }

    private class SongLoadTask extends AsyncTask<String, Integer, Boolean> {
        Semaphore mTaskEndSemaphore=new Semaphore(0);
        CancelEvent mCancelEvent=new CancelEvent();
        boolean mCancelled=false;
        String mProgressTitle="";

        SongLoadTask(LoadingSongFile lsf,Handler handler)
        {
            mLoadingSongFile=lsf;
            this.mSongListHandler=handler;
        }
        LoadingSongFile mLoadingSongFile=null;
        ProgressDialog mProgressDialog;
        Handler mSongListHandler=null;

        Handler mSongLoadHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case BeatPrompterApplication.SONG_LOAD_COMPLETED:
                        mTaskEndSemaphore.release();
                        SongLoadTask.this.mSongListHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_COMPLETED,msg.obj).sendToTarget();
                        break;
                    case BeatPrompterApplication.SONG_LOAD_CANCELLED:
                        mCancelled=true;
                        mTaskEndSemaphore.release();
                        break;
                    case BeatPrompterApplication.SONG_LOAD_LINE_READ:
                        mProgressTitle=SongList.this.getString(R.string.loadingSong);
                        publishProgress(msg.arg1,msg.arg2);
                        break;
                    case BeatPrompterApplication.SONG_LOAD_LINE_PROCESSED:
                        mProgressTitle=SongList.this.getString(R.string.processingSong);
                        publishProgress(msg.arg1,msg.arg2);
                        break;
                    case BeatPrompterApplication.SONG_LOAD_FAILED:
                        SongLoadTask.this.mSongListHandler.obtainMessage(BeatPrompterApplication.SONG_LOAD_FAILED,msg.obj).sendToTarget();
                        break;
                }
            }
        };

        @Override
        protected Boolean doInBackground(String... paramParams) {
            try
            {
                mTaskEndSemaphore.acquire();
            }
            catch(InterruptedException ie)
            {
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(values.length>1) {
                mProgressDialog.setMessage(mProgressTitle+mLoadingSongFile.mSongFile.mTitle);
                mProgressDialog.setMax(values[1]);
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
            Log.d(AUTOLOAD_TAG,"In load task PostExecute.");
            super.onPostExecute(b);
            if (mProgressDialog!=null) {
                mProgressDialog.dismiss();}
            if(mCancelled)
                Log.d(AUTOLOAD_TAG,"Song load was cancelled.");
            else
                Log.d(AUTOLOAD_TAG,"Song loaded successfully.");
                Log.d(AUTOLOAD_TAG,"Song loaded successfully.");
            synchronized (mSongLoadSyncObject)
            {
                mSongLoadTask=null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(mLoadingSongFile.mSongFile.mTitle);
            mProgressDialog.setMax(mLoadingSongFile.mSongFile.mLines);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mCancelled=true;
                    mCancelEvent.set();
                }
            });
            mProgressDialog.show();
        }

        void loadSong()
        {
            if(SongDisplayActivity.mSongDisplayActive) {
                mSongToLoadOnResume=mLoadingSongFile;
                if(!BeatPrompterApplication.cancelCurrentSong(mLoadingSongFile.mSongFile))
                    mSongToLoadOnResume=null;
                return;
            }

            ((BeatPrompterApplication)SongList.this.getApplicationContext()).broadcastMessageToClients(new ChooseSongMessage(mLoadingSongFile));
            mSongLoaderTask.setSongToLoad(mLoadingSongFile,mSongLoadHandler,mCancelEvent);
            this.execute();
        }
    }

    ArrayList<MIDIAlias> getMIDIAliases()
    {
        ArrayList<MIDIAlias> aliases=new ArrayList<>(mDefaultAliases);
        aliases.addAll(mDefaultAliases);
        for(MIDIAliasCachedFile maf:mMIDIAliasCachedFiles)
            aliases.addAll(maf.getAliases());
        return aliases;
    }

    void parseDefaultAliasFile()
    {
        mDefaultAliases=new ArrayList<>();
        InputStream inputStream =null;
        try
        {
            inputStream = getAssets().open(DEFAULT_MIDI_ALIASES_FILENAME);
            if(inputStream!=null)
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                    MIDIAliasFile maf = new MIDIAliasFile(this,br,getString(R.string.default_alias_set_name));
                    mDefaultAliases = maf.mAliases;
                }
                catch(Exception e)
                {
                    Log.d(TAG, "Error reading default MIDI aliases",e);
                }
        }
        catch(IOException ioe)
        {
            Toast.makeText(this,ioe.getMessage(),Toast.LENGTH_LONG).show();
        }
        finally
        {
            try {
                if(inputStream!=null)
                    inputStream.close();
            }
            catch(IOException e)
            {
                Log.d(TAG, "Error closing input stream",e);
            }
        }
    }

    boolean isFirstRun()
    {
        SharedPreferences sharedPrefs = getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID,Context.MODE_PRIVATE);
        String prefName=getString(R.string.pref_firstRun_key);
        boolean firstRun=sharedPrefs.getBoolean(prefName, true);
        sharedPrefs.edit().putBoolean(prefName,false).apply();
        return firstRun;
    }

    void showFirstRunMessages()
    {
        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(getApplicationContext(), IntroActivity.class);
                startActivity(i);
            }
        });

        // Start the thread
        t.start();
    }

    File createDemoSongFile()
    {
        String demoFileText=getString(R.string.demo_song);
        File destinationSongFile = new File(mDemoFolder, DEMO_SONG_FILENAME);
        BufferedWriter bw=null;
        try
        {
            bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destinationSongFile)));
            bw.write(demoFileText);
        }
        catch(Exception e)
        {
            Log.d(TAG,"Failed to create demo file",e);
            destinationSongFile=null;
        }
        finally
        {
            if(bw!=null)
                try
                {
                    bw.close();
                }
                catch(Exception e)
                {
                    Log.d(TAG,"Failed to close demo file",e);
                }
        }
        return destinationSongFile;
    }

    void copyAssetsFileToDemoFolder(String filename,File destination)
    {
        InputStream inputStream=null;
        OutputStream outputStream=null;
        try {
            inputStream = getAssets().open(filename);
            if (inputStream != null) {
                outputStream = new FileOutputStream(destination);
                int n;
                byte[] buffer = new byte[1024];
                while((n = inputStream.read(buffer)) > -1) {
                    outputStream.write(buffer, 0, n);
                }
                outputStream.close();
                inputStream.close();
            }
        } catch (IOException ioe) {
            Toast.makeText(this, ioe.getMessage(), Toast.LENGTH_LONG).show();
        }
        finally
        {
            try {
                if(inputStream!=null)
                    inputStream.close();
            }
            catch(IOException e)
            {
                Log.d(TAG,"Error closing input stream",e);
            }
            try {
                if(outputStream!=null)
                    outputStream.close();
            }
            catch(IOException e)
            {
                Log.d(TAG,"Error closing output stream",e);
            }
        }
    }

    void createDemoFile()
    {
        Cloud cloud=getCloud();
        if(cloud==Cloud.None) {
            deleteAllFiles();
            //File destinationSongFile = new File(mDemoFolder, DEMO_SONG_FILENAME);
            //copyAssetsFileToDemoFolder(DEMO_SONG_FILENAME, destinationSongFile);
            File destinationSongFile=createDemoSongFile();
            if(destinationSongFile!=null) {
                File destinationAudioFile = new File(mDemoFolder, DEMO_SONG_AUDIO_FILENAME);
                copyAssetsFileToDemoFolder(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile);
                AudioFile audioFile = new AudioFile(DEMO_SONG_AUDIO_FILENAME, destinationAudioFile, DEMO_SONG_AUDIO_FILENAME, new Date());
                ArrayList<AudioFile> audioFiles = new ArrayList<>();
                audioFiles.add(audioFile);
                try {
                    mSongs.add(new SongFile(this, new DownloadedFile(destinationSongFile, DEMO_SONG_FILENAME, new Date(),""), audioFiles,null));
                    mAudioFiles.add(audioFile);
                } catch (IOException ioe) {
                    Toast.makeText(this, ioe.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
        try
        {
            writeDatabase();
        }
        catch(Exception e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    void logOutOfOneDrive()
    {
        if(mOneDriveClient!=null) {
            mOneDriveClient.getAuthenticator().logout(new ICallback<Void>() {
                @Override
                public void success(final Void result) {
                    mOneDriveClient=null;
                }

                @Override
                public void failure(final ClientException ex) {
                    Toast.makeText(getBaseContext(), "Logout error: " + ex, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    void powerwash()
    {
        deleteAllFiles();
        mDropboxAPI=null;
        logOutOfOneDrive();
        createDemoFile();
        initialiseList();
        Toast.makeText(this, getString(R.string.powerwashed), Toast.LENGTH_LONG).show();
    }

    static String getCloudAsString(Context context,Cloud cloud)
    {
        if(cloud==Cloud.Dropbox)
            return context.getString(R.string.dropboxValue);
        else if(cloud==Cloud.GoogleDrive)
            return context.getString(R.string.googleDriveValue);
        else if(cloud==Cloud.OneDrive)
            return context.getString(R.string.oneDriveValue);
        return "";
    }

    Cloud getCloud()
    {
        return getCloud(this);
    }

    static Cloud getCloud(Activity activity)
    {
        Cloud cloud=Cloud.None;
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(activity);
        String cloudPref=sharedPrefs.getString(activity.getString(R.string.pref_cloudStorageSystem_key),null);
        if(cloudPref!=null)
        {
            if(cloudPref.equals(activity.getString(R.string.googleDriveValue)))
                cloud= Cloud.GoogleDrive;
            else if(cloudPref.equals(activity.getString(R.string.dropboxValue)))
                cloud= Cloud.Dropbox;
            else if(cloudPref.equals(activity.getString(R.string.oneDriveValue)))
                cloud= Cloud.OneDrive;
        }
        else
        {
            SharedPreferences privatePrefs = activity.getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID,Context.MODE_PRIVATE);
            String privateCloudPref=privatePrefs.getString(activity.getString(R.string.pref_songSource_key),"");
            if(privateCloudPref.equals(activity.getString(R.string.googleDriveValue)))
                cloud= Cloud.GoogleDrive;
            else if(privateCloudPref.equals(activity.getString(R.string.dropboxValue)))
                cloud= Cloud.Dropbox;
            else if(privateCloudPref.equals(activity.getString(R.string.oneDriveValue)))
                cloud= Cloud.OneDrive;
            if(cloud!=Cloud.None)
                sharedPrefs.edit().putString(activity.getString(R.string.pref_cloudStorageSystem_key),getCloudAsString(activity,cloud)).apply();
        }
        return cloud;
    }

    String getCloudPath()
    {
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getString(getString(R.string.pref_cloudPath_key),null);
    }

    boolean getIncludeSubfolders()
    {
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getBoolean(getString(R.string.pref_includeSubfolders_key),false);
    }

    void synchronizeFiles() {
        Cloud c = getCloud();
        if (c == Cloud.None)
            Toast.makeText(this,getString(R.string.no_cloud_storage_system_set),Toast.LENGTH_LONG).show();
        else {
            String path = getCloudPath();
            if ((path == null) || (path.length() == 0))
                Toast.makeText(this, getString(R.string.no_cloud_folder_currently_set), Toast.LENGTH_LONG).show();
            else
            {
                mSongToRefresh=null;
                mSetToRefresh=null;
                mMIDIAliasCachedFileToRefresh=null;

                if(c==Cloud.Dropbox)
                    fetchFilesFromDropbox();
                else if(c==Cloud.GoogleDrive)
                    fetchFilesFromGoogleDrive();
                else
                    fetchFilesFromOneDrive();
            }
        }
    }

    boolean canSynchronizeFiles()
    {
        String path=getCloudPath();
        return getCloud()!=Cloud.None && path!=null;
    }

    void updateBluetoothIcon()
    {
        BeatPrompterApplication app=((BeatPrompterApplication)SongList.this.getApplicationContext());
        boolean slave=app.getBluetoothMode()==BluetoothMode.client;
        boolean connectedToServer=app.isConnectedToServer();
        boolean master=app.getBluetoothMode()==BluetoothMode.server;
        int connectedClients=app.getBluetoothClientCount();
        int resourceID=slave?(connectedToServer?R.drawable.duncecap:R.drawable.duncecap_outline):R.drawable.blank_icon;
        if(master)
            switch(connectedClients)
            {
                case 0:
                    resourceID=R.drawable.master0;
                    break;
                case 1:
                    resourceID=R.drawable.master1;
                    break;
                case 2:
                    resourceID=R.drawable.master2;
                    break;
                case 3:
                    resourceID=R.drawable.master3;
                    break;
                case 4:
                    resourceID=R.drawable.master4;
                    break;
                case 5:
                    resourceID=R.drawable.master5;
                    break;
                case 6:
                    resourceID=R.drawable.master6;
                    break;
                case 7:
                    resourceID=R.drawable.master7;
                    break;
                case 8:
                    resourceID=R.drawable.master8;
                    break;
                default:
                    resourceID=R.drawable.master9plus;
                    break;
            }
        if(mMenu!=null)
        {
            LinearLayout btlayout = (LinearLayout) mMenu.findItem(R.id.btconnectionstatuslayout).getActionView();
            ImageView btIcon = (ImageView) btlayout.findViewById(R.id.btconnectionstatus);
            if (btIcon != null)
                btIcon.setImageResource(resourceID);
        }
    }
}
