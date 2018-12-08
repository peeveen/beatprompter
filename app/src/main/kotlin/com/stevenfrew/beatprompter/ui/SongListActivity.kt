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
import android.net.Uri
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.vending.billing.IInAppBillingService
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothManager
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.set.Playlist
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.load.*
import com.stevenfrew.beatprompter.storage.*
import com.stevenfrew.beatprompter.ui.filter.*
import com.stevenfrew.beatprompter.ui.filter.Filter
import com.stevenfrew.beatprompter.ui.pref.FontSizePreference
import com.stevenfrew.beatprompter.ui.pref.SettingsActivity
import com.stevenfrew.beatprompter.ui.pref.SortingPreference
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.flattenAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.xml.sax.SAXException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.coroutines.CoroutineContext

class SongListActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SharedPreferences.OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, CoroutineScope {
    private val mCoRoutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + mCoRoutineJob
    private var mMenu: Menu? = null
    private var mSelectedFilter: Filter = AllSongsFilter(mutableListOf())
    private var mPlaylist = Playlist()
    private var mNowPlayingNode: PlaylistNode? = null
    private var mFilters = listOf<Filter>()
    private var mListAdapter: BaseAdapter? = null
    private var mSearchText = ""
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

    private val isFirstRun: Boolean
        get() {
            return Preferences.firstRun
        }

    private val cloudPath: String?
        get() {
            return Preferences.cloudPath
        }

