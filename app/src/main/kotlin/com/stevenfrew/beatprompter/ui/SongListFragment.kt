package com.stevenfrew.beatprompter.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothController
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.coroutines.CoroutineContext

class SongListFragment
	: Fragment(),
	AdapterView.OnItemClickListener,
	AdapterView.OnItemSelectedListener,
	SearchView.OnQueryTextListener,
	AdapterView.OnItemLongClickListener,
	SharedPreferences.OnSharedPreferenceChangeListener,
	CoroutineScope {
	private val mCoRoutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + mCoRoutineJob
	private var mPlaylist = Playlist()
	private var mSongLauncher: ActivityResultLauncher<Intent>? = null
	private var mNowPlayingNode: PlaylistNode? = null
	private var mListAdapter: BaseAdapter? = null
	private var mSearchText = ""
	private var mPerformingCloudSync = false
	private var mSavedListIndex = 0
	private var mSavedListOffset = 0

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
			val maf = filterMIDIAliasFiles(mCachedCloudItems.midiAliasFiles)[position]
			if (maf.mErrors.isNotEmpty())
				showMIDIAliasErrors(maf.mErrors)
		} else {
			val songToLoad = filterPlaylistNodes(mPlaylist)[position]
			if (!SongLoadQueueWatcherTask.isAlreadyLoadingSong(songToLoad.mSongFile))
				playPlaylistNode(songToLoad, false)
		}
	}

	internal fun startSongActivity(loadID: UUID) {
		val intent = Intent(context, SongDisplayActivity::class.java)
		intent.putExtra("loadID", ParcelUuid(loadID))
		Logger.logLoader { "Starting SongDisplayActivity for $loadID!" }
		mSongLauncher?.launch(intent)
	}

	internal fun startSongViaMidiProgramChange(
		bankMSB: Byte,
		bankLSB: Byte,
		program: Byte,
		channel: Byte
	) {
		startSongViaMidiSongTrigger(
			SongTrigger(
				bankMSB,
				bankLSB,
				program,
				channel,
				TriggerType.ProgramChange
			)
		)
	}

	internal fun startSongViaMidiSongSelect(song: Byte) {
		startSongViaMidiSongTrigger(
			SongTrigger(
				0.toByte(),
				0.toByte(),
				song,
				0.toByte(),
				TriggerType.SongSelect
			)
		)
	}

	private var mMenu: Menu? = null
	private var mFilters = listOf<Filter>()
	private var mSelectedFilter: Filter = AllSongsFilter(mutableListOf())

	internal fun updateBluetoothIcon() {
		val bluetoothMode = Preferences.bluetoothMode
		val slave = bluetoothMode === BluetoothMode.Client
		val connectedToServer = BluetoothController.isConnectedToServer
		val master = bluetoothMode === BluetoothMode.Server
		val connectedClients = BluetoothController.bluetoothClientCount
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

	override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
		applyFileFilter(mFilters[position])
		if (mPerformingCloudSync) {
			mPerformingCloudSync = false
			val listView = requireView().findViewById<ListView>(R.id.listView)
			listView.setSelectionFromTop(mSavedListIndex, mSavedListOffset)
		}
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

	override fun onNothingSelected(parent: AdapterView<*>) {
		//applyFileFilter(null)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		val beforeListView = requireView().findViewById<ListView>(R.id.listView)
		val currentPosition = beforeListView.firstVisiblePosition
		val v = beforeListView.getChildAt(0)
		val top = if (v == null) 0 else v.top - beforeListView.paddingTop

		super.onConfigurationChanged(newConfig)
		registerEventHandler()
		buildList().setSelectionFromTop(currentPosition, top)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		mMenu = menu
		inflater.inflate(R.menu.songlistmenu, menu)

		val spinnerLayout = menu.findItem(R.id.tagspinnerlayout).actionView as LinearLayout
		spinnerLayout.findViewById<Spinner>(R.id.tagspinner).apply {
			onItemSelectedListener = this@SongListFragment
			adapter = FilterListAdapter(mFilters)
		}

		(menu.findItem(R.id.search).actionView as SearchView).apply {
			setOnQueryTextListener(this@SongListFragment)
			isSubmitButtonEnabled = false
		}

		updateBluetoothIcon()
		super.onCreateOptionsMenu(menu, inflater)
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
		for (sf in mCachedCloudItems.songFiles)
			if (sf.matchesTrigger(mst)) {
				Logger.log { "Found trigger match: '${sf.mTitle}'." }
				playSongFile(sf, PlaylistNode(sf), true)
			}
	}

	private fun playPlaylistNode(node: PlaylistNode, startedByMidiTrigger: Boolean) {
		val selectedSong = node.mSongFile
		playSongFile(selectedSong, node, startedByMidiTrigger)
	}

	private fun playSongFile(
		selectedSong: SongFile,
		node: PlaylistNode,
		startedByMidiTrigger: Boolean
	) {
		val mute = Preferences.mute
		val manualMode = Preferences.manualMode
		val startupAudioIsViable =
			(selectedSong.mAudioFiles.isNotEmpty() && !manualMode && !selectedSong.mMixedMode)
		val (startupTrackName, startupTrack) =
			if (startupAudioIsViable)
				selectedSong.mAudioFiles[0] to mCachedCloudItems.getMappedAudioFiles(selectedSong.mAudioFiles[0])
					.firstOrNull()
			else
				null to null
		val mode =
			if (manualMode)
				ScrollingMode.Manual
			else
				selectedSong.bestScrollingMode
		val sds = getSongDisplaySettings(mode)
		val noAudioWhatsoever =
			manualMode || mute || (startupTrackName == null && !selectedSong.mMixedMode)
		playSong(node, startupTrack, mode, startedByMidiTrigger, sds, sds, noAudioWhatsoever)
	}

	private fun shouldPlayNextSong(): Boolean {
		return when (Preferences.playNextSong) {
			getString(R.string.playNextSongAlwaysValue) -> true
			getString(R.string.playNextSongSetListsOnlyValue) -> mSelectedFilter is SetListFilter
			else -> false
		}
	}

	private fun getSongDisplaySettings(songScrollMode: ScrollingMode): DisplaySettings {
		val onlyUseBeatFontSizes = Preferences.onlyUseBeatFontSizes

		val minimumFontSizeBeat = Preferences.minimumBeatFontSize
		val maximumFontSizeBeat = Preferences.maximumBeatFontSize
		val minimumFontSizeSmooth =
			if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.minimumSmoothFontSize
		val maximumFontSizeSmooth =
			if (onlyUseBeatFontSizes) maximumFontSizeBeat else Preferences.maximumSmoothFontSize
		val minimumFontSizeManual =
			if (onlyUseBeatFontSizes) minimumFontSizeBeat else Preferences.minimumManualFontSize
		val maximumFontSizeManual =
			if (onlyUseBeatFontSizes) maximumFontSizeBeat else Preferences.maximumManualFontSize

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

		val display = requireActivity().windowManager.defaultDisplay
		val size = Point()
		display.getSize(size)
		return DisplaySettings(
			resources.configuration.orientation,
			minimumFontSize.toFloat(),
			maximumFontSize.toFloat(),
			Rect(0, 0, size.x, size.y),
			songScrollMode != ScrollingMode.Manual
		)
	}

	private fun playSong(
		selectedNode: PlaylistNode,
		track: AudioFile?,
		scrollMode: ScrollingMode,
		startedByMidiTrigger: Boolean,
		nativeSettings: DisplaySettings,
		sourceSettings: DisplaySettings,
		noAudio: Boolean
	) {
		showLoadingProgressUI(true)
		mNowPlayingNode = selectedNode

		val nextSongName =
			if (selectedNode.mNextNode != null && shouldPlayNextSong()) selectedNode.mNextNode!!.mSongFile.mTitle else ""
		val songLoadInfo = SongLoadInfo(
			selectedNode.mSongFile,
			track,
			scrollMode,
			nextSongName,
			false,
			startedByMidiTrigger,
			nativeSettings,
			sourceSettings,
			noAudio
		)
		val songLoadJob = SongLoadJob(songLoadInfo)
		SongLoadQueueWatcherTask.loadSong(songLoadJob)
	}

	private fun clearTemporarySetList() {
		mFilters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.clear()
		for (slf in mCachedCloudItems.setListFiles)
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
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	private fun initialiseTemporarySetListFile(deleteExisting: Boolean) {
		try {
			if (deleteExisting)
				if (!mTemporarySetListFile!!.delete())
					Logger.log("Could not delete temporary set list file.")
			if (!mTemporarySetListFile!!.exists())
				Utils.appendToTextFile(
					mTemporarySetListFile!!,
					String.format("{set:%1\$s}", getString(R.string.temporary))
				)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	private fun onSongListLongClick(position: Int) {
		val selectedNode = filterPlaylistNodes(mPlaylist)[position]
		val selectedSong = selectedNode.mSongFile
		val selectedSet =
			if (mSelectedFilter is SetListFileFilter) (mSelectedFilter as SetListFileFilter).mSetListFile else null
		val mappedAudioFiles =
			mCachedCloudItems.getMappedAudioFiles(*selectedSong.mAudioFiles.toTypedArray())
		val trackNames =
			listOf(getString(R.string.no_audio), *mappedAudioFiles.map { it.mName }.toTypedArray())
		val tempSetListFilter =
			mFilters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()

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

		val arrayID: Int = if (includeRefreshSet)
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

		AlertDialog.Builder(context).apply {
			setTitle(R.string.song_options)
			setItems(arrayID) { _, which ->
				when (which) {
					0 -> showPlayDialog(selectedNode, selectedSong, trackNames)
					1 -> performCloudSync(selectedSong, false)
					2 -> performCloudSync(selectedSong, true)
					3 -> when {
						includeRefreshSet -> performCloudSync(selectedSet, false)
						includeClearSet -> clearTemporarySetList()
						else -> addToTemporarySet(selectedSong)
					}

					4 -> addToTemporarySet(selectedSong)
				}
			}
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun showPlayDialog(
		selectedNode: PlaylistNode,
		selectedSong: SongFile,
		trackNames: List<String>
	) {
		// Get the layout inflater
		val inflater = layoutInflater

		@SuppressLint("InflateParams")
		val view = inflater.inflate(R.layout.songlist_long_press_dialog, null)

		val audioSpinner = view
			.findViewById<Spinner>(R.id.audioSpinner)
		val audioSpinnerAdapter = ArrayAdapter(
			requireContext(),
			android.R.layout.simple_spinner_item, trackNames
		)
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
		AlertDialog.Builder(context).apply {
			setView(view)
			// Add action buttons
			setPositiveButton(R.string.play) { _, _ ->
				val selectedTrackName =
					if (audioSpinner.selectedItemPosition == 0)
						null
					else
						audioSpinner.selectedItem as String?
				val mode =
					when {
						beatButton.isChecked -> ScrollingMode.Beat
						smoothButton.isChecked -> ScrollingMode.Smooth
						else -> ScrollingMode.Manual
					}
				val sds = getSongDisplaySettings(mode)
				val track =
					if (selectedTrackName != null)
						mCachedCloudItems.getMappedAudioFiles(selectedTrackName).firstOrNull()
					else
						null
				playSong(selectedNode, track, mode, false, sds, sds, selectedTrackName == null)
			}
			setNegativeButton(R.string.cancel) { _, _ -> }
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun onMIDIAliasListLongClick(position: Int) {
		val maf = filterMIDIAliasFiles(mCachedCloudItems.midiAliasFiles)[position]
		val showErrors = maf.mErrors.isNotEmpty()
		val arrayID =
			if (showErrors) R.array.midi_alias_options_array_with_show_errors else R.array.midi_alias_options_array

		AlertDialog.Builder(context).apply {
			setTitle(R.string.midi_alias_list_options)
				.setItems(arrayID) { _, which ->
					if (which == 0)
						performCloudSync(maf, false)
					else if (which == 1)
						showMIDIAliasErrors(maf.mErrors)
				}
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun showMIDIAliasErrors(errors: List<FileParseError>) {
		val inflater = this.layoutInflater

		@SuppressLint("InflateParams")
		val view = inflater.inflate(R.layout.parse_errors_dialog, null)
		val tv = view.findViewById<TextView>(R.id.errors)
		val str = StringBuilder()
		for (fpe in errors)
			str.append(fpe.toString()).append("\n")
		tv.text = str.toString().trim()
		AlertDialog.Builder(context).apply {
			setView(view)
			create().apply {
				setButton(
					AlertDialog.BUTTON_NEUTRAL, "OK"
				) { dialog, _ -> dialog.dismiss() }
				setTitle(getString(R.string.midi_alias_file_errors))
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	override fun onItemLongClick(
		parent: AdapterView<*>,
		view: View,
		position: Int,
		id: Long
	): Boolean {
		if (mSelectedFilter is MIDIAliasFilesFilter)
			onMIDIAliasListLongClick(position)
		else
			onSongListLongClick(position)
		return true
	}

	private fun registerEventHandler() {
		mSongListInstance = this
		mSongListEventHandler = SongListEventHandler(this)
		// Now ready to receive events.
		EventRouter.addSongListEventHandler(tag!!, mSongListEventHandler!!)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		mSongLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK)
					startNextSong()
			}

		super.onCreate(savedInstanceState)

		registerEventHandler()

		initialiseLocalStorage()

		Preferences.registerOnSharedPreferenceChangeListener(this)

		setHasOptionsMenu(true)

		// Set font stuff first.
		val metrics = resources.displayMetrics
		Utils.FONT_SCALING = metrics.density
		Utils.MAXIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMax))
		Utils.MINIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMin))
		FontSizePreference.FONT_SIZE_MAX = Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE
		FontSizePreference.FONT_SIZE_MIN = 0
		FontSizePreference.FONT_SIZE_OFFSET = Utils.MINIMUM_FONT_SIZE

		if (isFirstRun)
			showFirstRunMessages()

		initialiseList()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.activity_song_list, container, false)
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
		val fragmentContext = requireContext()
		val s = fragmentContext.packageName
		try {
			val m = fragmentContext.packageManager
			val p = m.getPackageInfo(s, 0)
			mBeatPrompterDataFolder = File(p.applicationInfo.dataDir)
		} catch (e: PackageManager.NameNotFoundException) {
			// There is no way that this can happen.
			Logger.log("Package name not found ", e)
		}

		val songFilesFolder: String
		val useExternalStorage = Preferences.useExternalStorage
		val externalFilesDir = fragmentContext.getExternalFilesDir(null)
		songFilesFolder = if (useExternalStorage && externalFilesDir != null)
			externalFilesDir.absolutePath
		else
			mBeatPrompterDataFolder!!.absolutePath

		mBeatPrompterSongFilesFolder =
			if (songFilesFolder.isEmpty()) mBeatPrompterDataFolder else File(songFilesFolder)
		if (!mBeatPrompterSongFilesFolder!!.exists())
			if (!mBeatPrompterSongFilesFolder!!.mkdir())
				Logger.log("Failed to create song files folder.")

		if (!mBeatPrompterSongFilesFolder!!.exists())
			mBeatPrompterSongFilesFolder = mBeatPrompterDataFolder

		mTemporarySetListFile = File(mBeatPrompterDataFolder, TEMPORARY_SET_LIST_FILENAME)
		mDefaultMidiAliasesFile = File(mBeatPrompterDataFolder, DEFAULT_MIDI_ALIASES_FILENAME)
		initialiseTemporarySetListFile(false)
		try {
			copyAssetsFileToLocalFolder(DEFAULT_MIDI_ALIASES_FILENAME, mDefaultMidiAliasesFile!!)
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}

		mDefaultDownloads.apply {
			clear()
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterTemporarySetList",
						"BeatPrompterTemporarySetList",
						Date()
					), mTemporarySetListFile!!
				)
			)
			add(
				SuccessfulDownloadResult(
					FileInfo(
						"idBeatPrompterDefaultMidiAliases",
						getString(R.string.default_alias_set_name),
						Date()
					), mDefaultMidiAliasesFile!!
				)
			)
		}

		if (previousSongFilesFolder != null)
			if (previousSongFilesFolder != mBeatPrompterSongFilesFolder)
			// Song file storage folder has changed. We need to clear the cache.
				clearCache(false)
	}

	override fun onDestroy() {
		Preferences.unregisterOnSharedPreferenceChangeListener(this)
		EventRouter.removeSongListEventHandler(tag!!)
		super.onDestroy()
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
			SongLoadQueueWatcherTask.onResume()
	}

	private fun firstRunSetup() {
		Preferences.firstRun = false
		Preferences.storageSystem = StorageType.Demo
		Preferences.cloudPath = "/"
		performFullCloudSync()
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
			Toast.makeText(context, getString(R.string.no_cloud_folder_currently_set), Toast.LENGTH_LONG)
				.show()
		else {
			mPerformingCloudSync = true
			val cdt = DownloadTask(
				cs,
				mSongListEventHandler!!,
				cloudPath,
				includeSubFolders,
				mCachedCloudItems.getFilesToRefresh(fileToUpdate, dependenciesToo)
			)
			cdt.execute()
		}
	}

	private fun sortSongList() {
		if (mSelectedFilter.mCanSort) {
			val sorting = Preferences.sorting
			sorting.forEach {
				when (it) {
					SortingPreference.Date -> sortSongsByDateModified()
					SortingPreference.Artist -> sortSongsByArtist()
					SortingPreference.Title -> sortSongsByTitle()
					SortingPreference.Mode -> sortSongsByMode()
					SortingPreference.Key -> sortSongsByKey()
				}
			}
		}
	}

	private fun sortSongsByTitle() {
		mPlaylist.sortByTitle()
	}

	private fun sortSongsByMode() {
		mPlaylist.sortByMode()
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

	private fun shuffleSongList() {
		mPlaylist.shuffle()
		buildList()
	}

	private fun buildList(): ListView {
		mListAdapter = if (mSelectedFilter is MIDIAliasFilesFilter)
			MIDIAliasListAdapter(filterMIDIAliasFiles(mCachedCloudItems.midiAliasFiles))
		else
			SongListAdapter(filterPlaylistNodes(mPlaylist))

		return requireView().findViewById<ListView>(R.id.listView).apply {
			onItemClickListener = this@SongListFragment
			onItemLongClickListener = this@SongListFragment
			adapter = mListAdapter
		}
	}

	private fun readDatabase() {
		val database = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
		if (database.exists()) {
			mPlaylist = Playlist()

			val xmlDoc = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(database)

			mCachedCloudItems.readFromXML(xmlDoc)
			buildFilterList()
		}
	}

	private fun writeDatabase() {
		val database = File(mBeatPrompterDataFolder, XML_DATABASE_FILE_NAME)
		if (!database.delete())
			Logger.log("Failed to delete database file.")
		val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		val d = docBuilder.newDocument()
		val root = d.createElement(XML_DATABASE_FILE_ROOT_ELEMENT_TAG)
		d.appendChild(root)
		mCachedCloudItems.writeToXML(d, root)
		val transformer = TransformerFactory.newInstance().newTransformer()
		val output = StreamResult(database)
		val input = DOMSource(d)
		transformer.transform(input, output)
	}

	private fun buildFilterList() {
		Logger.log("Building tag list ...")
		val tagAndFolderFilters = mutableListOf<Filter>()

		// Create filters from song tags and sub-folders. Many songs can share the same
		// tag/subfolder, so a bit of clever collection management is required here.
		val tagDictionaries = HashMap<String, MutableList<SongFile>>()
		mCachedCloudItems.songFiles.forEach {
			it.mTags.forEach { tag -> tagDictionaries.getOrPut(tag) { mutableListOf() }.add(it) }
		}

		val folderDictionaries = HashMap<String, List<SongFile>>()
		mCachedCloudItems.folders.forEach {
			mCachedCloudItems.getSongsInFolder(it).let { songList ->
				if (songList.isNotEmpty())
					folderDictionaries[it.mName] = songList
			}
		}

		tagDictionaries.forEach {
			tagAndFolderFilters.add(TagFilter(it.key, it.value))
		}
		folderDictionaries.forEach {
			tagAndFolderFilters.add(FolderFilter(it.key, it.value))
		}
		tagAndFolderFilters.addAll(mCachedCloudItems.setListFiles.mapNotNull {
			if (it.mFile != mTemporarySetListFile)
				SetListFileFilter(it, mCachedCloudItems.songFiles)
			else
				null
		})
		tagAndFolderFilters.sortBy { it.mName.lowercase() }

		// Now create the basic "all songs" filter, dead easy ...
		val allSongsFilter = AllSongsFilter(mCachedCloudItems
			.songFiles
			.asSequence()
			.filter { !mCachedCloudItems.isFilterOnly(it) }
			.toList())

		// Depending on whether we have a temporary set list file, we can create a temporary
		// set list filter ...
		val tempSetListFile =
			mCachedCloudItems.setListFiles.firstOrNull { it.mFile == mTemporarySetListFile }
		val tempSetListFilter =
			if (tempSetListFile != null)
				TemporarySetListFilter(tempSetListFile, mCachedCloudItems.songFiles)
			else
				null

		// Same thing for MIDI alias files ... there's always at least ONE (default aliases), but
		// if there aren't any more, don't bother creating a filter.
		val midiAliasFilesFilter =
			if (mCachedCloudItems.midiAliasFiles.size > 1)
				MIDIAliasFilesFilter(getString(R.string.midi_alias_files))
			else
				null

		// Now bundle them altogether into one list.
		mFilters = listOf(
			allSongsFilter,
			tempSetListFilter,
			tagAndFolderFilters,
			midiAliasFilesFilter
		)
			.flattenAll()
			.filterIsInstance<Filter>()

		// The default selected filter should be "all songs".
		mSelectedFilter = allSongsFilter
		applyFileFilter(mSelectedFilter)
		requireActivity().invalidateOptionsMenu()
	}

	override fun onPrepareOptionsMenu(menu: Menu) {
		menu.apply {
			findItem(R.id.sort_songs)?.isEnabled = mSelectedFilter.mCanSort
			findItem(R.id.synchronize)?.isEnabled = canPerformCloudSync()
		}
	}

	private fun showSortDialog() {
		if (mSelectedFilter.mCanSort) {
			AlertDialog.Builder(context).apply {
				val items = arrayOf<CharSequence>(
					getString(R.string.byTitle),
					getString(R.string.byArtist),
					getString(R.string.byDate),
					getString(R.string.byKey),
					getString(R.string.byMode)
				)
				setItems(items) { d, n ->
					d.dismiss()
					Preferences.sorting = arrayOf(
						when (n) {
							1 -> SortingPreference.Artist
							2 -> SortingPreference.Date
							3 -> SortingPreference.Key
							4 -> SortingPreference.Mode
							else -> SortingPreference.Title
						}
					)
					sortSongList()
					buildList()
				}
				setTitle(getString(R.string.sortSongs))
				create().apply {
					setCanceledOnTouchOutside(true)
					show()
				}
			}
		}
	}

	private fun openBrowser(uri: Uri) {
		val browserIntent = Intent(Intent.ACTION_VIEW, uri)
		startActivity(browserIntent)

	}

	private fun openManualURL() {
		openBrowser(MANUAL_URL)
	}

	private fun openPrivacyPolicyURL() {
		openBrowser(PRIVACY_POLICY_URL)
	}

	private fun openBuyMeACoffeeURL() {
		openBrowser(Uri.parse(getString(R.string.buyMeACoffeeUrl)))
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.synchronize -> performFullCloudSync()
			R.id.shuffle -> shuffleSongList()
			R.id.sort_songs -> showSortDialog()
			R.id.settings -> startActivity(Intent(context, SettingsActivity::class.java))
			R.id.manual -> openManualURL()
			R.id.privacy_policy -> openPrivacyPolicyURL()
			R.id.buy_me_a_coffee -> openBuyMeACoffeeURL()
			R.id.about -> showAboutDialog()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
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
				AlertDialog.Builder(context).create().apply {
					setTitle(R.string.missing_songs_dialog_title)
					setMessage(message.toString())
					setButton(
						AlertDialog.BUTTON_NEUTRAL, "OK"
					) { dialog, _ -> dialog.dismiss() }
					show()
				}
			}
		}
	}

	private fun showAboutDialog() {
		AlertDialog.Builder(context).apply {
			@SuppressLint("InflateParams")
			val view = layoutInflater.inflate(R.layout.about_dialog, null)
			setView(view)
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	internal fun clearCache(report: Boolean) {
		// Clear both cache folders
		val cs = Storage.getInstance(Preferences.storageSystem, this)
		cs.cacheFolder.clear()
		mPlaylist = Playlist()
		mCachedCloudItems.clear()
		buildFilterList()
		try {
			writeDatabase()
		} catch (ioe: Exception) {
			Logger.log(ioe)
		}

		if (report)
			Toast.makeText(context, getString(R.string.cache_cleared), Toast.LENGTH_LONG).show()
	}

	fun processBluetoothChooseSongMessage(choiceInfo: SongChoiceInfo) {
		val beat = choiceInfo.mBeatScroll
		val smooth = choiceInfo.mSmoothScroll
		val scrollingMode =
			if (beat) ScrollingMode.Beat else if (smooth) ScrollingMode.Smooth else ScrollingMode.Manual

		val mimicDisplay = scrollingMode === ScrollingMode.Manual && Preferences.mimicBandLeaderDisplay

		// Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
		// Also, beat and smooth scrolling should never mimic.
		val nativeSettings = getSongDisplaySettings(scrollingMode)
		val sourceSettings = if (mimicDisplay) DisplaySettings(choiceInfo) else nativeSettings

		for (sf in mCachedCloudItems.songFiles)
			if (sf.mNormalizedTitle == choiceInfo.mNormalizedTitle && sf.mNormalizedArtist == choiceInfo.mNormalizedArtist) {
				val track = mCachedCloudItems.getMappedAudioFiles(choiceInfo.mTrack).firstOrNull()

				val songLoadInfo = SongLoadInfo(
					sf,
					track,
					scrollingMode,
					"",
					mStartedByBandLeader = true,
					mStartedByMIDITrigger = false,
					mNativeDisplaySettings = nativeSettings,
					mSourceDisplaySettings = sourceSettings,
					mNoAudio = choiceInfo.mNoAudio
				)
				val songLoadJob = SongLoadJob(songLoadInfo)
				if (SongDisplayActivity.interruptCurrentSong(songLoadJob) == SongInterruptResult.NoSongToInterrupt)
					playSong(
						PlaylistNode(sf),
						track,
						scrollingMode,
						true,
						nativeSettings,
						sourceSettings,
						choiceInfo.mNoAudio
					)
				break
			}
	}

	private fun showFirstRunMessages() {
		//  Declare a new thread to do a preference check
		val t = Thread {
			val i = Intent(context, IntroActivity::class.java)
			startActivity(i)
		}

		// Start the thread
		t.start()
	}

	internal fun onCacheUpdated(cache: CachedCloudCollection) {
		val listView = requireView().findViewById<ListView>(R.id.listView)
		mSavedListIndex = listView.firstVisiblePosition
		val v = listView.getChildAt(0)
		mSavedListOffset = if (v == null) 0 else v.top - listView.paddingTop

		mCachedCloudItems = cache
		try {
			writeDatabase()
			buildFilterList()
		} catch (ioe: Exception) {
			Logger.log(ioe)
		}
	}

	private fun showLoadingProgressUI(show: Boolean) {
		requireView().findViewById<LinearLayout>(R.id.songLoadUI).visibility =
			if (show) View.VISIBLE else View.GONE
		if (!show)
			updateLoadingProgress(0, 1)
	}

	private fun updateLoadingProgress(currentProgress: Int, maxProgress: Int) {
		launch {
			requireView().findViewById<ProgressBar>(R.id.loadingProgress).apply {
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
			|| key == getString(R.string.pref_showKeyInList_key)
		)
			buildList()
	}

	override fun onQueryTextSubmit(searchText: String?): Boolean {
		return true
	}

	override fun onQueryTextChange(searchText: String?): Boolean {
		mSearchText = searchText?.lowercase() ?: ""
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
		var mSongListEventHandler: SongListEventHandler? = null
		var mDefaultDownloads: MutableList<DownloadResult> = mutableListOf()
		var mCachedCloudItems = CachedCloudCollection()

		private var mBeatPrompterDataFolder: File? = null
		var mBeatPrompterSongFilesFolder: File? = null

		var mSongEndedNaturally = false

		private val MANUAL_URL =
			Uri.parse("https://drive.google.com/open?id=19Unw7FkSWNWGAncC_5D3DC0IANxvLMKG1pj6vfamnOI")
		private val PRIVACY_POLICY_URL =
			Uri.parse("https://github.com/peeveen/app-policies/blob/269734a07fee937f4e87d69415cd8dc3d4c999d9/beatprompter/privacy-policy.md")

		private const val XML_DATABASE_FILE_NAME = "bpdb.xml"
		private const val XML_DATABASE_FILE_ROOT_ELEMENT_TAG = "beatprompterDatabase"
		private const val TEMPORARY_SET_LIST_FILENAME = "temporary_setlist.txt"
		private const val DEFAULT_MIDI_ALIASES_FILENAME = "default_midi_aliases.txt"

		lateinit var mSongListInstance: SongListFragment

		// Fake storage items for temporary set list and default midi aliases
		var mTemporarySetListFile: File? = null
		var mDefaultMidiAliasesFile: File? = null

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

	class SongListEventHandler internal constructor(private val mSongList: SongListFragment) :
		Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.BLUETOOTH_CHOOSE_SONG -> mSongList.processBluetoothChooseSongMessage(msg.obj as SongChoiceInfo)
				Events.CLOUD_SYNC_ERROR -> {
					AlertDialog.Builder(mSongList.context).apply {
						setMessage(
							BeatPrompter.getResourceString(
								R.string.cloudSyncErrorMessage,
								msg.obj as String
							)
						)
						setTitle(BeatPrompter.getResourceString(R.string.cloudSyncErrorTitle))
						setPositiveButton("OK") { dialog, _ -> dialog.cancel() }
						create().apply {
							setCanceledOnTouchOutside(true)
							show()
						}
					}
				}

				Events.MIDI_PROGRAM_CHANGE -> {
					val bytes = msg.obj as ByteArray
					mSongList.startSongViaMidiProgramChange(bytes[0], bytes[1], bytes[2], bytes[3])
				}

				Events.MIDI_SONG_SELECT -> mSongList.startSongViaMidiSongSelect(msg.arg1.toByte())
				Events.CLEAR_CACHE -> mSongList.clearCache(true)
				Events.CACHE_UPDATED -> {
					val cache = msg.obj as CachedCloudCollection
					mSongList.onCacheUpdated(cache)
				}

				Events.CONNECTION_ADDED -> {
					Toast.makeText(
						mSongList.context,
						BeatPrompter.getResourceString(R.string.connection_added, msg.obj.toString()),
						Toast.LENGTH_LONG
					).show()
					mSongList.updateBluetoothIcon()
				}

				Events.CONNECTION_LOST -> {
					Logger.log("Lost connection to device.")
					Toast.makeText(
						mSongList.context,
						BeatPrompter.getResourceString(R.string.connection_lost, msg.obj.toString()),
						Toast.LENGTH_LONG
					).show()
					mSongList.updateBluetoothIcon()
				}

				Events.SONG_LOAD_CANCELLED -> {
					if (!SongLoadQueueWatcherTask.isLoadingASong && !SongLoadQueueWatcherTask.hasASongToLoad)
						mSongList.showLoadingProgressUI(false)
				}

				Events.SONG_LOAD_FAILED -> {
					mSongList.showLoadingProgressUI(false)
					Toast.makeText(mSongList.context, msg.obj.toString(), Toast.LENGTH_LONG).show()
				}

				Events.SONG_LOAD_COMPLETED -> {
					Logger.logLoader { "Song ${msg.obj} was fully loaded successfully." }
					mSongList.showLoadingProgressUI(false)
					// No point starting up the activity if there are songs in the load queue
					if (SongLoadQueueWatcherTask.hasASongToLoad || SongLoadQueueWatcherTask.isLoadingASong)
						Logger.logLoader("Abandoning loaded song: there appears to be another song incoming.")
					else
						mSongList.startSongActivity(msg.obj as UUID)
				}

				Events.SONG_LOAD_LINE_PROCESSED -> {
					mSongList.updateLoadingProgress(msg.arg1, msg.arg2)
				}
			}
		}
	}
}
