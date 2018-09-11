package com.stevenfrew.beatprompter.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.vending.billing.IInAppBillingService
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.cache.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cloud.*
import com.stevenfrew.beatprompter.filter.*
import com.stevenfrew.beatprompter.filter.Filter
import com.stevenfrew.beatprompter.comm.midi.MIDIController
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.ui.pref.FontSizePreference
import com.stevenfrew.beatprompter.ui.pref.SettingsActivity
import com.stevenfrew.beatprompter.ui.pref.SortingPreference
import com.stevenfrew.beatprompter.set.Playlist
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.songload.SongChoiceInfo
import com.stevenfrew.beatprompter.songload.SongLoadTask
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.flattenAll
import org.json.JSONException
import org.json.JSONObject
import org.xml.sax.SAXException
import java.io.*
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SongListActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private var mMenu: Menu? = null
    private var mSelectedFilter: Filter=AllSongsFilter(mutableListOf())
    private var mSortingPreference = SortingPreference.Title
    private var mPlaylist = Playlist()
    private var mNowPlayingNode: PlaylistNode? = null
    private var mFilters = listOf<Filter>()
    private var mListAdapter: BaseAdapter? = null

    private var mPerformingCloudSync = false
    private var mSavedListIndex = 0
    private var mSavedListOffset = 0

    internal var mIAPService: IInAppBillingService? = null

    private val mInAppPurchaseServiceConn: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mIAPService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mIAPService = IInAppBillingService.Stub.asInterface(service)
            fullVersionUnlocked()
        }
    }

    private var sortingPreference: SortingPreference
        get() {
            return try {
                SortingPreference.valueOf(BeatPrompterApplication.preferences.getString("pref_sorting", SortingPreference.Title.name)!!)
            } catch (ignored: Exception) {
                SortingPreference.Title
            }
        }
        set(pref) {
            BeatPrompterApplication.preferences.edit().putString("pref_sorting", pref.name).apply()
            mSortingPreference = pref
            sortSongList()
        }

    private val isFirstRun: Boolean
        get() { return BeatPrompterApplication.preferences.getBoolean(getString(R.string.pref_firstRun_key), true) }

    private val cloudPath: String?
        get() { return BeatPrompterApplication.preferences.getString(getString(R.string.pref_cloudPath_key), null) }

    private val includeSubFolders: Boolean
        get() { return BeatPrompterApplication.preferences.getBoolean(getString(R.string.pref_includeSubfolders_key), false) }


    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (mSelectedFilter is MIDIAliasFilesFilter) {
            val maf = mCachedCloudFiles.midiAliasFiles[position]
            if (maf.mErrors.isNotEmpty())
                showMIDIAliasErrors(maf.mErrors)
        } else if (!SongLoadTask.songCurrentlyLoading())
            // Don't allow another song to be started from the song list (by clicking)
            // if one is already loading. The only circumstances this is allowed is via
            // MIDI triggers.
            playPlaylistNode(mPlaylist.getNodeAt(position), false)
    }

    internal fun startSongActivity() {
        val i = Intent(applicationContext, SongDisplayActivity::class.java)
        startActivityForResult(i, PLAY_SONG_REQUEST_CODE)
    }

    internal fun startSongViaMidiProgramChange(bankMSB: Byte, bankLSB: Byte, program: Byte, channel: Byte) {
        startSongViaMidiSongTrigger(SongTrigger(bankMSB, bankLSB, program, channel, TriggerType.ProgramChange))
    }

    internal fun startSongViaMidiSongSelect(song: Byte) {
        startSongViaMidiSongTrigger(SongTrigger(0.toByte(), 0.toByte(), song, 0.toByte(), TriggerType.SongSelect))
    }

    private fun startSongViaMidiSongTrigger(mst: SongTrigger) {
        for (node in mPlaylist.nodes)
            if (node.mSongFile.matchesTrigger(mst)) {
                playPlaylistNode(node, true)
                return
            }
        // Otherwise, it might be a song that is not currently onscreen.
        // Still play it though!
        for (sf in mCachedCloudFiles.songFiles)
            if (sf.matchesTrigger(mst))
                playSongFile(sf, null, true)
    }

    private fun playPlaylistNode(node: PlaylistNode?, startedByMidiTrigger: Boolean) {
        val selectedSong = node!!.mSongFile
        playSongFile(selectedSong, node, startedByMidiTrigger)
    }

    private fun playSongFile(selectedSong: SongFile, node: PlaylistNode?, startedByMidiTrigger: Boolean) {
        val manualMode = BeatPrompterApplication.preferences.getBoolean(getString(R.string.pref_manualMode_key), false)
        val track:AudioFile?=if (selectedSong.mAudioFiles.isNotEmpty() && !manualMode && !selectedSong.mMixedMode) mCachedCloudFiles.getMappedAudioFiles(selectedSong.mAudioFiles[0]).firstOrNull() else null
        val bestMode=selectedSong.bestScrollingMode
        val sds = getSongDisplaySettings(bestMode)
        playSong(node, selectedSong, track, bestMode, startedByMidiTrigger, sds, sds)
    }

    private fun shouldPlayNextSong(): Boolean {
        val sharedPrefs = BeatPrompterApplication.preferences
        val playNextSongPref = sharedPrefs.getString(getString(R.string.pref_automaticallyPlayNextSong_key), getString(R.string.pref_automaticallyPlayNextSong_defaultValue))
        var playNextSong = false
        if (playNextSongPref == getString(R.string.playNextSongAlwaysValue))
            playNextSong = true
        else if (playNextSongPref == getString(R.string.playNextSongSetListsOnlyValue))
            playNextSong = mSelectedFilter is SetListFilter
        return playNextSong
    }

    private fun getSongDisplaySettings(songScrollMode: ScrollingMode): DisplaySettings {
        val sharedPref = BeatPrompterApplication.preferences
        val onlyUseBeatFontSizes = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_alwaysUseBeatFontPrefs_key), BeatPrompterApplication.getResourceString(R.string.pref_alwaysUseBeatFontPrefs_defaultValue).toBoolean())

        val fontSizeMin = Integer.parseInt(getString(R.string.fontSizeMin))
        var minimumFontSizeBeat = sharedPref.getInt(getString(R.string.pref_minFontSize_key), Integer.parseInt(getString(R.string.pref_minFontSize_default)))
        minimumFontSizeBeat += fontSizeMin
        var maximumFontSizeBeat = sharedPref.getInt(getString(R.string.pref_maxFontSize_key), Integer.parseInt(getString(R.string.pref_maxFontSize_default)))
        maximumFontSizeBeat += fontSizeMin
        var minimumFontSizeSmooth = sharedPref.getInt(getString(R.string.pref_minFontSizeSmooth_key), Integer.parseInt(getString(R.string.pref_minFontSizeSmooth_default)))
        minimumFontSizeSmooth += fontSizeMin
        var maximumFontSizeSmooth = sharedPref.getInt(getString(R.string.pref_maxFontSizeSmooth_key), Integer.parseInt(getString(R.string.pref_maxFontSizeSmooth_default)))
        maximumFontSizeSmooth += fontSizeMin
        var minimumFontSizeManual = sharedPref.getInt(getString(R.string.pref_minFontSizeManual_key), Integer.parseInt(getString(R.string.pref_minFontSizeManual_default)))
        minimumFontSizeManual += fontSizeMin
        var maximumFontSizeManual = sharedPref.getInt(getString(R.string.pref_maxFontSizeManual_key), Integer.parseInt(getString(R.string.pref_maxFontSizeManual_default)))
        maximumFontSizeManual += fontSizeMin

        if (onlyUseBeatFontSizes) {
            minimumFontSizeManual = minimumFontSizeBeat
            minimumFontSizeSmooth = minimumFontSizeManual
            maximumFontSizeManual = maximumFontSizeBeat
            maximumFontSizeSmooth = maximumFontSizeManual
        }

        val minimumFontSize: Int
        val maximumFontSize: Int
        when {
            songScrollMode === ScrollingMode.Beat -> {
                minimumFontSize = minimumFontSizeBeat
                maximumFontSize = maximumFontSizeBeat
            }
            songScrollMode === ScrollingMode.Smooth -> {
                minimumFontSize = minimumFontSizeSmooth
                maximumFontSize = maximumFontSizeSmooth
            }
            else -> {
                minimumFontSize = minimumFontSizeManual
                maximumFontSize = maximumFontSizeManual
            }
        }

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return DisplaySettings(resources.configuration.orientation, minimumFontSize.toFloat(), maximumFontSize.toFloat(), Rect(0, 0, size.x, size.y), songScrollMode != ScrollingMode.Manual)
    }

    private fun playSong(selectedNode: PlaylistNode?, selectedSong: SongFile, track:AudioFile?, scrollMode: ScrollingMode, startedByMidiTrigger: Boolean, nativeSettings: DisplaySettings, sourceSettings: DisplaySettings) {
        mNowPlayingNode = selectedNode

        var nextSongName = ""
        if (selectedNode?.mNextNode != null && shouldPlayNextSong())
            nextSongName = selectedNode.mNextNode!!.mSongFile.mTitle

        SongLoadTask.loadSong(SongLoadTask(selectedSong, track, scrollMode, nextSongName, false, startedByMidiTrigger, nativeSettings, sourceSettings, mFullVersionUnlocked || cloud === CloudType.Demo))
    }

    private fun clearTemporarySetList() {
        mFilters.filterIsInstance<TemporarySetListFilter>().firstOrNull()?.clear()
        for (slf in mCachedCloudFiles.setListFiles)
            if (slf.mFile == mTemporarySetListFile)
                slf.mSetListEntries.clear()
        initialiseTemporarySetListFile(true)
        buildFilterList()
        try {
            writeDatabase()
        } catch (ioe: Exception) {
            Log.e(BeatPrompterApplication.TAG, ioe.message)
        }
    }

    private fun addToTemporarySet(song: SongFile) {
        mFilters.filterIsInstance<TemporarySetListFilter>().firstOrNull()?.addSong(song)
        try {
            initialiseTemporarySetListFile(false)
            Utils.appendToTextFile(mTemporarySetListFile!!, SetListEntry(song).toString())
        } catch (ioe: IOException) {
            Toast.makeText(this, ioe.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initialiseTemporarySetListFile(deleteExisting: Boolean) {
        try {
            if (deleteExisting)
                if (!mTemporarySetListFile!!.delete())
                    Log.d(BeatPrompterApplication.TAG, "Could not delete temporary set list file.")
            if (!mTemporarySetListFile!!.exists())
                Utils.appendToTextFile(mTemporarySetListFile!!, String.format("{set:%1\$s}", getString(R.string.temporary)))
        } catch (ioe: IOException) {
            Toast.makeText(this, ioe.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onSongListLongClick(position: Int) {
        val selectedNode = mPlaylist.getNodeAt(position)
        val selectedSong = selectedNode.mSongFile
        val selectedSet = if (mSelectedFilter is SetListFileFilter) (mSelectedFilter as SetListFileFilter).mSetListFile else null
        val trackNames = mutableListOf<String>()
        trackNames.add(getString(R.string.no_audio))
        val mappedAudioFiles= mCachedCloudFiles.getMappedAudioFiles(*selectedSong.mAudioFiles.toTypedArray())
        trackNames.addAll(mappedAudioFiles.map{it.mName})
        val tempSetListFilter=mFilters.filterIsInstance<TemporarySetListFilter>().firstOrNull()

        val addAllowed=
            if (tempSetListFilter != null)
                if (mSelectedFilter !== tempSetListFilter)
                    !tempSetListFilter.containsSong(selectedSong)
                else
                    false
            else
                true
        val includeRefreshSet = selectedSet != null && mSelectedFilter !== tempSetListFilter
        val includeClearSet = mSelectedFilter === tempSetListFilter
        val activity = this

        val arrayID: Int
        arrayID = if (includeRefreshSet)
            if (addAllowed)
                R.array.song_options_array_with_refresh_and_add
            else
                R.array.song_options_array_with_refresh
        else if (includeClearSet)
            R.array.song_options_array_with_clear
        else if (addAllowed)
            R.array.song_options_array_with_add
        else
            R.array.song_options_array

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.song_options)
                .setItems(arrayID) { _, which ->
                    when (which) {
                        1 -> performCloudSync(selectedSong, false)
                        2 -> performCloudSync(selectedSong, true)
                        3 -> when {
                            includeRefreshSet -> performCloudSync(selectedSet, false)
                            includeClearSet -> clearTemporarySetList()
                            else -> addToTemporarySet(selectedSong)
                        }
                        4 -> addToTemporarySet(selectedSong)
                        0 -> {
                            val builder1 = AlertDialog.Builder(activity)
                            // Get the layout inflater
                            val inflater = activity.layoutInflater

                            @SuppressLint("InflateParams")
                            val view = inflater.inflate(R.layout.songlist_long_press_dialog, null)

                            val audioSpinner = view
                                    .findViewById<Spinner>(R.id.audioSpinner)
                            val audioSpinnerAdapter = ArrayAdapter(activity,
                                    android.R.layout.simple_spinner_item, trackNames)
                            audioSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            audioSpinner.adapter = audioSpinnerAdapter
                            if (trackNames.size > 1)
                                audioSpinner.setSelection(1)

                            val beatScrollable = selectedSong.isBeatScrollable
                            val smoothScrollable = selectedSong.isSmoothScrollable
                            val beatButton = view
                                    .findViewById<ToggleButton>(R.id.toggleButton_beat)
                            val smoothButton = view
                                    .findViewById<ToggleButton>(R.id.toggleButton_smooth)
                            val manualButton = view
                                    .findViewById<ToggleButton>(R.id.toggleButton_manual)
                            if (!smoothScrollable) {
                                val layout = smoothButton.parent as ViewGroup
                                layout.removeView(smoothButton)
                            }
                            if (!beatScrollable) {
                                val layout = beatButton.parent as ViewGroup
                                layout.removeView(beatButton)
                            }
                            when {
                                beatScrollable -> {
                                    beatButton.isChecked = true
                                    beatButton.isEnabled = false
                                }
                                smoothScrollable -> {
                                    smoothButton.isChecked = true
                                    smoothButton.isEnabled = false
                                }
                                else -> {
                                    manualButton.isChecked = true
                                    manualButton.isEnabled = false
                                }
                            }

                            beatButton.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    smoothButton.isChecked = false
                                    manualButton.isChecked = false
                                    smoothButton.isEnabled = true
                                    manualButton.isEnabled = true
                                    beatButton.isEnabled = false
                                }
                            }

                            smoothButton.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    beatButton.isChecked = false
                                    manualButton.isChecked = false
                                    beatButton.isEnabled = true
                                    manualButton.isEnabled = true
                                    smoothButton.isEnabled = false
                                }
                            }

                            manualButton.setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    beatButton.isChecked = false
                                    smoothButton.isChecked = false
                                    smoothButton.isEnabled = true
                                    beatButton.isEnabled = true
                                    manualButton.isEnabled = false
                                }
                            }

                            // Inflate and set the layout for the dialog
                            // Pass null as the parent view because its going in the dialog layout
                            builder1.setView(view)
                                    // Add action buttons
                                    .setPositiveButton(R.string.play) { _, _ ->
                                        // sign in the user ...
                                        var selectedTrackName = audioSpinner.selectedItem as String?
                                        val mode = if (beatButton.isChecked) ScrollingMode.Beat else if (smoothButton.isChecked) ScrollingMode.Smooth else ScrollingMode.Manual
                                        if (audioSpinner.selectedItemPosition == 0)
                                            selectedTrackName = null
                                        val sds = getSongDisplaySettings(mode)
                                        val track=if(selectedTrackName!=null) mCachedCloudFiles.getMappedAudioFiles(selectedTrackName).firstOrNull() else null
                                        playSong(selectedNode, selectedSong, track, mode, false, sds, sds)
                                    }
                                    .setNegativeButton(R.string.cancel) { _, _ -> }
                            val customAD = builder1.create()
                            customAD.setCanceledOnTouchOutside(true)
                            customAD.show()
                        }
                    }
                }
        val al = builder.create()
        al.setCanceledOnTouchOutside(true)
        al.show()
    }

    private fun onMIDIAliasListLongClick(position: Int) {
        val maf = removeDefaultAliasFile(mCachedCloudFiles.midiAliasFiles)[position]
        val showErrors = maf.mErrors.isNotEmpty()
        val arrayID = if(showErrors) R.array.midi_alias_options_array_with_show_errors else R.array.midi_alias_options_array

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.midi_alias_list_options)
                .setItems(arrayID) { _, which ->
                    if (which == 0)
                        performCloudSync(maf, false)
                    else if (which == 1)
                        showMIDIAliasErrors(maf.mErrors)
                }
        val al = builder.create()
        al.setCanceledOnTouchOutside(true)
        al.show()
    }

    private fun showMIDIAliasErrors(errors: List<FileParseError>) {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        @SuppressLint("InflateParams")
        val view = inflater.inflate(R.layout.parse_errors_dialog, null)
        builder.setView(view)
        val tv = view.findViewById<TextView>(R.id.errors)
        val str = StringBuilder()
        for (fpe in errors)
            str.append(fpe.toString()).append("\n")
        tv.text = str.toString().trim()
        val customAD = builder.create()
        customAD.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
        ) { dialog, _ -> dialog.dismiss() }
        customAD.setTitle(getString(R.string.midi_alias_file_errors))
        customAD.setCanceledOnTouchOutside(true)
        customAD.show()
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        if (mSelectedFilter is MIDIAliasFilesFilter)
            onMIDIAliasListLongClick(position)
        else
            onSongListLongClick(position)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSongListEventHandler = SongListEventHandler(this)
        mSongListInstance = this
        initialiseLocalStorage()

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.GET_ACCOUNTS),
                    MY_PERMISSIONS_REQUEST_GET_ACCOUNTS)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_STORAGE)

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE)

        BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.setPackage("com.android.vending")
        bindService(serviceIntent, mInAppPurchaseServiceConn, Context.BIND_AUTO_CREATE)

        // Set font stuff first.
        val metrics = resources.displayMetrics
        Utils.FONT_SCALING = metrics.density
        Utils.MAXIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMax))
        Utils.MINIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMin))
        FontSizePreference.FONT_SIZE_MAX = Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE
        FontSizePreference.FONT_SIZE_MIN = 0
        FontSizePreference.FONT_SIZE_OFFSET = Utils.MINIMUM_FONT_SIZE

        setContentView(R.layout.activity_song_list)

        if (isFirstRun)
            showFirstRunMessages()

        initialiseList()

        // Now ready to receive events.
        EventHandler.setSongListEventHandler(mSongListEventHandler!!)
    }

    private fun initialiseList() {
        try {
            mSortingPreference = sortingPreference
            readDatabase()
            sortSongList()
            buildList()
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, e.message)
        }
    }

    private fun initialiseLocalStorage() {
        val previousSongFilesFolder = mBeatPrompterSongFilesFolder

        val s = packageName
        try {
            val m = packageManager
            val p = m.getPackageInfo(s, 0)
            mBeatPrompterDataFolder = File(p.applicationInfo.dataDir)
        } catch (e: PackageManager.NameNotFoundException) {
            // There is no way that this can happen.
            Log.e(BeatPrompterApplication.TAG, "Package name not found ", e)
        }

        val sharedPrefs = BeatPrompterApplication.preferences
        val songFilesFolder: String
        val useExternalStorage = sharedPrefs.getBoolean(getString(R.string.pref_useExternalStorage_key), false)
        val externalFilesDir = getExternalFilesDir(null)
        songFilesFolder = if (useExternalStorage && externalFilesDir != null)
            externalFilesDir.absolutePath
        else
            mBeatPrompterDataFolder!!.absolutePath

        mBeatPrompterSongFilesFolder = if (songFilesFolder.isEmpty()) mBeatPrompterDataFolder else File(songFilesFolder)
        if (!mBeatPrompterSongFilesFolder!!.exists())
            if (!mBeatPrompterSongFilesFolder!!.mkdir())
                Log.e(BeatPrompterApplication.TAG, "Failed to create song files folder.")

        if (!mBeatPrompterSongFilesFolder!!.exists())
            mBeatPrompterSongFilesFolder = mBeatPrompterDataFolder

        mTemporarySetListFile = File(mBeatPrompterDataFolder, TEMPORARY_SETLIST_FILENAME)
        mDefaultMidiAliasesFile = File(mBeatPrompterDataFolder, DEFAULT_MIDI_ALIASES_FILENAME)
        initialiseTemporarySetListFile(false)
        try {
            copyAssetsFileToLocalFolder(DEFAULT_MIDI_ALIASES_FILENAME, mDefaultMidiAliasesFile!!)
        } catch (ioe: IOException) {
            Toast.makeText(this, ioe.message, Toast.LENGTH_LONG).show()
        }

        mDefaultCloudDownloads.clear()
        mDefaultCloudDownloads.add(SuccessfulCloudDownloadResult(CloudFileInfo("idBeatPrompterTemporarySetList", "BeatPrompterTemporarySetList", Date(), ""), mTemporarySetListFile!!))
        mDefaultCloudDownloads.add(SuccessfulCloudDownloadResult(CloudFileInfo("idBeatPrompterDefaultMidiAliases", getString(R.string.default_alias_set_name), Date(), ""), mDefaultMidiAliasesFile!!))

        if (previousSongFilesFolder != null)
            if (previousSongFilesFolder != mBeatPrompterSongFilesFolder)
            // Song file storage folder has changed. We need to clear the cache.
                clearCache(false)
    }

    public override fun onDestroy() {
        BeatPrompterApplication.preferences.unregisterOnSharedPreferenceChangeListener(this)
        EventHandler.setSongListEventHandler(null)
        super.onDestroy()

        unbindService(mInAppPurchaseServiceConn)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        var listView = findViewById<ListView>(R.id.listView)
        val currentPosition = listView.firstVisiblePosition
        val v = listView.getChildAt(0)
        val top = if (v == null) 0 else v.top - listView.paddingTop

        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_song_list)
        buildList()

        listView = findViewById(R.id.listView)
        listView.setSelectionFromTop(currentPosition, top)
    }

    override fun onResume() {
        super.onResume()

        updateBluetoothIcon()

        if (mListAdapter != null)
            mListAdapter!!.notifyDataSetChanged()

        // First run? Install demo files.
        if (isFirstRun)
            firstRunSetup()
        else
            SongLoadTask.onResume()
    }

    private fun firstRunSetup() {
        val sharedPrefs = BeatPrompterApplication.preferences
        val editor = sharedPrefs.edit()
        editor.putBoolean(getString(R.string.pref_firstRun_key), false)
        editor.putString(getString(R.string.pref_cloudStorageSystem_key), "demo")
        editor.putString(getString(R.string.pref_cloudPath_key), "/")
        editor.apply()
        performFullCloudSync()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                GOOGLE_PLAY_TRANSACTION_FINISHED -> {
                    val purchaseData = data!!.getStringExtra("INAPP_PURCHASE_DATA")
                    try {
                        val jo = JSONObject(purchaseData)
                        val sku = jo.getString("productId")
                        mFullVersionUnlocked = mFullVersionUnlocked || sku.equals(FULL_VERSION_SKU_NAME, ignoreCase = true)
                        Toast.makeText(this@SongListActivity, getString(R.string.thankyou), Toast.LENGTH_LONG).show()
                    } catch (e: JSONException) {
                        Log.e(BeatPrompterApplication.TAG, "JSON exception during purchase.")
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                PLAY_SONG_REQUEST_CODE -> startNextSong()
            }
    }

    private fun startNextSong() {
        mSongEndedNaturally = false
        if (mNowPlayingNode != null && mNowPlayingNode!!.mNextNode != null && shouldPlayNextSong())
            playPlaylistNode(mNowPlayingNode!!.mNextNode, false)
        else
            mNowPlayingNode = null
    }

    private fun canPerformCloudSync(): Boolean {
        return cloud !== CloudType.Demo && cloudPath != null
    }

    private fun performFullCloudSync() {
        performCloudSync(null, false)
    }

    private fun performCloudSync(fileToUpdate: CachedCloudFile?, dependenciesToo: Boolean) {
        if(fileToUpdate==null)
            clearTemporarySetList()
        val cs = CloudStorage.getInstance(cloud, this)
        val cloudPath = cloudPath
        if (cloudPath.isNullOrBlank())
            Toast.makeText(this, getString(R.string.no_cloud_folder_currently_set), Toast.LENGTH_LONG).show()
        else {
            mPerformingCloudSync = true
            val cdt = CloudDownloadTask(cs, mSongListEventHandler!!, cloudPath!!, includeSubFolders, mCachedCloudFiles.getFilesToRefresh(fileToUpdate, dependenciesToo))
            cdt.execute()
        }
    }

    private fun sortSongList() {
        if (mSelectedFilter.mCanSort)
            when (mSortingPreference) {
                SortingPreference.Date -> sortSongsByDateModified()
                SortingPreference.Artist -> sortSongsByArtist()
                SortingPreference.Title -> sortSongsByTitle()
                SortingPreference.Key -> sortSongsByKey()
            }
    }

    private fun sortSongsByTitle() {
        mPlaylist.sortByTitle()
    }

    private fun sortSongsByArtist() {
        mPlaylist.sortByArtist()
    }

    private fun sortSongsByDateModified() {
        mPlaylist.sortByDateModified()
    }

    private fun sortSongsByKey() {
        mPlaylist.sortByKey()
    }

    private fun buildList() {
        mListAdapter = if (mSelectedFilter is MIDIAliasFilesFilter)
            MIDIAliasListAdapter(removeDefaultAliasFile(mCachedCloudFiles.midiAliasFiles))
        else
            SongListAdapter(mPlaylist.nodes)

        val listView = findViewById<ListView>(R.id.listView)
        listView.onItemClickListener = this
        listView.onItemLongClickListener = this
        listView.adapter = mListAdapter
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    private fun readDatabase() {
        val bpdb = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
        if (bpdb.exists()) {
            mPlaylist = Playlist()

            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val xmlDoc = docBuilder.parse(bpdb)

            mCachedCloudFiles.readFromXML(xmlDoc)
            buildFilterList()
        }
    }

    @Throws(ParserConfigurationException::class, TransformerException::class)
    private fun writeDatabase() {
        val bpdb = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
        if (!bpdb.delete())
            Log.e(BeatPrompterApplication.TAG, "Failed to delete database file.")
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val d = docBuilder.newDocument()
        val root = d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG)
        d.appendChild(root)
        mCachedCloudFiles.writeToXML(d, root)
        val transformer = TransformerFactory.newInstance().newTransformer()
        val output = StreamResult(bpdb)
        val input = DOMSource(d)
        transformer.transform(input, output)
    }

    private fun buildFilterList() {
        Log.d(BeatPrompterApplication.TAG, "Building taglist ...")
        val tagAndFolderFilters = mutableListOf<Filter>()

        // Create filters from song tags and sub-folders. Many songs can share the same
        // tag/subfolder, so a bit of clever collection management is required here.
        val tagDictionaries = HashMap<String, MutableList<SongFile>>()
        val folderDictionaries = HashMap<String, MutableList<SongFile>>()
        for (song in mCachedCloudFiles.songFiles) {
            song.mTags.forEach{ tagDictionaries.getOrPut(it) {mutableListOf()}.add(song) }
            if (!song.mSubfolder.isNullOrBlank())
                folderDictionaries.getOrPut(song.mSubfolder!!) {mutableListOf()}.add(song)
        }
        tagDictionaries.forEach{
            tagAndFolderFilters.add(TagFilter(it.key, it.value))
        }
        folderDictionaries.forEach {
            tagAndFolderFilters.add(FolderFilter(it.key, it.value))
        }
        tagAndFolderFilters.addAll(mCachedCloudFiles.setListFiles.mapNotNull {
            if (it.mFile != mTemporarySetListFile)
                SetListFileFilter(it, mCachedCloudFiles.songFiles.toMutableList())
            null
        })
        tagAndFolderFilters.sortBy{it.mName.toLowerCase()}

        // Now create the basic "all songs" filter, dead easy ...
        val allSongsFilter = AllSongsFilter(mCachedCloudFiles.songFiles.filter { !it.mFilterOnly }.toMutableList())

        // Depending on whether we have a temporary set list file, we can create a temporary
        // set list filter ...
        val tempSetListFile= mCachedCloudFiles.setListFiles.firstOrNull {it.mFile== mTemporarySetListFile }
        val tempSetListFilter=
            if(tempSetListFile!=null)
                TemporarySetListFilter(tempSetListFile, mCachedCloudFiles.songFiles.toMutableList())
            else
                null

        // Same thing for MIDI alias files ... there's always at least ONE (default aliases), but
        // if there aren't any more, don't bother creating a filter.
        val midiAliasFilesFilter=
            if(mCachedCloudFiles.midiAliasFiles.size>1)
                MIDIAliasFilesFilter(getString(R.string.midi_alias_files))
            else
                null

        // Now bundle them altogether into one list.
        mFilters=listOf(allSongsFilter,tempSetListFilter,tagAndFolderFilters,midiAliasFilesFilter).flattenAll().filterIsInstance<Filter>()

        // The default selected filter should be "all songs".
        mSelectedFilter = allSongsFilter
        applyFileFilter(mSelectedFilter)
        invalidateOptionsMenu()
    }

    fun fullVersionUnlocked(): Boolean {
        if (!mFullVersionUnlocked)
            try {
                if (mIAPService != null) {
                    val ownedItems = mIAPService!!.getPurchases(3, packageName, "inapp", null)
                    val response = ownedItems.getInt("RESPONSE_CODE")
                    if (response == 0) {
                        val ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST")
                        if (ownedSkus != null)
                            for (sku in ownedSkus)
                                mFullVersionUnlocked = mFullVersionUnlocked or sku.equals(FULL_VERSION_SKU_NAME, ignoreCase = true)
                    }
                }
            } catch (e: Exception) {
                Log.e(BeatPrompterApplication.TAG, "Failed to check for purchased version.", e)
            }
        return mFullVersionUnlocked
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        var item: MenuItem? = menu.findItem(R.id.sort_songs)

        if (item != null) {
            item.isEnabled = mSelectedFilter.mCanSort
        }
        if (fullVersionUnlocked()) {
            item = menu.findItem(R.id.buy_full_version)
            if (item != null)
                item.isVisible = false
        }
        item = menu.findItem(R.id.synchronize)
        if (item != null)
            item.isEnabled = canPerformCloudSync()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu
        menuInflater.inflate(R.menu.songlistmenu, menu)
        val spinner = menu.findItem(R.id.tagspinner).actionView as Spinner
        spinner.onItemSelectedListener = this
        val filterListAdapter = FilterListAdapter(mFilters)
        spinner.adapter = filterListAdapter

        updateBluetoothIcon()
        return true
    }

    private fun showSortDialog()
    {
        if (mSelectedFilter.mCanSort) {
            val adb = AlertDialog.Builder(this)
            val items = arrayOf<CharSequence>(getString(R.string.byTitle), getString(R.string.byArtist), getString(R.string.byDate), getString(R.string.byKey))
            adb.setItems(items) { d, n ->
                d.dismiss()
                when (n) {
                    0 -> sortingPreference = SortingPreference.Title
                    1 -> sortingPreference = SortingPreference.Artist
                    2 -> sortingPreference = SortingPreference.Date
                    3 -> sortingPreference = SortingPreference.Key
                }
                buildList()
            }
            adb.setTitle(getString(R.string.sortSongs))
            val ad = adb.create()
            ad.setCanceledOnTouchOutside(true)
            ad.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.synchronize -> performFullCloudSync()
            R.id.sort_songs -> showSortDialog()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.buy_full_version -> buyFullVersion()
            R.id.about -> showAboutDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        applyFileFilter(mFilters[position])
        if (mPerformingCloudSync) {
            mPerformingCloudSync = false
            val listView = findViewById<ListView>(R.id.listView)
            listView.setSelectionFromTop(mSavedListIndex, mSavedListOffset)
        }
    }

    private fun buyFullVersion() {
        try {
            val buyIntentBundle = mIAPService!!.getBuyIntent(3, packageName,
                    FULL_VERSION_SKU_NAME, "inapp", "")
            val response = buyIntentBundle.getInt("RESPONSE_CODE")
            if (response == 0) {
                val pendingIntent = buyIntentBundle.getParcelable<PendingIntent>("BUY_INTENT")
                if (pendingIntent != null)
                    startIntentSenderForResult(pendingIntent.intentSender,
                            GOOGLE_PLAY_TRANSACTION_FINISHED, Intent(), 0, 0, 0)
            }
        } catch (e: Exception) {
            Log.e(BeatPrompterApplication.TAG, "Failed to buy full version.", e)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        //applyFileFilter(null)
    }

    private fun applyFileFilter(filter: Filter) {
        mSelectedFilter = filter
        mPlaylist = if (filter is SongFilter)
            Playlist(filter.mSongs)
        else
            Playlist()
        sortSongList()
        buildList()
        showSetListMissingSongs()
    }

    private fun showSetListMissingSongs() {
        if (mSelectedFilter is SetListFileFilter) {
            val slf = mSelectedFilter as SetListFileFilter
            val missing = slf.mMissingSetListEntries.take(3)
            if (missing.isNotEmpty() && !slf.mWarned) {
                slf.mWarned = true
                val message = StringBuilder(getString(R.string.missing_songs_message, missing.size))
                message.append("\n\n")
                missing.forEach {
                    message.append(it.toDisplayString())
                    message.append("\n")
                }
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle(R.string.missing_songs_dialog_title)
                alertDialog.setMessage(message.toString())
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.show()
            }
        }
    }

    private fun showAboutDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        @SuppressLint("InflateParams")
        val view = inflater.inflate(R.layout.about_dialog, null)
        builder.setView(view)
        val customAD = builder.create()
        customAD.setCanceledOnTouchOutside(true)
        customAD.show()
    }

    internal fun clearCache(report: Boolean) {
        // Clear both cache folders
        val cs = CloudStorage.getInstance(cloud, this)
        cs.cacheFolder.clear()
        mPlaylist = Playlist()
        mCachedCloudFiles.clear()
        buildFilterList()
        try {
            writeDatabase()
        } catch (ioe: Exception) {
            Log.e(BeatPrompterApplication.TAG, ioe.message)
        }

        if (report)
            Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_LONG).show()
    }

    fun processBluetoothChooseSongMessage(choiceInfo: SongChoiceInfo) {
        val beat = choiceInfo.mBeatScroll
        val smooth = choiceInfo.mSmoothScroll
        val scrollingMode = if (beat) ScrollingMode.Beat else if (smooth) ScrollingMode.Smooth else ScrollingMode.Manual

        val sharedPrefs = BeatPrompterApplication.preferences
        val prefName = getString(R.string.pref_mimicBandLeaderDisplay_key)
        val mimicDisplay = scrollingMode === ScrollingMode.Manual && sharedPrefs.getBoolean(prefName, true)

        // Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
        // Also, beat and smooth scrolling should never mimic.
        val nativeSettings = getSongDisplaySettings(scrollingMode)
        val sourceSettings = if (mimicDisplay) DisplaySettings(choiceInfo) else nativeSettings

        for (sf in mCachedCloudFiles.songFiles)
            if (sf.mNormalizedTitle == choiceInfo.mNormalizedTitle && sf.mNormalizedArtist==choiceInfo.mNormalizedArtist) {
                val loadTask = SongLoadTask(sf, mCachedCloudFiles.getMappedAudioFiles(choiceInfo.mTrack).firstOrNull(), scrollingMode, "", true,
                        false, nativeSettings, sourceSettings, mFullVersionUnlocked || cloud === CloudType.Demo)
                SongDisplayActivity.interruptCurrentSong(loadTask, sf)
                break
            }
    }

    private fun showFirstRunMessages() {
        //  Declare a new thread to do a preference check
        val t = Thread {
            val i = Intent(applicationContext, IntroActivity::class.java)
            startActivity(i)
        }

        // Start the thread
        t.start()
    }

    internal fun updateBluetoothIcon() {
        val slave = BluetoothManager.bluetoothMode === BluetoothMode.Client
        val connectedToServer = BluetoothManager.isConnectedToServer
        val master = BluetoothManager.bluetoothMode === BluetoothMode.Server
        val connectedClients = BluetoothManager.bluetoothClientCount
        var resourceID = if (slave) if (connectedToServer) R.drawable.duncecap else R.drawable.duncecap_outline else R.drawable.blank_icon
        if (master)
            resourceID = when (connectedClients) {
                0 -> R.drawable.master0
                1 -> R.drawable.master1
                2 -> R.drawable.master2
                3 -> R.drawable.master3
                4 -> R.drawable.master4
                5 -> R.drawable.master5
                6 -> R.drawable.master6
                7 -> R.drawable.master7
                8 -> R.drawable.master8
                else -> R.drawable.master9plus
            }
        if (mMenu != null) {
            val btlayout = mMenu!!.findItem(R.id.btconnectionstatuslayout).actionView as LinearLayout
            val btIcon = btlayout.findViewById<ImageView>(R.id.btconnectionstatus)
            btIcon?.setImageResource(resourceID)
        }
    }

    internal fun onCacheUpdated(cache: CachedCloudFileCollection) {
        val listView = findViewById<ListView>(R.id.listView)
        mSavedListIndex = listView.firstVisiblePosition
        val v = listView.getChildAt(0)
        mSavedListOffset = if (v == null) 0 else v.top - listView.paddingTop

        mCachedCloudFiles = cache
        try {
            writeDatabase()
            buildFilterList()
        } catch (ioe: Exception) {
            Log.e(BeatPrompterApplication.TAG, ioe.message)
        }
    }

    class SongListEventHandler internal constructor(private val mSongList: SongListActivity) : EventHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BLUETOOTH_CHOOSE_SONG -> mSongList.processBluetoothChooseSongMessage(msg.obj as SongChoiceInfo)
                CLIENT_CONNECTED -> {
                    Toast.makeText(mSongList, msg.obj.toString() + " " + BeatPrompterApplication.getResourceString(R.string.hasConnected), Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                CLIENT_DISCONNECTED -> {
                    Toast.makeText(mSongList, msg.obj.toString() + " " + BeatPrompterApplication.getResourceString(R.string.hasDisconnected), Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                SERVER_DISCONNECTED -> {
                    Toast.makeText(mSongList, BeatPrompterApplication.getResourceString(R.string.disconnectedFromBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                SERVER_CONNECTED -> {
                    Toast.makeText(mSongList, BeatPrompterApplication.getResourceString(R.string.connectedToBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                CLOUD_SYNC_ERROR -> {
                    val adb = AlertDialog.Builder(mSongList)
                    adb.setMessage(BeatPrompterApplication.getResourceString(R.string.cloudSyncErrorMessage, msg.obj as String))
                    adb.setTitle(BeatPrompterApplication.getResourceString(R.string.cloudSyncErrorTitle))
                    adb.setPositiveButton("OK") { dialog, _ -> dialog.cancel() }
                    val ad = adb.create()
                    ad.setCanceledOnTouchOutside(true)
                    ad.show()
                }
                SONG_LOAD_FAILED -> Toast.makeText(mSongList, msg.obj.toString(), Toast.LENGTH_LONG).show()
                MIDI_PROGRAM_CHANGE -> mSongList.startSongViaMidiProgramChange(MIDIController.mMidiBankMSBs[msg.arg1], MIDIController.mMidiBankLSBs[msg.arg1], msg.arg2.toByte(), msg.arg1.toByte())
                MIDI_SONG_SELECT -> mSongList.startSongViaMidiSongSelect(msg.arg1.toByte())
                SONG_LOAD_COMPLETED -> mSongList.startSongActivity()
                CLEAR_CACHE -> mSongList.clearCache(true)
                CACHE_UPDATED -> {
                    val cache = msg.obj as CachedCloudFileCollection
                    mSongList.onCacheUpdated(cache)
                }
            }
        }
    }

    companion object {
        var mDefaultCloudDownloads: MutableList<CloudDownloadResult> = mutableListOf()
        var mCachedCloudFiles = CachedCloudFileCollection()

        private var mBeatPrompterDataFolder: File?=null
        var mBeatPrompterSongFilesFolder: File?=null

        var mSongEndedNaturally = false

        private var mFullVersionUnlocked = true

        private const val XML_DATABASE_FILE_NAME = "bpdb.xml"
        private const val XML_DATABASE_FILE_ROOT_ELEMENT_TAG = "beatprompterDatabase"
        private const val TEMPORARY_SETLIST_FILENAME = "temporary_setlist.txt"
        private const val DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt"

        lateinit var mSongListInstance: SongListActivity

        private const val PLAY_SONG_REQUEST_CODE = 3
        private const val GOOGLE_PLAY_TRANSACTION_FINISHED = 4
        private const val MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 5
        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 6
        private const val MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 7

        private const val FULL_VERSION_SKU_NAME = "full_version"

        // Fake cloud items for temporary set list and default midi aliases
        var mTemporarySetListFile: File?=null
        var mDefaultMidiAliasesFile: File?=null

        var mSongListEventHandler: SongListEventHandler?=null

        private fun removeDefaultAliasFile(fileList: List<MIDIAliasFile>): List<MIDIAliasFile> {
            val nonDefaults = mutableListOf<MIDIAliasFile>()
            for (file in fileList)
                if (file.mFile != mDefaultMidiAliasesFile)
                    nonDefaults.add(file)
            return nonDefaults
        }

        val cloud: CloudType
            get() {
                val sharedPrefs = BeatPrompterApplication.preferences
                val cloudPref = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_cloudStorageSystem_key), null)
                return when (cloudPref) {
                    BeatPrompterApplication.getResourceString(R.string.localStorageValue) -> CloudType.Local
                    BeatPrompterApplication.getResourceString(R.string.googleDriveValue) -> CloudType.GoogleDrive
                    BeatPrompterApplication.getResourceString(R.string.dropboxValue) -> CloudType.Dropbox
                    BeatPrompterApplication.getResourceString(R.string.oneDriveValue) -> CloudType.OneDrive
                    else -> CloudType.Demo
                }
            }

        @Throws(IOException::class)
        fun copyAssetsFileToLocalFolder(filename: String, destination: File) {
            val inputStream = BeatPrompterApplication.assetManager.open(filename)
            inputStream.use { inStream ->
                val outputStream = FileOutputStream(destination)
                outputStream.use{
                    Utils.streamToStream(inStream, it)
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_storageLocation_key) || key == getString(R.string.pref_useExternalStorage_key))
            initialiseLocalStorage()
        else if (key == getString(R.string.pref_largePrintList_key))
            buildList()
    }
}
