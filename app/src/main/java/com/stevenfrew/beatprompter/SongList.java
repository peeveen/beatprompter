package com.stevenfrew.beatprompter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.vending.billing.IInAppBillingService;
import com.stevenfrew.beatprompter.bluetooth.BluetoothManager;
import com.stevenfrew.beatprompter.bluetooth.BluetoothMessage;
import com.stevenfrew.beatprompter.bluetooth.BluetoothMode;
import com.stevenfrew.beatprompter.bluetooth.ChooseSongMessage;
import com.stevenfrew.beatprompter.cache.AudioFile;
import com.stevenfrew.beatprompter.cache.CachedCloudFile;
import com.stevenfrew.beatprompter.cache.CachedCloudFileCollection;
import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.cache.ImageFile;
import com.stevenfrew.beatprompter.cache.SetListFile;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudDownloadTask;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;
import com.stevenfrew.beatprompter.filter.AllSongsFilter;
import com.stevenfrew.beatprompter.filter.Filter;
import com.stevenfrew.beatprompter.filter.FolderFilter;
import com.stevenfrew.beatprompter.filter.SetListFileFilter;
import com.stevenfrew.beatprompter.filter.SetListFilter;
import com.stevenfrew.beatprompter.filter.SongFilter;
import com.stevenfrew.beatprompter.filter.TagFilter;
import com.stevenfrew.beatprompter.filter.TemporarySetListFilter;
import com.stevenfrew.beatprompter.midi.Alias;
import com.stevenfrew.beatprompter.cache.MIDIAliasFile;
import com.stevenfrew.beatprompter.filter.MIDIAliasFilesFilter;
import com.stevenfrew.beatprompter.ui.MIDIAliasListAdapter;
import com.stevenfrew.beatprompter.midi.InTask;
import com.stevenfrew.beatprompter.midi.SongDisplayInTask;
import com.stevenfrew.beatprompter.midi.SongTrigger;
import com.stevenfrew.beatprompter.midi.USBInTask;
import com.stevenfrew.beatprompter.midi.USBOutTask;
import com.stevenfrew.beatprompter.pref.FontSizePreference;
import com.stevenfrew.beatprompter.pref.SettingsActivity;
import com.stevenfrew.beatprompter.pref.SortingPreference;
import com.stevenfrew.beatprompter.ui.FilterListAdapter;
import com.stevenfrew.beatprompter.ui.SongListAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class SongList extends AppCompatActivity implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    public static List<CloudDownloadResult> mDefaultCloudDownloads=new ArrayList<>();
    static boolean mFullVersionUnlocked = true;
    public static CachedCloudFileCollection mCachedCloudFiles = new CachedCloudFileCollection();

    CachedCloudFile mFileToUpdate = null;
    boolean mFetchDependenciesToo = false;

    private static final String TEMPORARY_SETLIST_FILENAME = "temporary_setlist.txt";
    private static final String DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt";

    private boolean mMidiUsbRegistered = false;
    boolean mSongListActive = false;
    public static boolean mSongEndedNaturally = false;
    Menu mMenu = null;
    public static SongList mSongListInstance = null;
    Filter mSelectedFilter = null;
    SortingPreference mSortingPreference = SortingPreference.TITLE;
    Playlist mPlaylist = new Playlist();
    PlaylistNode mNowPlayingNode = null;
    ArrayList<Filter> mFilters = new ArrayList<>();
    TemporarySetListFilter mTemporarySetListFilter = null;
    BaseAdapter mListAdapter = null;

    public static File mBeatPrompterDataFolder;
    public static File mBeatPrompterSongFilesFolder;

    private static final String XML_DATABASE_FILE_NAME="bpdb.xml";
    private static final String XML_DATABASE_FILE_ROOT_ELEMENT_TAG="beatprompterDatabase";

    private static final int PLAY_SONG_REQUEST_CODE=3;
    private static final int GOOGLE_PLAY_TRANSACTION_FINISHED=4;
    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS=4;

    SharedPreferences.OnSharedPreferenceChangeListener mStorageLocationPrefListener = (sharedPreferences, key) -> {
        if((key.equals(getString(R.string.pref_storageLocation_key)))||(key.equals(getString(R.string.pref_useExternalStorage_key))))
            initialiseLocalStorage();
        else if(key.equals(getString(R.string.pref_largePrintList_key)))
            buildList();
        else if(key.equals(getString(R.string.pref_midiIncomingChannels_key)))
            setIncomingMIDIChannels();
    };

    // Fake cloud items for temporary set list and default midi aliases
    public static File mTemporarySetListFile;
    public static File mDefaultMidiAliasesFile;

    UsbManager mUsbManager;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    public static SongListEventHandler mSongListEventHandler;

    static SongLoaderTask mSongLoaderTask = null;

    USBInTask mMidiUsbInTask = null;
    USBOutTask mMidiUsbOutTask = new USBOutTask();
    InTask mMidiInTask = new InTask();
    SongDisplayInTask mMidiSongDisplayInTask = new SongDisplayInTask();
    Thread mMidiUsbInTaskThread = null;
    Thread mMidiUsbOutTaskThread = new Thread(mMidiUsbOutTask);
    Thread mMidiInTaskThread = new Thread(mMidiInTask);
    Thread mMidiSongDisplayInTaskThread = new Thread(mMidiSongDisplayInTask);
    Thread mSongLoaderTaskThread = null;

    final static private String FULL_VERSION_SKU_NAME = "full_version";

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
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                attemptUsbMidiConnection();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                mMidiUsbOutTask.setConnection(null, null);
                Task.stopTask(mMidiUsbInTask, mMidiUsbInTaskThread);
                mMidiUsbInTask = null;
                mMidiUsbInTaskThread = null;
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbInterface midiInterface = getDeviceMidiInterface(device);
                            if (midiInterface != null) {
                                UsbDeviceConnection conn = mUsbManager.openDevice(device);
                                if (conn != null) {
                                    if (conn.claimInterface(midiInterface, true)) {
                                        int endpointCount = midiInterface.getEndpointCount();
                                        for (int f = 0; f < endpointCount; ++f) {
                                            UsbEndpoint endPoint = midiInterface.getEndpoint(f);
                                            if (endPoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                                mMidiUsbOutTask.setConnection(conn, endPoint);
                                            } else if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                                if (mMidiUsbInTask == null) {
                                                    mMidiUsbInTask = new USBInTask(conn, endPoint, getIncomingMIDIChannelsPref());
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
        if ((mSelectedFilter != null) && (mSelectedFilter instanceof MIDIAliasFilesFilter)) {
            final MIDIAliasFile maf = mCachedCloudFiles.getMIDIAliasFiles().get(position);
            if (maf.mErrors.size() > 0)
                showMIDIAliasErrors(maf.mErrors);
        } else {
            // Don't allow another song to be started from the song list (by clicking)
            // if one is already loading. The only circumstances this is allowed is via
            // MIDI triggers.
            if (!songCurrentlyLoading()) {
                PlaylistNode selectedNode = mPlaylist.getNodeAt(position);
                playPlaylistNode(selectedNode, false);
            }
        }
    }

    boolean songCurrentlyLoading() {
        synchronized (SongLoadTask.mSongLoadSyncObject) {
            return SongLoadTask.mSongLoadTask != null;
        }
    }

    void startSongActivity() {
        Intent i = new Intent(getApplicationContext(), SongDisplayActivity.class);
        i.putExtra("registered", fullVersionUnlocked());
        startActivityForResult(i, PLAY_SONG_REQUEST_CODE);
    }

    void startSongViaMidiProgramChange(byte bankMSB, byte bankLSB, byte program, byte channel) {
        startSongViaMidiSongTrigger(new SongTrigger(bankMSB, bankLSB, program, channel, false));
    }

    void startSongViaMidiSongSelect(byte song) {
        startSongViaMidiSongTrigger(new SongTrigger((byte) 0, (byte) 0, song, (byte) 0, true));
    }

    void startSongViaMidiSongTrigger(SongTrigger mst) {
        for (PlaylistNode node : mPlaylist.getNodesAsArray())
            if (node.mSongFile.matchesTrigger(mst)) {
                playPlaylistNode(node, true);
                return;
            }
        // Otherwise, it might be a song that is not currently onscreen.
        // Still play it though!
        for (SongFile sf : mCachedCloudFiles.getSongFiles())
            if (sf.matchesTrigger(mst)) {
                playSongFile(sf, null, true);
            }
    }

    public void playPlaylistNode(PlaylistNode node, boolean startedByMidiTrigger) {
        SongFile selectedSong = node.mSongFile;
        playSongFile(selectedSong, node, startedByMidiTrigger);
    }

    void playSongFile(SongFile selectedSong, PlaylistNode node, boolean startedByMidiTrigger) {
        String trackName = null;
        if ((selectedSong.mAudioFiles != null) && (selectedSong.mAudioFiles.size() > 0))
            trackName = selectedSong.mAudioFiles.get(0);
        boolean beatScroll = selectedSong.isBeatScrollable();
        boolean smoothScroll = selectedSong.isSmoothScrollable();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean manualMode = sharedPrefs.getBoolean(getString(R.string.pref_manualMode_key), false);
        if (manualMode) {
            beatScroll = smoothScroll = false;
            trackName = null;
        }
        ScrollingMode scrollingMode = beatScroll ? ScrollingMode.Beat : (smoothScroll ? ScrollingMode.Smooth : ScrollingMode.Manual);
        SongDisplaySettings sds = getSongDisplaySettings(scrollingMode);
        playSong(node, selectedSong, trackName, scrollingMode, false, startedByMidiTrigger, sds, sds);
    }

    private boolean shouldPlayNextSong() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String playNextSongPref = sharedPrefs.getString(getString(R.string.pref_automaticallyPlayNextSong_key), getString(R.string.pref_automaticallyPlayNextSong_defaultValue));
        boolean playNextSong = false;
        if (playNextSongPref.equals(getString(R.string.playNextSongAlwaysValue)))
            playNextSong = true;
        else if (playNextSongPref.equals(getString(R.string.playNextSongSetListsOnlyValue)))
            playNextSong = (mSelectedFilter != null) && (mSelectedFilter instanceof SetListFilter);
        return playNextSong;
    }

    SongDisplaySettings getSongDisplaySettings(ScrollingMode scrollMode) {
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
            minimumFontSizeSmooth = minimumFontSizeManual = minimumFontSizeBeat;
            maximumFontSizeSmooth = maximumFontSizeManual = maximumFontSizeBeat;
        }

        int minimumFontSize, maximumFontSize;
        if (scrollMode == ScrollingMode.Beat) {
            minimumFontSize = minimumFontSizeBeat;
            maximumFontSize = maximumFontSizeBeat;
        } else if (scrollMode == ScrollingMode.Smooth) {
            minimumFontSize = minimumFontSizeSmooth;
            maximumFontSize = maximumFontSizeSmooth;
        } else {
            minimumFontSize = minimumFontSizeManual;
            maximumFontSize = maximumFontSizeManual;
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return new SongDisplaySettings(getResources().getConfiguration().orientation, minimumFontSize, maximumFontSize, size.x, size.y);
    }

    private void playSong(PlaylistNode selectedNode, SongFile selectedSong, String trackName, ScrollingMode scrollMode, boolean startedByBandLeader, boolean startedByMidiTrigger, SongDisplaySettings nativeSettings, SongDisplaySettings sourceSettings) {
        mNowPlayingNode = selectedNode;

        String nextSong = null;
        if ((selectedNode != null) && (selectedNode.mNextNode != null) && (shouldPlayNextSong()))
            nextSong = selectedNode.mNextNode.mSongFile.mTitle;

        LoadingSongFile lsf = new LoadingSongFile(selectedSong, trackName, scrollMode, nextSong, startedByBandLeader, startedByMidiTrigger, getCloud() == CloudType.Demo, nativeSettings, sourceSettings);
        startSong(lsf);
    }

    void startSong(LoadingSongFile lsf) {
        synchronized (SongLoadTask.mSongLoadSyncObject) {
            SongLoadTask.mSongLoadTask = new SongLoadTask(lsf);
        }
        SongLoadTask.mSongLoadTask.loadSong();
    }

    void clearTemporarySetList() {
        if(mTemporarySetListFilter!=null)
            mTemporarySetListFilter.clear();
        initialiseTemporarySetListFile(true);
        buildFilterList();
    }

    void addToTemporarySet(SongFile song) {
        mTemporarySetListFilter.addSong(song);
        try {
            initialiseTemporarySetListFile(false);
            Utils.appendToTextFile(mTemporarySetListFile, song.mTitle);
        } catch (IOException ioe) {
            Toast.makeText(this, ioe.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initialiseTemporarySetListFile(boolean deleteExisting) {
        try {
            if (deleteExisting)
                if(!mTemporarySetListFile.delete())
                    Log.d(BeatPrompterApplication.TAG,"Could not delete temporary set list file.");
            if(!mTemporarySetListFile.exists())
                Utils.appendToTextFile(mTemporarySetListFile, String.format("{set:%1$s}", getString(R.string.temporary)));
        } catch (IOException ioe) {
            Toast.makeText(this, ioe.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        final boolean includeRefreshSet=selectedSet!=null && mSelectedFilter!=mTemporarySetListFilter;
        final boolean includeClearSet=mSelectedFilter==mTemporarySetListFilter;
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
                .setItems(arrayID, (dialog, which) -> {
                    if (which == 1)
                        performCloudSync(selectedSong,false);
                    else if (which == 2)
                        performCloudSync(selectedSong,true);
                    else if (which == 3)
                    {
                        if(includeRefreshSet) {
                            performCloudSync(selectedSet,false);
                        }
                        else if(includeClearSet)
                            clearTemporarySetList();
                        else
                            addToTemporarySet(selectedSong);
                    }
                    else if(which==4)
                        addToTemporarySet(selectedSong);
                    else if (which == 0) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                        // Get the layout inflater
                        LayoutInflater inflater = activity.getLayoutInflater();

                        @SuppressLint("InflateParams")
                        View view = inflater.inflate(R.layout.songlist_long_press_dialog, null);

                        final Spinner audioSpinner =  view
                                .findViewById(R.id.audioSpinner);
                        ArrayAdapter<String> audioSpinnerAdapter = new ArrayAdapter<>(activity,
                                android.R.layout.simple_spinner_item, trackNames);
                        audioSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        audioSpinner.setAdapter(audioSpinnerAdapter);
                        if (trackNames.size() > 1)
                            audioSpinner.setSelection(1);

                        boolean beatScrollable=selectedSong.isBeatScrollable();
                        boolean smoothScrollable=selectedSong.isSmoothScrollable();
                        final ToggleButton beatButton =  view
                                .findViewById(R.id.toggleButton_beat);
                        final ToggleButton smoothButton =  view
                                .findViewById(R.id.toggleButton_smooth);
                        final ToggleButton manualButton =  view
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

                        beatButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if(isChecked)
                            {
                                smoothButton.setChecked(false);
                                manualButton.setChecked(false);
                                smoothButton.setEnabled(true);
                                manualButton.setEnabled(true);
                                beatButton.setEnabled(false);
                            }
                        });

                        smoothButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if(isChecked)
                            {
                                beatButton.setChecked(false);
                                manualButton.setChecked(false);
                                beatButton.setEnabled(true);
                                manualButton.setEnabled(true);
                                smoothButton.setEnabled(false);
                            }
                        });

                        manualButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if(isChecked)
                            {
                                beatButton.setChecked(false);
                                smoothButton.setChecked(false);
                                smoothButton.setEnabled(true);
                                beatButton.setEnabled(true);
                                manualButton.setEnabled(false);
                            }
                        });

                        // Inflate and set the layout for the dialog
                        // Pass null as the parent view because its going in the dialog layout
                        builder1.setView(view)
                                // Add action buttons
                                .setPositiveButton(R.string.play, (dialog1, id) -> {
                                    // sign in the user ...
                                    String selectedTrack = (String) (audioSpinner.getSelectedItem());
                                    ScrollingMode mode=beatButton.isChecked()?ScrollingMode.Beat:(smoothButton.isChecked()?ScrollingMode.Smooth:ScrollingMode.Manual);
                                    if (audioSpinner.getSelectedItemPosition() == 0)
                                        selectedTrack = null;
                                    SongDisplaySettings sds=getSongDisplaySettings(mode);
                                    playSong(selectedNode, selectedSong, selectedTrack,mode,false,false,sds,sds);
                                })
                                .setNegativeButton(R.string.cancel, (dialog12, id) -> {
                                });
                        AlertDialog customAD = builder1.create();
                        customAD.setCanceledOnTouchOutside(true);
                        customAD.show();
                    }
                });
        AlertDialog al = builder.create();
        al.setCanceledOnTouchOutside(true);
        al.show();
    }

    void onMIDIAliasListLongClick(int position)
    {
        final MIDIAliasFile maf=removeDefaultAliasFile(mCachedCloudFiles.getMIDIAliasFiles()).get(position);
        final boolean showErrors=maf.mErrors.size()>0;

        int arrayID=R.array.midi_alias_options_array;
        if(showErrors)
            arrayID=R.array.midi_alias_options_array_with_show_errors;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.midi_alias_list_options)
                .setItems(arrayID, (dialog, which) -> {
                    if (which == 0)
                        performCloudSync(maf,false);
                    else if (which == 1)
                        showMIDIAliasErrors(maf.mErrors);
                });
        AlertDialog al = builder.create();
        al.setCanceledOnTouchOutside(true);
        al.show();
    }

    void showMIDIAliasErrors(ArrayList<FileParseError> errors)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.parse_errors_dialog, null);
        builder.setView(view);
        TextView tv=view.findViewById(R.id.errors);
        StringBuilder str= new StringBuilder();
        for(FileParseError fpe:errors)
            str.append(fpe.getErrorMessage()).append("\n");
        tv.setText(str.toString().trim());
        AlertDialog customAD = builder.create();
        customAD.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSongListEventHandler=new SongListEventHandler(this);
        mSongListInstance=this;
        initialiseLocalStorage();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
        }

        EventHandler.setSongListEventHandler(mSongListEventHandler);

        mMidiInTaskThread.start();
        Task.resumeTask(mMidiInTask);
        mMidiUsbOutTaskThread.start();
        Task.resumeTask(mMidiUsbOutTask);

        mSongLoaderTask=new SongLoaderTask();
        mSongLoaderTaskThread=new Thread(mSongLoaderTask);
        mSongLoaderTaskThread.start();
        Task.resumeTask(mSongLoaderTask);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mStorageLocationPrefListener);
        //SharedPreferences sharedPrefs =getPreferences(Context.MODE_PRIVATE);

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

        setContentView(R.layout.activity_song_list);

        if(isFirstRun())
            showFirstRunMessages();

        initialiseList();

        BluetoothManager.startBluetooth();
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
            Log.e(BeatPrompterApplication.TAG,e.getMessage());
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

    void initialiseLocalStorage()
    {
        File previousSongFilesFolder=mBeatPrompterSongFilesFolder;

        String s = getPackageName();
        try {
            PackageManager m=getPackageManager();
            PackageInfo p = m.getPackageInfo(s, 0);
            mBeatPrompterDataFolder = new File(p.applicationInfo.dataDir);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(BeatPrompterApplication.TAG, "Package name not found ", e);
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
                Log.e(BeatPrompterApplication.TAG,"Failed to create song files folder.");

        if(!mBeatPrompterSongFilesFolder.exists())
            mBeatPrompterSongFilesFolder=mBeatPrompterDataFolder;

        mTemporarySetListFile=new File(mBeatPrompterDataFolder,TEMPORARY_SETLIST_FILENAME);
        mDefaultMidiAliasesFile=new File(mBeatPrompterDataFolder,DEFAULT_MIDI_ALIASES_FILENAME);
        initialiseTemporarySetListFile(false);
        try {
            copyAssetsFileToLocalFolder(DEFAULT_MIDI_ALIASES_FILENAME, mDefaultMidiAliasesFile);
        } catch (IOException ioe) {
            Toast.makeText(this, ioe.getMessage(), Toast.LENGTH_LONG).show();
        }
        mDefaultCloudDownloads.clear();
        mDefaultCloudDownloads.add(new CloudDownloadResult(new CloudFileInfo("idBeatPrompterTemporarySetList","BeatPrompterTemporarySetList",new Date(),""),mTemporarySetListFile));
        mDefaultCloudDownloads.add(new CloudDownloadResult(new CloudFileInfo("idBeatPrompterDefaultMidiAliases",getString(R.string.default_alias_set_name),new Date(),""),mDefaultMidiAliasesFile));

        if(previousSongFilesFolder!=null)
            if(!previousSongFilesFolder.equals(mBeatPrompterSongFilesFolder))
                // Song file storage folder has changed. We need to clear the cache.
                deleteAllFiles();
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
        EventHandler.setSongListEventHandler(null);
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
            for (AudioFile afm : mCachedCloudFiles.getAudioFiles()) {
                String secondChance = in.replace('’', '\'');
                if ((afm.mName.equalsIgnoreCase(in)) || (afm.mName.equalsIgnoreCase(secondChance)))
                    return afm;
            }
            if(tempAudioFileCollection!=null)
                for (AudioFile afm : tempAudioFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((afm.mName.equalsIgnoreCase(in)) || (afm.mName.equalsIgnoreCase(secondChance)))
                        return afm;
                }
        }
        return null;
    }

    public static ImageFile getMappedImageFilename(String in,ArrayList<ImageFile> tempImageFileCollection)
    {
        if(in!=null) {
            for (ImageFile ifm : mCachedCloudFiles.getImageFiles()) {
                String secondChance = in.replace('’', '\'');
                if ((ifm.mName.equalsIgnoreCase(in)) || (ifm.mName.equalsIgnoreCase(secondChance)))
                    return ifm;
            }
            if(tempImageFileCollection!=null)
                for (ImageFile ifm : tempImageFileCollection) {
                    String secondChance = in.replace('’', '\'');
                    if ((ifm.mName.equalsIgnoreCase(in)) || (ifm.mName.equalsIgnoreCase(secondChance)))
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

        if(mListAdapter!=null)
            mListAdapter.notifyDataSetChanged();

        // First run? Install demo files.
        if(isFirstRun())
            firstRunSetup();
        else if(SongLoadTask.mSongToLoadOnResume!=null)
        {
            try
            {
                startSong(SongLoadTask.mSongToLoadOnResume);
            }
            finally
            {
                SongLoadTask.mSongToLoadOnResume=null;
            }
        }
    }

    private void firstRunSetup()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor=sharedPrefs.edit();
        editor.putBoolean(getString(R.string.pref_firstRun_key), false);
        editor.putString(getString(R.string.pref_cloudStorageSystem_key), "demo");
        editor.putString(getString(R.string.pref_cloudPath_key), "/");
        editor.apply();
        performFullCloudSync();
    }

    @Override
    protected void onPause() {
        mSongListActive=false;
        Task.pauseTask(mMidiSongDisplayInTask,mMidiSongDisplayInTaskThread);
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
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
                        Log.e(BeatPrompterApplication.TAG,"JSON exception during purchase.");
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case PLAY_SONG_REQUEST_CODE:
                if(resultCode==RESULT_OK)
                    startNextSong();
                break;
        }
    }

    private void startNextSong()
    {
        mSongEndedNaturally=false;
        if((mNowPlayingNode!=null)&&(mNowPlayingNode.mNextNode!=null)&&(shouldPlayNextSong()))
            playPlaylistNode(mNowPlayingNode.mNextNode,false);
        else
            mNowPlayingNode=null;
    }

    private ArrayList<CachedCloudFile> getFilesToRefresh()
    {
        ArrayList<CachedCloudFile> filesToRefresh=new ArrayList<>();
        if(mFileToUpdate!=null)
        {
            filesToRefresh.add(mFileToUpdate);
            if((mFileToUpdate instanceof SongFile) && (mFetchDependenciesToo))
            {
                SongFile song=(SongFile)mFileToUpdate;
                if (song.mAudioFiles != null)
                    for (String audioFileName : song.mAudioFiles) {
                        AudioFile audioFile = getMappedAudioFilename(audioFileName, null);
                        File actualAudioFile = null;
                        if (audioFile != null)
                            actualAudioFile = new File(song.mFile.getParent(), audioFile.mFile.getName());
                        if ((actualAudioFile != null) && (actualAudioFile.exists()))
                            filesToRefresh.add(audioFile);
                    }
                if (song.mImageFiles != null)
                    for (String imageFileName : song.mImageFiles) {
                        ImageFile imageFile = getMappedImageFilename(imageFileName, null);
                        File actualImageFile = null;
                        if (imageFile != null)
                            actualImageFile = new File(song.mFile.getParent(), imageFile.mFile.getName());
                        if ((actualImageFile != null) && (actualImageFile.exists()))
                            filesToRefresh.add(imageFile);
                    }
            }
        }
        return filesToRefresh;
    }

    void performCloudSync(CachedCloudFile fileToUpdate,boolean dependenciesToo)
    {
        mFileToUpdate = fileToUpdate;
        mFetchDependenciesToo = dependenciesToo;
        performCloudSync();
    }

    void performCloudSync()
    {
        CloudStorage cs=CloudStorage.getInstance(getCloud(),this);
        String cloudPath = getCloudPath();
        if ((cloudPath == null) || (cloudPath.length() == 0))
            Toast.makeText(this, getString(R.string.no_cloud_folder_currently_set), Toast.LENGTH_LONG).show();
        else {
            CloudDownloadTask cdt = new CloudDownloadTask(cs, mSongListEventHandler, cloudPath, getIncludeSubfolders(), getFilesToRefresh());
            cdt.execute();
        }
    }

    public boolean wasPowerwashed()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SongList.mSongListInstance);
        boolean powerwashed = sharedPrefs.getBoolean(getString(R.string.pref_wasPowerwashed_key), false);
        sharedPrefs.edit().putBoolean(getString(R.string.pref_wasPowerwashed_key), false).apply();
        return powerwashed;
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

    private static List<MIDIAliasFile> removeDefaultAliasFile(List<MIDIAliasFile> fileList)
    {
        List<MIDIAliasFile> nonDefaults=new ArrayList<>();
        for(MIDIAliasFile file:fileList)
            if(!file.mFile.equals(SongList.mDefaultMidiAliasesFile))
                nonDefaults.add(file);
        return nonDefaults;
    }

    private void buildList()
    {
        if((mSelectedFilter!=null)&&(mSelectedFilter instanceof MIDIAliasFilesFilter))
            mListAdapter=new MIDIAliasListAdapter(removeDefaultAliasFile(mCachedCloudFiles.getMIDIAliasFiles()));
        else
            mListAdapter = new SongListAdapter(mPlaylist.getNodesAsArray());

        ListView listView = findViewById(R.id.listView);

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
            mPlaylist=new Playlist();

            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document xmlDoc = docBuilder.parse(bpdb);

            mCachedCloudFiles.readFromXML(xmlDoc);
            buildFilterList();
        }
    }

    private void writeDatabase() throws ParserConfigurationException, TransformerException
    {
        File bpdb=new File(mBeatPrompterDataFolder,XML_DATABASE_FILE_NAME);
        if(!bpdb.delete())
            Log.e(BeatPrompterApplication.TAG,"Failed to delete database file.");
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document d=docBuilder.newDocument();
        Element root=d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG);
        d.appendChild(root);
        mCachedCloudFiles.writeToXML(d,root);
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
        Log.d(BeatPrompterApplication.TAG,"Building taglist ...");
        mFilters=new ArrayList<>();
        Filter mOldSelectedFilter=mSelectedFilter;
        mSelectedFilter=null;
        Map<String,ArrayList<SongFile>> tagDicts=new HashMap<>();
        Map<String,ArrayList<SongFile>> folderDicts=new HashMap<>();
        for(SongFile song: mCachedCloudFiles.getSongFiles())
        {
            for(String tag: song.mTags)
            {
                ArrayList<SongFile> songs = tagDicts.computeIfAbsent(tag, k -> new ArrayList<>());
                songs.add(song);
            }
            if(song.mSubfolder!=null && song.mSubfolder.length()>0)
            {
                ArrayList<SongFile> songs = folderDicts.computeIfAbsent(song.mSubfolder, k -> new ArrayList<>());
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

        for(SetListFile slf:mCachedCloudFiles.getSetListFiles())
        {
            SetListFileFilter filter;
            if(slf.mFile.equals(mTemporarySetListFile))
                filter=mTemporarySetListFilter=new TemporarySetListFilter(slf,mCachedCloudFiles.getSongFiles());
            else
                filter=new SetListFileFilter(slf,mCachedCloudFiles.getSongFiles());
            mFilters.add(filter);
        }

        mFilters.sort((f1, f2) -> {
            String tag1 = f1.mName.toLowerCase();
            String tag2 = f2.mName.toLowerCase();
            return tag1.compareTo(tag2);
        });

//        if(mTemporarySetListFilter!=null)
//            mFilters.add(0, mTemporarySetListFilter);

        Filter allSongsFilter=new AllSongsFilter(getString(R.string.no_tag_selected),mCachedCloudFiles.getSongFiles());
        mFilters.add(0, allSongsFilter);

        if(!mCachedCloudFiles.getMIDIAliasFiles().isEmpty())
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
            Log.e(BeatPrompterApplication.TAG,"Failed to check for purchased version.",e);
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
        item.setEnabled(canPerformCloudSync());
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
        FilterListAdapter filterListAdapter = new FilterListAdapter( mFilters);
        spinner.setAdapter(filterListAdapter);

        updateBluetoothIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.synchronize:
                performFullCloudSync();
                return true;
            case R.id.sort_songs:
                if(mSelectedFilter.mCanSort) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    CharSequence items[] = new CharSequence[]{getString(R.string.byTitle), getString(R.string.byArtist), getString(R.string.byDate),getString(R.string.byKey)};
                    adb.setItems(items, (d, n) -> {
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
                            GOOGLE_PLAY_TRANSACTION_FINISHED, new Intent(), 0,0,0);
            }
        }
        catch(Exception e)
        {
            Log.e(BeatPrompterApplication.TAG,"Failed to buy full version.",e);
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
            List<String> missing=slf.mMissingSongs;
            if((missing.size()>0)&&(!slf.mWarned))
            {
                slf.mWarned=true;
                StringBuilder message=new StringBuilder(getString(R.string.missing_songs_message,missing.size()));
                message.append("\n\n");
                for(int f=0;f<Math.min(missing.size(),3);++f)
                {
                    message.append(missing.get(f));
                    message.append("\n");
                }
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(R.string.missing_songs_dialog_title);
                alertDialog.setMessage(message.toString());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        }
    }

    private void showAboutDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        @SuppressLint("InflateParams")
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

    public void deleteAllFiles()
    {
        // Clear both cache folders
        setLastSyncDate(new Date(0));
        CloudStorage cs=CloudStorage.getInstance(getCloud(),this);
        cs.getCacheFolder().clear();
        mPlaylist=new Playlist();
        mCachedCloudFiles.clear();
        buildFilterList();
        try {
            writeDatabase();
//            buildList();
        }
        catch(Exception ioe)
        {
            Log.e(BeatPrompterApplication.TAG,ioe.getMessage());
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
            boolean mimicDisplay=(scrollingMode==ScrollingMode.Manual && sharedPrefs.getBoolean(prefName, true));

            // Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
            // Also, beat and smooth scrolling should never mimic.
            SongDisplaySettings nativeSettings=getSongDisplaySettings(scrollingMode);
            SongDisplaySettings sourceSettings=mimicDisplay?new SongDisplaySettings(csm):nativeSettings;

            for (SongFile sf : mCachedCloudFiles.getSongFiles())
                if (sf.mTitle.equals(title))
                {
                    if(mSongListActive)
                        playSong(null, sf, track, scrollingMode,true,false,nativeSettings,sourceSettings);
                    else
                        SongLoadTask.mSongToLoadOnResume=new LoadingSongFile(sf,track,scrollingMode,null,true,
                                false,getCloud()==CloudType.Demo,nativeSettings,sourceSettings);
                    break;
                }
        }
    }

    static boolean isFullVersionUnlocked()
    {
        return mFullVersionUnlocked;
    }

    static ArrayList<Alias> getMIDIAliases()
    {
        ArrayList<Alias> aliases=new ArrayList<>();
        for(MIDIAliasFile maf:mCachedCloudFiles.getMIDIAliasFiles())
            aliases.addAll(maf.mAliasSet.mAliases);
        return aliases;
    }

    boolean isFirstRun()
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mSongListInstance);
        return sharedPrefs.getBoolean(getString(R.string.pref_firstRun_key), true);
    }

    void showFirstRunMessages()
    {
        //  Declare a new thread to do a preference check
        Thread t = new Thread(() -> {
            Intent i = new Intent(getApplicationContext(), IntroActivity.class);
            startActivity(i);
        });

        // Start the thread
        t.start();
    }

    public static CloudType getCloud()
    {
        CloudType cloud=CloudType.Demo;
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(SongList.mSongListInstance);
        String cloudPref=sharedPrefs.getString(mSongListInstance.getString(R.string.pref_cloudStorageSystem_key),null);
        if(cloudPref!=null)
        {
            if(cloudPref.equals(mSongListInstance.getString(R.string.googleDriveValue)))
                cloud= CloudType.GoogleDrive;
            else if(cloudPref.equals(mSongListInstance.getString(R.string.dropboxValue)))
                cloud= CloudType.Dropbox;
            else if(cloudPref.equals(mSongListInstance.getString(R.string.oneDriveValue)))
                cloud= CloudType.OneDrive;
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

    void performFullCloudSync() {
        performCloudSync(null,false);
    }

    boolean canPerformCloudSync()
    {
        return getCloud()!=CloudType.Demo && getCloudPath()!=null;
    }

    void updateBluetoothIcon()
    {
        boolean slave= BluetoothManager.getBluetoothMode()== BluetoothMode.Client;
        boolean connectedToServer=BluetoothManager.isConnectedToServer();
        boolean master=BluetoothManager.getBluetoothMode()==BluetoothMode.Server;
        int connectedClients=BluetoothManager.getBluetoothClientCount();
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
            ImageView btIcon = btlayout.findViewById(R.id.btconnectionstatus);
            if (btIcon != null)
                btIcon.setImageResource(resourceID);
        }
    }

    int getIncomingMIDIChannelsPref() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SongList.this);
        return sharedPrefs.getInt(getString(R.string.pref_midiIncomingChannels_key), 65535);
    }

    void setIncomingMIDIChannels()
    {
        if(mMidiUsbInTask!=null)
            mMidiUsbInTask.setIncomingChannels(getIncomingMIDIChannelsPref());
    }

    void onCacheUpdated(CachedCloudFileCollection cache)
    {
        mCachedCloudFiles=cache;
        try
        {
            writeDatabase();
            buildFilterList();
        }
        catch(Exception ioe)
        {
            Log.e(BeatPrompterApplication.TAG,ioe.getMessage());
        }
    }

    public static void copyAssetsFileToLocalFolder(String filename,File destination) throws IOException
    {
        InputStream inputStream=null;
        OutputStream outputStream=null;
        try {
            inputStream = SongList.mSongListInstance.getAssets().open(filename);
            if (inputStream != null) {
                outputStream = new FileOutputStream(destination);
                Utils.streamToStream(inputStream,outputStream);
            }
        }
        finally
        {
            try {
                if(inputStream!=null)
                    inputStream.close();
            }
            finally
            {
                if(outputStream!=null)
                    outputStream.close();
            }
        }
    }

    static class SongListEventHandler extends EventHandler {
        private SongList mSongList;
        SongListEventHandler(SongList songList)
        {
            mSongList=songList;
        }
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BLUETOOTH_MESSAGE_RECEIVED:
                    mSongList.processBluetoothMessage((BluetoothMessage) msg.obj);
                    break;
                case CLIENT_CONNECTED:
                    Toast.makeText(mSongList, msg.obj + " " + SongList.mSongListInstance.getString(R.string.hasConnected), Toast.LENGTH_LONG).show();
                    mSongList.updateBluetoothIcon();
                    break;
                case CLIENT_DISCONNECTED:
                    Toast.makeText(mSongList, msg.obj + " " + SongList.mSongListInstance.getString(R.string.hasDisconnected), Toast.LENGTH_LONG).show();
                    mSongList.updateBluetoothIcon();
                    break;
                case SERVER_DISCONNECTED:
                    Toast.makeText(mSongList, SongList.mSongListInstance.getString(R.string.disconnectedFromBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show();
                    mSongList.updateBluetoothIcon();
                    break;
                case SERVER_CONNECTED:
                    Toast.makeText(mSongList, SongList.mSongListInstance.getString(R.string.connectedToBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show();
                    mSongList.updateBluetoothIcon();
                    break;
                case CLOUD_SYNC_ERROR:
                    AlertDialog.Builder adb = new AlertDialog.Builder(mSongList);
                    adb.setMessage(SongList.mSongListInstance.getString(R.string.cloudSyncErrorMessage, (String) msg.obj));
                    adb.setTitle(SongList.mSongListInstance.getString(R.string.cloudSyncErrorTitle));
                    adb.setPositiveButton("OK", (dialog, id) -> dialog.cancel());
                    AlertDialog ad = adb.create();
                    ad.setCanceledOnTouchOutside(true);
                    ad.show();
                    break;
                case SONG_LOAD_FAILED:
                    Toast.makeText(mSongList, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;
                case MIDI_LSB_BANK_SELECT:
                    BeatPrompterApplication.mMidiBankLSBs[msg.arg1] = (byte) msg.arg2;
                    break;
                case MIDI_MSB_BANK_SELECT:
                    BeatPrompterApplication.mMidiBankMSBs[msg.arg1] = (byte) msg.arg2;
                    break;
                case MIDI_PROGRAM_CHANGE:
                    mSongList.startSongViaMidiProgramChange(BeatPrompterApplication.mMidiBankMSBs[msg.arg1], BeatPrompterApplication.mMidiBankLSBs[msg.arg1], (byte) msg.arg2, (byte) msg.arg1);
                    break;
                case MIDI_SONG_SELECT:
                    mSongList.startSongViaMidiSongSelect((byte) msg.arg1);
                    break;
                case SONG_LOAD_COMPLETED:
                    mSongList.startSongActivity();
                    break;
                case CLEAR_CACHE:
                    mSongList.clearCache();
                    break;
                case CACHE_UPDATED:
                    CachedCloudFileCollection cache = (CachedCloudFileCollection) msg.obj;
                    mSongList.onCacheUpdated(cache);
                    break;
            }
        }

    }
}