    private val includeSubFolders: Boolean
        get() {
            return Preferences.includeSubFolders
        }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (mSelectedFilter is MIDIAliasFilesFilter) {
            val maf = filterMIDIAliasFiles(mCachedCloudFiles.midiAliasFiles)[position]
            if (maf.mErrors.isNotEmpty())
                showMIDIAliasErrors(maf.mErrors)
        } else {
            val songToLoad = filterPlaylistNodes(mPlaylist)[position]
            if (!SongLoadQueueWatcherTask.isAlreadyLoadingSong(songToLoad.mSongFile))
                playPlaylistNode(songToLoad, false)
        }
    }

    internal fun startSongActivity(loadID: UUID) {
        val i = Intent(applicationContext, SongDisplayActivity::class.java)
        i.putExtra("loadID", ParcelUuid(loadID))
        Logger.logLoader { "Starting SongDisplayActivity for $loadID!" }
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
                Logger.log { "Found trigger match: '${node.mSongFile.mTitle}'." }
                playPlaylistNode(node, true)
                return
            }
        // Otherwise, it might be a song that is not currently onscreen.
        // Still play it though!
        for (sf in mCachedCloudFiles.songFiles)
            if (sf.matchesTrigger(mst)) {
                Logger.log { "Found trigger match: '${sf.mTitle}'." }
                playSongFile(sf, PlaylistNode(sf), true)
            }
    }

    private fun playPlaylistNode(node: PlaylistNode, startedByMidiTrigger: Boolean) {
        val selectedSong = node.mSongFile
        playSongFile(selectedSong, node, startedByMidiTrigger)
    }

    private fun playSongFile(selectedSong: SongFile, node: PlaylistNode, startedByMidiTrigger: Boolean) {
        val manualMode = Preferences.manualMode
        val track: AudioFile? = if (selectedSong.mAudioFiles.isNotEmpty() && !manualMode && !selectedSong.mMixedMode) mCachedCloudFiles.getMappedAudioFiles(selectedSong.mAudioFiles[0]).firstOrNull() else null
        val mode = if (manualMode) ScrollingMode.Manual else selectedSong.bestScrollingMode
        val sds = getSongDisplaySettings(mode)
        playSong(node, track, mode, startedByMidiTrigger, sds, sds, manualMode || (track == null && !selectedSong.mMixedMode))
    }

    private fun shouldPlayNextSong(): Boolean {
        val playNextSongPref = Preferences.playNextSong
        return when (playNextSongPref) {
            getString(R.string.playNextSongAlwaysValue) -> true
            getString(R.string.playNextSongSetListsOnlyValue) -> mSelectedFilter is SetListFilter
            else -> false
        }
    }

    private fun getSongDisplaySettings(songScrollMode: ScrollingMode): DisplaySettings {
        val onlyUseBeatFontSizes = Preferences.onlyUseBeatFontSizes

        val minimumFontSizeBeat = Preferences.minimumBeatFontSize
        val maximumFontSizeBeat = Preferences.maximumBeatFontSize
        val minimumFontSizeSmooth = if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.minimumSmoothFontSize
        val maximumFontSizeSmooth = if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.maximumSmoothFontSize
        val minimumFontSizeManual = if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.minimumManualFontSize
        val maximumFontSizeManual = if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.maximumManualFontSize

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

    private fun playSong(selectedNode: PlaylistNode, track: AudioFile?, scrollMode: ScrollingMode, startedByMidiTrigger: Boolean, nativeSettings: DisplaySettings, sourceSettings: DisplaySettings, noAudio: Boolean) {
        showLoadingProgressUI(true)
        mNowPlayingNode = selectedNode

        val nextSongName = if (selectedNode.mNextNode != null && shouldPlayNextSong()) selectedNode.mNextNode!!.mSongFile.mTitle else ""
        val songLoadInfo = SongLoadInfo(selectedNode.mSongFile, track, scrollMode, nextSongName, false, startedByMidiTrigger, nativeSettings, sourceSettings, noAudio)
        val songLoadJob = SongLoadJob(songLoadInfo, mFullVersionUnlocked || Preferences.storageSystem === StorageType.Demo)
        SongLoadQueueWatcherTask.loadSong(songLoadJob)
    }

    private fun clearTemporarySetList() {
        mFilters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.clear()
        for (slf in mCachedCloudFiles.setListFiles)
            if (slf.mFile == mTemporarySetListFile)
                slf.mSetListEntries.clear()
        initialiseTemporarySetListFile(true)
        buildFilterList()
        try {
            writeDatabase()
        } catch (ioe: Exception) {
            Logger.log(ioe)
        }
    }

    private fun addToTemporarySet(song: SongFile) {
        mFilters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.addSong(song)
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
                    Logger.log("Could not delete temporary set list file.")
            if (!mTemporarySetListFile!!.exists())
                Utils.appendToTextFile(mTemporarySetListFile!!, String.format("{set:%1\$s}", getString(R.string.temporary)))
        } catch (ioe: IOException) {
            Toast.makeText(this, ioe.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onSongListLongClick(position: Int) {
        val selectedNode = filterPlaylistNodes(mPlaylist)[position]
        val selectedSong = selectedNode.mSongFile
        val selectedSet = if (mSelectedFilter is SetListFileFilter) (mSelectedFilter as SetListFileFilter).mSetListFile else null
        val trackNames = mutableListOf<String>()
        trackNames.add(getString(R.string.no_audio))
        val mappedAudioFiles = mCachedCloudFiles.getMappedAudioFiles(*selectedSong.mAudioFiles.toTypedArray())
        trackNames.addAll(mappedAudioFiles.map { it.mName })
        val tempSetListFilter = mFilters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()

        val addAllowed =
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
                                        val selectedTrackName = if (audioSpinner.selectedItemPosition == 0) null else audioSpinner.selectedItem as String?
                                        val mode = if (beatButton.isChecked) ScrollingMode.Beat else if (smoothButton.isChecked) ScrollingMode.Smooth else ScrollingMode.Manual
                                        val sds = getSongDisplaySettings(mode)
                                        val track = if (selectedTrackName != null) mCachedCloudFiles.getMappedAudioFiles(selectedTrackName).firstOrNull() else null
                                        playSong(selectedNode, track, mode, false, sds, sds, selectedTrackName == null)
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
        val maf = filterMIDIAliasFiles(mCachedCloudFiles.midiAliasFiles)[position]
        val showErrors = maf.mErrors.isNotEmpty()
        val arrayID = if (showErrors) R.array.midi_alias_options_array_with_show_errors else R.array.midi_alias_options_array

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

    private fun checkPermissions(permissions: List<String>) {
        permissions.filter {
            ContextCompat.checkSelfPermission(this,
                    it) != PackageManager.PERMISSION_GRANTED
        }.let { missingPermissions ->
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toTypedArray(),
                    MY_PERMISSIONS_REQUEST)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSongListInstance = this

        mSongListEventHandler = SongListEventHandler(this)
        // Now ready to receive events.
        EventHandler.setSongListEventHandler(mSongListEventHandler!!)

        initialiseLocalStorage()

        checkPermissions(listOf(Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE))

        Preferences.registerOnSharedPreferenceChangeListener(this)

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

        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_HOME
        supportActionBar?.setIcon(R.drawable.ic_beatprompter)
        supportActionBar?.title = ""
    }

    private fun initialiseList() {
        try {
            readDatabase()
            sortSongList()
            buildList()
        } catch (e: Exception) {
            Logger.log(e)
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
            Logger.log("Package name not found ", e)
        }

        val songFilesFolder: String
        val useExternalStorage = Preferences.useExternalStorage
        val externalFilesDir = getExternalFilesDir(null)
        songFilesFolder = if (useExternalStorage && externalFilesDir != null)
            externalFilesDir.absolutePath
        else
            mBeatPrompterDataFolder!!.absolutePath

        mBeatPrompterSongFilesFolder = if (songFilesFolder.isEmpty()) mBeatPrompterDataFolder else File(songFilesFolder)
        if (!mBeatPrompterSongFilesFolder!!.exists())
            if (!mBeatPrompterSongFilesFolder!!.mkdir())
                Logger.log("Failed to create song files folder.")

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

        mDefaultDownloads.clear()
        mDefaultDownloads.add(SuccessfulDownloadResult(FileInfo("idBeatPrompterTemporarySetList", "BeatPrompterTemporarySetList", Date(), ""), mTemporarySetListFile!!))
        mDefaultDownloads.add(SuccessfulDownloadResult(FileInfo("idBeatPrompterDefaultMidiAliases", getString(R.string.default_alias_set_name), Date(), ""), mDefaultMidiAliasesFile!!))

        if (previousSongFilesFolder != null)
            if (previousSongFilesFolder != mBeatPrompterSongFilesFolder)
            // Song file storage folder has changed. We need to clear the cache.
                clearCache(false)
    }

    public override fun onDestroy() {
        Preferences.unregisterOnSharedPreferenceChangeListener(this)
        EventHandler.setSongListEventHandler(null)
        super.onDestroy()

        unbindService(mInAppPurchaseServiceConn)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val beforeListView = findViewById<ListView>(R.id.listView)
        val currentPosition = beforeListView.firstVisiblePosition
        val v = beforeListView.getChildAt(0)
        val top = if (v == null) 0 else v.top - beforeListView.paddingTop

        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_song_list)
        buildList()

        val afterListView = findViewById<ListView>(R.id.listView)
        afterListView.setSelectionFromTop(currentPosition, top)
    }

    override fun onResume() {
        super.onResume()
        SongLoadJob.mLoadedSong = null

        updateBluetoothIcon()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            if (mSongEndedNaturally)
                if (startNextSong())
                    return

        if (mListAdapter != null)
            mListAdapter!!.notifyDataSetChanged()

        // First run? Install demo files.
        if (isFirstRun)
            firstRunSetup()
        else
            SongLoadQueueWatcherTask.onResume()
    }

    private fun firstRunSetup() {
        Preferences.firstRun = false
        Preferences.storageSystem = StorageType.Demo
        Preferences.cloudPath = "/"
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
                        Logger.log("JSON exception during purchase.")
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                PLAY_SONG_REQUEST_CODE ->
                    if (resultCode == RESULT_OK)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            startNextSong()
            }
    }

    private fun startNextSong(): Boolean {
        mSongEndedNaturally = false
        if (mNowPlayingNode != null && mNowPlayingNode!!.mNextNode != null && shouldPlayNextSong()) {
            playPlaylistNode(mNowPlayingNode!!.mNextNode!!, false)
            return true
        }
        mNowPlayingNode = null
        return false
    }

    private fun canPerformCloudSync(): Boolean {
        return Preferences.storageSystem !== StorageType.Demo && cloudPath != null
    }

    private fun performFullCloudSync() {
        performCloudSync(null, false)
    }

    private fun performCloudSync(fileToUpdate: CachedFile?, dependenciesToo: Boolean) {
        if (fileToUpdate == null)
            clearTemporarySetList()
        val cs = Storage.getInstance(Preferences.storageSystem, this)
        val cloudPath = cloudPath
        if (cloudPath.isNullOrBlank())
            Toast.makeText(this, getString(R.string.no_cloud_folder_currently_set), Toast.LENGTH_LONG).show()
        else {
            mPerformingCloudSync = true
            val cdt = DownloadTask(cs, mSongListEventHandler!!, cloudPath, includeSubFolders, mCachedCloudFiles.getFilesToRefresh(fileToUpdate, dependenciesToo))
            cdt.execute()
        }
    }

    private fun sortSongList() {
        if (mSelectedFilter.mCanSort)
            when (Preferences.sorting) {
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
            MIDIAliasListAdapter(filterMIDIAliasFiles(mCachedCloudFiles.midiAliasFiles))
        else
            SongListAdapter(filterPlaylistNodes(mPlaylist))

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
            Logger.log("Failed to delete database file.")
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
        Logger.log("Building taglist ...")
        val tagAndFolderFilters = mutableListOf<Filter>()

        // Create filters from song tags and sub-folders. Many songs can share the same
        // tag/subfolder, so a bit of clever collection management is required here.
        val tagDictionaries = HashMap<String, MutableList<SongFile>>()
        val folderDictionaries = HashMap<String, MutableList<SongFile>>()
        for (song in mCachedCloudFiles.songFiles) {
            song.mTags.forEach { tagDictionaries.getOrPut(it) { mutableListOf() }.add(song) }
            if (!song.mSubfolder.isNullOrBlank())
                folderDictionaries.getOrPut(song.mSubfolder) { mutableListOf() }.add(song)
        }
        tagDictionaries.forEach {
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
        tagAndFolderFilters.sortBy { it.mName.toLowerCase() }

        // Now create the basic "all songs" filter, dead easy ...
        val allSongsFilter = AllSongsFilter(mCachedCloudFiles.songFiles.asSequence().filter { !it.mFilterOnly }.toMutableList())

        // Depending on whether we have a temporary set list file, we can create a temporary
        // set list filter ...
        val tempSetListFile = mCachedCloudFiles.setListFiles.firstOrNull { it.mFile == mTemporarySetListFile }
        val tempSetListFilter =
                if (tempSetListFile != null)
                    TemporarySetListFilter(tempSetListFile, mCachedCloudFiles.songFiles.toMutableList())
                else
                    null

        // Same thing for MIDI alias files ... there's always at least ONE (default aliases), but
        // if there aren't any more, don't bother creating a filter.
        val midiAliasFilesFilter =
                if (mCachedCloudFiles.midiAliasFiles.size > 1)
                    MIDIAliasFilesFilter(getString(R.string.midi_alias_files))
                else
                    null

        // Now bundle them altogether into one list.
        mFilters = listOf(allSongsFilter, tempSetListFilter, tagAndFolderFilters, midiAliasFilesFilter).flattenAll().filterIsInstance<Filter>()

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
                Logger.log("Failed to check for purchased version.", e)
            }
        return mFullVersionUnlocked
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.sort_songs)?.isEnabled = mSelectedFilter.mCanSort
        menu.findItem(R.id.buy_full_version)?.isVisible = !fullVersionUnlocked()
        menu.findItem(R.id.synchronize)?.isEnabled = canPerformCloudSync()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu
        menuInflater.inflate(R.menu.songlistmenu, menu)
        val spinnerLayout = menu.findItem(R.id.tagspinnerlayout).actionView as LinearLayout
        val spinner = spinnerLayout.findViewById<Spinner>(R.id.tagspinner)
        spinner.onItemSelectedListener = this
        val filterListAdapter = FilterListAdapter(mFilters)
        spinner.adapter = filterListAdapter

        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.isSubmitButtonEnabled = false

        updateBluetoothIcon()
        return true
    }

    private fun showSortDialog() {
        if (mSelectedFilter.mCanSort) {
            val adb = AlertDialog.Builder(this)
            val items = arrayOf<CharSequence>(getString(R.string.byTitle), getString(R.string.byArtist), getString(R.string.byDate), getString(R.string.byKey))
            adb.setItems(items) { d, n ->
                d.dismiss()
                when (n) {
                    0 -> Preferences.sorting = SortingPreference.Title
                    1 -> Preferences.sorting = SortingPreference.Artist
                    2 -> Preferences.sorting = SortingPreference.Date
                    3 -> Preferences.sorting = SortingPreference.Key
                }
                sortSongList()
                buildList()
            }
            adb.setTitle(getString(R.string.sortSongs))
            val ad = adb.create()
            ad.setCanceledOnTouchOutside(true)
            ad.show()
        }
    }

    private fun openManualURL() {
        val browserIntent = Intent(Intent.ACTION_VIEW, MANUAL_URL)
        startActivity(browserIntent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.synchronize -> performFullCloudSync()
            R.id.sort_songs -> showSortDialog()
            R.id.settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.buy_full_version -> buyFullVersion()
            R.id.manual -> openManualURL()
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
            Logger.log("Failed to buy full version.", e)
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
        val cs = Storage.getInstance(Preferences.storageSystem, this)
        cs.cacheFolder.clear()
        mPlaylist = Playlist()
        mCachedCloudFiles.clear()
        buildFilterList()
        try {
            writeDatabase()
        } catch (ioe: Exception) {
            Logger.log(ioe)
        }

        if (report)
            Toast.makeText(this, getString(R.string.cache_cleared), Toast.LENGTH_LONG).show()
    }

    fun processBluetoothChooseSongMessage(choiceInfo: SongChoiceInfo) {
        val beat = choiceInfo.mBeatScroll
        val smooth = choiceInfo.mSmoothScroll
        val scrollingMode = if (beat) ScrollingMode.Beat else if (smooth) ScrollingMode.Smooth else ScrollingMode.Manual

        val mimicDisplay = scrollingMode === ScrollingMode.Manual && Preferences.mimicBandLeaderDisplay

        // Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
        // Also, beat and smooth scrolling should never mimic.
        val nativeSettings = getSongDisplaySettings(scrollingMode)
        val sourceSettings = if (mimicDisplay) DisplaySettings(choiceInfo) else nativeSettings

        for (sf in mCachedCloudFiles.songFiles)
            if (sf.mNormalizedTitle == choiceInfo.mNormalizedTitle && sf.mNormalizedArtist == choiceInfo.mNormalizedArtist) {
                val track = mCachedCloudFiles.getMappedAudioFiles(choiceInfo.mTrack).firstOrNull()

                val songLoadInfo = SongLoadInfo(sf, track, scrollingMode, "", true, false, nativeSettings, sourceSettings, choiceInfo.mNoAudio)
                val songLoadJob = SongLoadJob(songLoadInfo, mFullVersionUnlocked || Preferences.storageSystem === StorageType.Demo)
                if (SongDisplayActivity.interruptCurrentSong(songLoadJob) == SongInterruptResult.NoSongToInterrupt)
                    playSong(PlaylistNode(sf), track, scrollingMode, true, nativeSettings, sourceSettings, choiceInfo.mNoAudio)
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
        val bluetoothMode = Preferences.bluetoothMode
        val slave = bluetoothMode === BluetoothMode.Client
        val connectedToServer = BluetoothManager.isConnectedToServer
        val master = bluetoothMode === BluetoothMode.Server
        val connectedClients = BluetoothManager.bluetoothClientCount
        val resourceID =
                if (slave)
                    if (connectedToServer)
                        R.drawable.duncecap
                    else
                        R.drawable.duncecap_outline
                else if (master)
                    when (connectedClients) {
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
                else R.drawable.blank_icon
        if (mMenu != null) {
            val btLayout = mMenu!!.findItem(R.id.btconnectionstatuslayout).actionView as LinearLayout
            val btIcon = btLayout.findViewById<ImageView>(R.id.btconnectionstatus)
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
            Logger.log(ioe)
        }
    }

    class SongListEventHandler internal constructor(private val mSongList: SongListActivity) : EventHandler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BLUETOOTH_CHOOSE_SONG -> mSongList.processBluetoothChooseSongMessage(msg.obj as SongChoiceInfo)
                CLOUD_SYNC_ERROR -> {
                    val adb = AlertDialog.Builder(mSongList)
                    adb.setMessage(BeatPrompter.getResourceString(R.string.cloudSyncErrorMessage, msg.obj as String))
                    adb.setTitle(BeatPrompter.getResourceString(R.string.cloudSyncErrorTitle))
                    adb.setPositiveButton("OK") { dialog, _ -> dialog.cancel() }
                    val ad = adb.create()
                    ad.setCanceledOnTouchOutside(true)
                    ad.show()
                }
                MIDI_PROGRAM_CHANGE -> {
                    val bytes = msg.obj as ByteArray
                    mSongList.startSongViaMidiProgramChange(bytes[0], bytes[1], bytes[2], bytes[3])
                }
                MIDI_SONG_SELECT -> mSongList.startSongViaMidiSongSelect(msg.arg1.toByte())
                CLEAR_CACHE -> mSongList.clearCache(true)
                CACHE_UPDATED -> {
                    val cache = msg.obj as CachedCloudFileCollection
                    mSongList.onCacheUpdated(cache)
                }
                CONNECTION_ADDED -> {
                    Toast.makeText(mSongList, BeatPrompter.getResourceString(R.string.connection_added, msg.obj.toString()), Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                CONNECTION_LOST -> {
                    Logger.log("Lost connection to device.")
                    Toast.makeText(mSongList, BeatPrompter.getResourceString(R.string.connection_lost, msg.obj.toString()), Toast.LENGTH_LONG).show()
                    mSongList.updateBluetoothIcon()
                }
                SONG_LOAD_CANCELLED -> {
                    if (!SongLoadQueueWatcherTask.isLoadingASong && !SongLoadQueueWatcherTask.hasASongToLoad)
                        mSongList.showLoadingProgressUI(false)
                }
                SONG_LOAD_FAILED -> {
                    mSongList.showLoadingProgressUI(false)
                    Toast.makeText(mSongList, msg.obj.toString(), Toast.LENGTH_LONG).show()
                }
                SONG_LOAD_COMPLETED -> {
                    Logger.logLoader { "Song ${msg.obj} was fully loaded successfully." }
                    mSongList.showLoadingProgressUI(false)
                    // No point starting up the activity if there are songs in the load queue
                    if (SongLoadQueueWatcherTask.hasASongToLoad || SongLoadQueueWatcherTask.isLoadingASong)
                        Logger.logLoader("Abandoning loaded song: there appears to be another song incoming.")
                    else
                        mSongList.startSongActivity(msg.obj as UUID)
                }
                SONG_LOAD_LINE_PROCESSED -> {
                    mSongList.updateLoadingProgress(msg.arg1, msg.arg2)
                }
            }
        }
    }

    private fun showLoadingProgressUI(show: Boolean) {
        findViewById<LinearLayout>(R.id.songLoadUI).visibility = if (show) View.VISIBLE else View.GONE
        if (!show)
            updateLoadingProgress(0, 1)
    }

    private fun updateLoadingProgress(currentProgress: Int, maxProgress: Int) {
        launch {
            findViewById<ProgressBar>(R.id.loadingProgress).apply {
                max = maxProgress
                progress = currentProgress
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_storageLocation_key) || key == getString(R.string.pref_useExternalStorage_key))
            initialiseLocalStorage()
        else if (key == getString(R.string.pref_largePrintList_key)
                || key == getString(R.string.pref_showBeatStyleIcons_key)
                || key == getString(R.string.pref_showMusicIcon_key)
                || key == getString(R.string.pref_showKeyInList_key))
            buildList()
    }

    override fun onQueryTextSubmit(searchText: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(searchText: String?): Boolean {
        mSearchText = searchText ?: ""
        buildList()
        return true
    }

    private fun filterMIDIAliasFiles(fileList: List<MIDIAliasFile>): List<MIDIAliasFile> {
        return fileList.filter {
            it.mFile != mDefaultMidiAliasesFile &&
                    (mSearchText.isBlank() || it.mNormalizedName.contains(mSearchText))
        }
    }

    private fun filterPlaylistNodes(playlist: Playlist): List<PlaylistNode> {
        return playlist.nodes.filter {
            mSearchText.isBlank() ||
                    it.mSongFile.mNormalizedArtist.contains(mSearchText) ||
                    it.mSongFile.mNormalizedTitle.contains(mSearchText)
        }
    }

    companion object {
        var mDefaultDownloads: MutableList<DownloadResult> = mutableListOf()
        var mCachedCloudFiles = CachedCloudFileCollection()

        private var mBeatPrompterDataFolder: File? = null
        var mBeatPrompterSongFilesFolder: File? = null

        var mSongEndedNaturally = false

        private var mFullVersionUnlocked = true

        private val MANUAL_URL = Uri.parse("https://drive.google.com/open?id=19Unw7FkSWNWGAncC_5D3DC0IANxvLMKG1pj6vfamnOI")

        private const val XML_DATABASE_FILE_NAME = "bpdb.xml"
        private const val XML_DATABASE_FILE_ROOT_ELEMENT_TAG = "beatprompterDatabase"
        private const val TEMPORARY_SETLIST_FILENAME = "temporary_setlist.txt"
        private const val DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt"

        lateinit var mSongListInstance: SongListActivity

        private const val PLAY_SONG_REQUEST_CODE = 3
        private const val GOOGLE_PLAY_TRANSACTION_FINISHED = 4
        private const val MY_PERMISSIONS_REQUEST = 5

        private const val FULL_VERSION_SKU_NAME = "full_version"

        // Fake storage items for temporary set list and default midi aliases
        var mTemporarySetListFile: File? = null
        var mDefaultMidiAliasesFile: File? = null

        var mSongListEventHandler: SongListEventHandler? = null

        @Throws(IOException::class)
        fun copyAssetsFileToLocalFolder(filename: String, destination: File) {
            val inputStream = BeatPrompter.assetManager.open(filename)
            inputStream.use { inStream ->
                val outputStream = FileOutputStream(destination)
                outputStream.use {
                    Utils.streamToStream(inStream, it)
                }
            }
        }
    }
}
