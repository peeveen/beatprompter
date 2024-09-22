package com.stevenfrew.beatprompter.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.BuildConfig
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.CachedCloudCollection
import com.stevenfrew.beatprompter.cache.MIDIAliasFile
import com.stevenfrew.beatprompter.cache.ReadCacheTask
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.set.Playlist
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import com.stevenfrew.beatprompter.song.load.SongInterruptResult
import com.stevenfrew.beatprompter.song.load.SongLoadInfo
import com.stevenfrew.beatprompter.song.load.SongLoadJob
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.ui.filter.AllSongsFilter
import com.stevenfrew.beatprompter.ui.filter.Filter
import com.stevenfrew.beatprompter.ui.filter.FilterComparator
import com.stevenfrew.beatprompter.ui.filter.FolderFilter
import com.stevenfrew.beatprompter.ui.filter.MIDIAliasFilesFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFileFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFilter
import com.stevenfrew.beatprompter.ui.filter.SongFilter
import com.stevenfrew.beatprompter.ui.filter.TagFilter
import com.stevenfrew.beatprompter.ui.filter.TemporarySetListFilter
import com.stevenfrew.beatprompter.ui.pref.FontSizePreference
import com.stevenfrew.beatprompter.ui.pref.SettingsActivity
import com.stevenfrew.beatprompter.ui.pref.SortingPreference
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import com.stevenfrew.beatprompter.util.flattenAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class SongListFragment
	: Fragment(),
	AdapterView.OnItemClickListener,
	AdapterView.OnItemSelectedListener,
	SearchView.OnQueryTextListener,
	AdapterView.OnItemLongClickListener,
	SharedPreferences.OnSharedPreferenceChangeListener,
	CoroutineScope {
	private val coroutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main + coroutineJob
	private var songLauncher: ActivityResultLauncher<Intent>? = null
	private var listAdapter: BaseAdapter? = null
	private var menu: Menu? = null

	private var playlist = Playlist()
	private var nowPlayingNode: PlaylistNode? = null
	private var searchText = ""
	private var performingCloudSync = false
	private var savedListIndex = 0
	private var savedListOffset = 0
	private var filters = listOf<Filter>()
	private val selectedTagFilters = mutableListOf<TagFilter>()
	private var selectedFilter: Filter = AllSongsFilter(mutableListOf())

	override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		if (selectedFilter is MIDIAliasFilesFilter) {
			val maf = filterMIDIAliasFiles(Cache.cachedCloudItems.midiAliasFiles)[position]
			if (maf.errors.isNotEmpty())
				showMIDIAliasErrors(maf.errors)
		} else {
			val songToLoad = filterPlaylistNodes(playlist)[position]
			if (!SongLoadQueueWatcherTask.isAlreadyLoadingSong(songToLoad.songFile))
				playPlaylistNode(songToLoad, false)
		}
	}

	internal fun startSongActivity(loadID: UUID) {
		val intent = Intent(context, SongDisplayActivity::class.java)
		intent.putExtra("loadID", ParcelUuid(loadID))
		Logger.logLoader({ "Starting SongDisplayActivity for $loadID!" })
		songLauncher?.launch(intent)
	}

	internal fun startSongViaMidiProgramChange(
		bankMSB: Byte,
		bankLSB: Byte,
		program: Byte,
		channel: Byte
	) = startSongViaMidiSongTrigger(
		SongTrigger(
			bankMSB,
			bankLSB,
			program,
			channel,
			TriggerType.ProgramChange
		)
	)

	internal fun startSongViaMidiSongSelect(song: Byte) = startSongViaMidiSongTrigger(
		SongTrigger(
			0.toByte(),
			0.toByte(),
			song,
			0.toByte(),
			TriggerType.SongSelect
		)
	)

	internal fun updateBluetoothIcon() {
		val bluetoothMode = Preferences.bluetoothMode
		val slave = bluetoothMode === BluetoothMode.Client
		val connectedToServer = Bluetooth.isConnectedToServer
		val master = bluetoothMode === BluetoothMode.Server
		val connectedClients = Bluetooth.bluetoothClientCount
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
		if (menu != null) {
			val btLayout = menu!!.findItem(R.id.btconnectionstatuslayout).actionView as LinearLayout
			val btIcon = btLayout.findViewById<ImageView>(R.id.btconnectionstatus)
			btIcon?.setImageResource(resourceID)
		}
	}

	override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
		if (Preferences.clearTagsOnFolderChange)
			selectedTagFilters.clear()
		applyFileFilter(filters[position])
		if (performingCloudSync) {
			performingCloudSync = false
			val listView = requireView().findViewById<ListView>(R.id.listView)
			listView.setSelectionFromTop(savedListIndex, savedListOffset)
		}
	}

	private fun applyFileFilter(filter: Filter) {
		selectedFilter = filter
		playlist = if (filter is SongFilter)
			Playlist(filter.songs.filter {
				if (selectedTagFilters.isNotEmpty()) selectedTagFilters.any { filter ->
					filter.songs.contains(
						it
					)
				} else true
			})
		else
			Playlist()
		sortSongList()
		listAdapter = buildListAdapter()
		updateListView()
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
		listAdapter = buildListAdapter()
		updateListView().setSelectionFromTop(currentPosition, top)
	}

	private fun updateListView(): ListView =
		requireView().findViewById<ListView>(R.id.listView).apply {
			onItemClickListener = this@SongListFragment
			onItemLongClickListener = this@SongListFragment
			adapter = listAdapter
		}

	private fun setFilters(initialSelection: Int = 0) {
		val spinnerLayout = menu?.findItem(R.id.tagspinnerlayout)?.actionView as? LinearLayout
		spinnerLayout?.findViewById<Spinner>(R.id.tagspinner)?.apply {
			onItemSelectedListener = this@SongListFragment
			adapter = FilterListAdapter(filters, selectedTagFilters, requireActivity()) {
				applyFileFilter(selectedFilter)
			}
			setSelection(initialSelection)
		}
	}

	@Deprecated("Deprecated in Java")
	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.menu = menu
		inflater.inflate(R.menu.songlistmenu, menu)
		setFilters()
		(menu.findItem(R.id.search).actionView as SearchView).apply {
			setOnQueryTextListener(this@SongListFragment)
			isSubmitButtonEnabled = false
		}
		if (!BuildConfig.DEBUG) {
			menu.findItem(R.id.debug_log).apply {
				isVisible = false
			}
		}
		updateBluetoothIcon()
		super.onCreateOptionsMenu(menu, inflater)
	}

	private fun startSongViaMidiSongTrigger(mst: SongTrigger) {
		for (node in playlist.nodes)
			if (node.songFile.matchesTrigger(mst)) {
				Logger.log({ "Found trigger match: '${node.songFile.title}'." })
				playPlaylistNode(node, true)
				return
			}
		// Otherwise, it might be a song that is not currently onscreen.
		// Still play it though!
		for (sf in Cache.cachedCloudItems.songFiles)
			if (sf.matchesTrigger(mst)) {
				Logger.log({ "Found trigger match: '${sf.title}'." })
				playSongFile(sf, PlaylistNode(sf), true)
			}
	}

	private fun playPlaylistNode(node: PlaylistNode, startedByMidiTrigger: Boolean) {
		val selectedSong = node.songFile
		playSongFile(selectedSong, node, startedByMidiTrigger)
	}

	private fun playSongFile(
		selectedSong: SongFile,
		node: PlaylistNode,
		startedByMidiTrigger: Boolean
	) {
		val mute = Preferences.mute
		val manualMode = Preferences.manualMode
		val defaultVariation = selectedSong.variations.first()
		val mode =
			if (manualMode)
				ScrollingMode.Manual
			else
				selectedSong.bestScrollingMode
		val sds = getSongDisplaySettings(mode)
		val noAudioWhatsoever = manualMode || mute
		playSong(node, defaultVariation, mode, startedByMidiTrigger, sds, sds, noAudioWhatsoever)
	}

	private fun shouldPlayNextSong(): Boolean =
		when (Preferences.playNextSong) {
			getString(R.string.playNextSongAlwaysValue) -> true
			getString(R.string.playNextSongSetListsOnlyValue) -> selectedFilter is SetListFilter
			else -> false
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
		variation: String,
		scrollMode: ScrollingMode,
		startedByMidiTrigger: Boolean,
		nativeSettings: DisplaySettings,
		sourceSettings: DisplaySettings,
		noAudio: Boolean
	) {
		showLoadingProgressUI(true)
		nowPlayingNode = selectedNode

		val nextSongName =
			if (selectedNode.nextSong != null && shouldPlayNextSong()) selectedNode.nextSong.songFile.title else ""
		val songLoadInfo = SongLoadInfo(
			selectedNode.songFile,
			variation,
			scrollMode,
			nextSongName,
			false,
			startedByMidiTrigger,
			nativeSettings,
			sourceSettings,
			noAudio,
			Preferences.audioLatency,
			0
		)
		val songLoadJob = SongLoadJob(songLoadInfo)
		SongLoadQueueWatcherTask.loadSong(songLoadJob)
	}

	private fun addToTemporarySet(song: SongFile) {
		filters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.addSong(song)
		try {
			Cache.initialiseTemporarySetListFile(false, requireContext())
			Utils.appendToTextFile(Cache.temporarySetListFile!!, SetListEntry(song).toString())
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	private fun onSongListLongClick(position: Int) {
		val selectedNode = filterPlaylistNodes(playlist)[position]
		val selectedSong = selectedNode.songFile
		val selectedSet =
			if (selectedFilter is SetListFileFilter) (selectedFilter as SetListFileFilter).setListFile else null
		val tempSetListFilter =
			filters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()

		val addAllowed =
			if (tempSetListFilter != null)
				if (selectedFilter !== tempSetListFilter)
					!tempSetListFilter.containsSong(selectedSong)
				else
					false
			else
				true
		val includeRefreshSet = selectedSet != null && selectedFilter !== tempSetListFilter
		val includeClearSet = selectedFilter === tempSetListFilter

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
					0 -> showPlayDialog(selectedNode, selectedSong)
					1 -> performingCloudSync =
						Cache.performCloudSync(selectedSong, false, this@SongListFragment)

					2 -> performingCloudSync =
						Cache.performCloudSync(selectedSong, true, this@SongListFragment)

					3 -> when {
						includeRefreshSet -> performingCloudSync =
							Cache.performCloudSync(selectedSet, false, this@SongListFragment)

						includeClearSet -> Cache.clearTemporarySetList(requireContext())
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
		selectedSong: SongFile
	) {
		// Get the layout inflater
		val inflater = layoutInflater

		@SuppressLint("InflateParams")
		val view = inflater.inflate(R.layout.songlist_long_press_dialog, null)

		val variationSpinner = view
			.findViewById<Spinner>(R.id.variationSpinner)
		val variationSpinnerAdapter = ArrayAdapter(
			requireContext(),
			android.R.layout.simple_spinner_item, selectedSong.variations
		)
		val noAudioCheckbox = view.findViewById<CheckBox>(R.id.noAudioCheckbox)
		variationSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		variationSpinner.adapter = variationSpinnerAdapter

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
			setTitle(R.string.play_options)
			// Add action buttons
			setPositiveButton(R.string.play) { _, _ ->
				val selectedVariation = variationSpinner.selectedItem as String
				val noAudio = noAudioCheckbox.isChecked
				val mode =
					when {
						beatButton.isChecked -> ScrollingMode.Beat
						smoothButton.isChecked -> ScrollingMode.Smooth
						else -> ScrollingMode.Manual
					}
				val sds = getSongDisplaySettings(mode)
				playSong(selectedNode, selectedVariation, mode, false, sds, sds, noAudio)
			}
			setNegativeButton(R.string.cancel) { _, _ -> }
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun onMIDIAliasListLongClick(position: Int) {
		val maf = filterMIDIAliasFiles(Cache.cachedCloudItems.midiAliasFiles)[position]
		val showErrors = maf.errors.isNotEmpty()
		val arrayID =
			if (showErrors) R.array.midi_alias_options_array_with_show_errors else R.array.midi_alias_options_array

		AlertDialog.Builder(context).apply {
			setTitle(R.string.midi_alias_list_options)
				.setItems(arrayID) { _, which ->
					if (which == 0)
						performingCloudSync = Cache.performCloudSync(maf, false, this@SongListFragment)
					else if (which == 1)
						showMIDIAliasErrors(maf.errors)
				}
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun showMIDIAliasErrors(errors: List<FileParseError>) {
		@SuppressLint("InflateParams")
		val view = layoutInflater.inflate(R.layout.parse_errors_dialog, null)
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
		if (selectedFilter is MIDIAliasFilesFilter)
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
		songLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK)
					startNextSong()
			}

		super.onCreate(savedInstanceState)

		registerEventHandler()

		Preferences.registerOnSharedPreferenceChangeListener(this)

		setHasOptionsMenu(true)

		Cache.initialiseLocalStorage(requireContext())

		// Set font stuff first.
		val metrics = resources.displayMetrics
		Utils.FONT_SCALING = metrics.density
		Utils.MAXIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMax))
		Utils.MINIMUM_FONT_SIZE = Integer.parseInt(getString(R.string.fontSizeMin))
		FontSizePreference.FONT_SIZE_MAX = Utils.MAXIMUM_FONT_SIZE - Utils.MINIMUM_FONT_SIZE
		FontSizePreference.FONT_SIZE_MIN = 0
		FontSizePreference.FONT_SIZE_OFFSET = Utils.MINIMUM_FONT_SIZE

		val firstRun = Preferences.firstRun
		if (firstRun) {
			Preferences.firstRun = false
			showFirstRunMessages()
		}

		ReadCacheTask(
			requireContext(),
			Cache.CacheEventHandler
		) { onDatabaseReadCompleted(it, firstRun) }.execute(Unit)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? = inflater.inflate(R.layout.activity_song_list, container, false)

	private fun onDatabaseReadCompleted(databaseExists: Boolean, firstRun: Boolean) {
		if (!databaseExists && firstRun) {
			Preferences.storageSystem = StorageType.Demo
			Preferences.cloudPath = "/"
			Cache.performFullCloudSync(this)
		}
	}

	private fun initialiseList(cache: CachedCloudCollection) {
		playlist = Playlist()
		buildFilterList(cache)
	}

	override fun onDestroy() {
		Preferences.unregisterOnSharedPreferenceChangeListener(this)
		EventRouter.removeSongListEventHandler(tag!!)
		super.onDestroy()
	}

	override fun onResume() {
		super.onResume()

		updateBluetoothIcon()

		if (listAdapter != null)
			listAdapter!!.notifyDataSetChanged()

		SongLoadQueueWatcherTask.onResume()
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

	private fun startNextSong(): Boolean {
		mSongEndedNaturally = false
		if (nowPlayingNode != null && nowPlayingNode!!.nextSong != null && shouldPlayNextSong()) {
			playPlaylistNode(nowPlayingNode!!.nextSong!!, false)
			return true
		}
		nowPlayingNode = null
		return false
	}

	private fun sortSongList() {
		if (selectedFilter.canSort) {
			val sorting = Preferences.sorting
			sorting.forEach {
				playlist = when (it) {
					SortingPreference.Date -> playlist.sortByDateModified()
					SortingPreference.Artist -> playlist.sortByArtist()
					SortingPreference.Title -> playlist.sortByTitle()
					SortingPreference.Mode -> playlist.sortByMode()
					SortingPreference.Rating -> playlist.sortByRating()
					SortingPreference.Key -> playlist.sortByKey()
				}
			}
		}
	}

	private fun shuffleSongList() {
		playlist = playlist.shuffle()
		listAdapter = buildListAdapter()
		updateListView()
	}

	private fun buildListAdapter(): BaseAdapter =
		requireActivity().let {
			if (selectedFilter is MIDIAliasFilesFilter)
				MIDIAliasListAdapter(
					filterMIDIAliasFiles(Cache.cachedCloudItems.midiAliasFiles),
					it
				)
			else
				SongListAdapter(filterPlaylistNodes(playlist), it)
		}

	private fun buildFilterList(cache: CachedCloudCollection) {
		Logger.log("Building tag list ...")
		val lastSelectedFilter = selectedFilter
		val tagAndFolderFilters = mutableListOf<Filter>()

		// Create filters from song tags and sub-folders. Many songs can share the same
		// tag/subfolder, so a bit of clever collection management is required here.
		val tagDictionaries = HashMap<String, MutableList<SongFile>>()
		cache.songFiles.forEach {
			it.tags.forEach { tag -> tagDictionaries.getOrPut(tag) { mutableListOf() }.add(it) }
		}

		val folderDictionaries = HashMap<String, List<SongFile>>()
		cache.folders.forEach {
			cache.getSongsInFolder(it).let { songList ->
				if (songList.isNotEmpty())
					folderDictionaries[it.name] = songList
			}
		}

		tagDictionaries.forEach {
			tagAndFolderFilters.add(TagFilter(it.key, it.value))
		}
		folderDictionaries.forEach {
			tagAndFolderFilters.add(FolderFilter(it.key, it.value))
		}
		tagAndFolderFilters.addAll(cache.setListFiles.mapNotNull {
			if (it.file != Cache.temporarySetListFile)
				SetListFileFilter(it, cache.songFiles)
			else
				null
		})
		tagAndFolderFilters.sortBy { it.name.lowercase() }

		// Now create the basic "all songs" filter, dead easy ...
		val allSongsFilter = createAllSongsFilter(cache)

		// Depending on whether we have a temporary set list file, we can create a temporary
		// set list filter ...
		val tempSetListFile =
			cache.setListFiles.firstOrNull { it.file == Cache.temporarySetListFile }
		val tempSetListFilter =
			if (tempSetListFile != null)
				TemporarySetListFilter(tempSetListFile, cache.songFiles)
			else
				null

		// Same thing for MIDI alias files ... there's always at least ONE (default aliases), but
		// if there aren't any more, don't bother creating a filter.
		val midiAliasFilesFilter =
			if (cache.midiAliasFiles.size > 1)
				MIDIAliasFilesFilter(getString(R.string.midi_alias_files))
			else
				null

		// Now bundle them altogether into one list.
		filters = listOf(
			allSongsFilter,
			tempSetListFilter,
			tagAndFolderFilters,
			midiAliasFilesFilter
		)
			.flattenAll()
			.filterIsInstance<Filter>()
			.sortedWith(FilterComparator.instance)

		// The default selected filter should be "all songs".
		selectedFilter = findFilter(lastSelectedFilter, cache)
		applyFileFilter(selectedFilter)
		val selectedFilterIndex = filters.indexOf(selectedFilter)
		setFilters(if (selectedFilterIndex == -1) 0 else selectedFilterIndex)
		requireActivity().invalidateOptionsMenu()
	}

	private fun createAllSongsFilter(cache: CachedCloudCollection): Filter = AllSongsFilter(cache
		.songFiles
		.asSequence()
		.filterNot { cache.isFilterOnly(it) }
		.toList())

	private fun findFilter(filter: Filter, cache: CachedCloudCollection): Filter =
		filters.find { it == filter } ?: filters.find { it is AllSongsFilter } ?: createAllSongsFilter(
			cache
		)

	@Deprecated("Deprecated in Java")
	override fun onPrepareOptionsMenu(menu: Menu) =
		menu.run {
			findItem(R.id.sort_songs)?.isEnabled = selectedFilter.canSort
			findItem(R.id.synchronize)?.isEnabled = Cache.canPerformCloudSync()
		}

	private fun showSortDialog() {
		if (selectedFilter.canSort) {
			AlertDialog.Builder(context).apply {
				val items = arrayOf<CharSequence>(
					getString(R.string.byTitle),
					getString(R.string.byArtist),
					getString(R.string.byDate),
					getString(R.string.byKey),
					getString(R.string.byMode),
					getString(R.string.byRating),
				)
				setItems(items) { d, n ->
					d.dismiss()
					Preferences.sorting = arrayOf(
						when (n) {
							1 -> SortingPreference.Artist
							2 -> SortingPreference.Date
							3 -> SortingPreference.Key
							4 -> SortingPreference.Mode
							5 -> SortingPreference.Rating
							else -> SortingPreference.Title
						}
					)
					sortSongList()
					listAdapter = buildListAdapter()
					updateListView()
				}
				setTitle(getString(R.string.sortSongs))
				create().apply {
					setCanceledOnTouchOutside(true)
					show()
				}
			}
		}
	}

	private fun openBrowser(uriResource: Int) {
		val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(uriResource)))
		startActivity(browserIntent)
	}

	private fun openManualURL() = openBrowser(R.string.instructionsUrl)
	private fun openPrivacyPolicyURL() = openBrowser(R.string.privacyPolicyUrl)
	private fun openBuyMeACoffeeURL() = openBrowser(R.string.buyMeACoffeeUrl)
	private fun showDebugLog() =
		Utils.showMessageDialog(
			BeatPrompter.debugLog,
			R.string.debugLogDialogCaption,
			this.requireContext()
		)

	@Deprecated("Deprecated in Java")
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.synchronize -> Cache.performFullCloudSync(this)
			R.id.shuffle -> shuffleSongList()
			R.id.sort_songs -> showSortDialog()
			R.id.settings -> startActivity(Intent(context, SettingsActivity::class.java))
			R.id.manual -> openManualURL()
			R.id.privacy_policy -> openPrivacyPolicyURL()
			R.id.buy_me_a_coffee -> openBuyMeACoffeeURL()
			R.id.debug_log -> showDebugLog()
			R.id.about -> showAboutDialog()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	private fun showSetListMissingSongs() {
		if (selectedFilter is SetListFileFilter) {
			val slf = selectedFilter as SetListFileFilter
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
				findViewById<TextView>(R.id.versionInfo)?.text = BeatPrompter.appResources.getString(
					R.string.versionInfo,
					BuildConfig.VERSION_NAME,
					BuildConfig.VERSION_CODE
				)
				findViewById<ImageView>(R.id.buyMeACoffeeIcon)?.setOnClickListener { openBuyMeACoffeeURL() }
			}
		}
	}

	fun processBluetoothChooseSongMessage(choiceInfo: SongChoiceInfo) {
		val beat = choiceInfo.isBeatScroll
		val smooth = choiceInfo.isSmoothScroll
		val scrollingMode =
			if (beat) ScrollingMode.Beat else if (smooth) ScrollingMode.Smooth else ScrollingMode.Manual

		val mimicDisplay = scrollingMode === ScrollingMode.Manual && Preferences.mimicBandLeaderDisplay

		// Only use the settings from the ChooseSongMessage if the "mimic band leader display" setting is true.
		// Also, beat and smooth scrolling should never mimic.
		val nativeSettings = getSongDisplaySettings(scrollingMode)
		val sourceSettings = if (mimicDisplay) DisplaySettings(choiceInfo) else nativeSettings

		for (sf in Cache.cachedCloudItems.songFiles)
			if (sf.normalizedTitle == choiceInfo.normalizedTitle && sf.normalizedArtist == choiceInfo.normalizedArtist) {
				val songLoadInfo = SongLoadInfo(
					sf,
					choiceInfo.variation,
					scrollingMode,
					"",
					wasStartedByBandLeader = true,
					wasStartedByMidiTrigger = false,
					nativeSettings,
					sourceSettings,
					choiceInfo.noAudio,
					choiceInfo.audioLatency,
					0
				)
				val songLoadJob = SongLoadJob(songLoadInfo)
				if (SongDisplayActivity.interruptCurrentSong(songLoadJob) == SongInterruptResult.NoSongToInterrupt)
					playSong(
						PlaylistNode(sf),
						choiceInfo.variation,
						scrollingMode,
						true,
						nativeSettings,
						sourceSettings,
						choiceInfo.noAudio
					)
				break
			}
	}

	internal fun onCacheUpdated(cache: CachedCloudCollection) {
		val listView = requireView().findViewById<ListView>(R.id.listView)
		savedListIndex = listView.firstVisiblePosition
		val v = listView.getChildAt(0)
		savedListOffset = if (v == null) 0 else v.top - listView.paddingTop
		initialiseList(cache)
	}

	internal fun onCacheCleared(report: Boolean) {
		playlist = Playlist()
		buildFilterList(Cache.cachedCloudItems)
		val context = requireContext()
		if (report) {
			Toast.makeText(
				context,
				context.getString(R.string.cache_cleared),
				Toast.LENGTH_LONG
			).show()
		}
	}

	internal fun onTemporarySetListCleared() {
		filters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.clear()
		buildFilterList(Cache.cachedCloudItems)
	}

	private fun showLoadingProgressUI(show: Boolean) {
		requireView().findViewById<LinearLayout>(R.id.songLoadUI).visibility =
			if (show) View.VISIBLE else View.GONE
		if (!show)
			updateLoadingProgress(0, 1)
	}

	private fun updateLoadingProgress(currentProgress: Int, maxProgress: Int) =
		launch {
			requireView().findViewById<ProgressBar>(R.id.loadingProgress).apply {
				max = maxProgress
				progress = currentProgress
			}
		}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		if (key == getString(R.string.pref_storageLocation_key) || key == getString(R.string.pref_useExternalStorage_key))
			Cache.initialiseLocalStorage(requireContext())
		else if (key == getString(R.string.pref_largePrintList_key)
			|| key == getString(R.string.pref_showBeatStyleIcons_key)
			|| key == getString(R.string.pref_showMusicIcon_key)
			|| key == getString(R.string.pref_showKeyInList_key)
		) {
			listAdapter = buildListAdapter()
			updateListView()
		}
	}

	override fun onQueryTextSubmit(searchText: String?): Boolean = true

	override fun onQueryTextChange(searchText: String?): Boolean {
		this.searchText = searchText?.lowercase() ?: ""
		listAdapter = buildListAdapter()
		updateListView()
		return true
	}

	private fun filterMIDIAliasFiles(fileList: List<MIDIAliasFile>): List<MIDIAliasFile> {
		return fileList.filter {
			it.file != Cache.defaultMidiAliasesFile &&
				(searchText.isBlank() || it.normalizedName.contains(searchText))
		}
	}

	private fun filterPlaylistNodes(playlist: Playlist): List<PlaylistNode> =
		playlist.nodes.filter {
			searchText.isBlank() ||
				it.songFile.normalizedArtist.contains(searchText) ||
				it.songFile.normalizedTitle.contains(searchText)
		}

	companion object {
		var mSongListEventHandler: SongListEventHandler? = null
		var mSongEndedNaturally = false

		lateinit var mSongListInstance: SongListFragment
	}

	class SongListEventHandler internal constructor(private val songList: SongListFragment) :
		Handler() {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.BLUETOOTH_CHOOSE_SONG -> songList.processBluetoothChooseSongMessage(msg.obj as SongChoiceInfo)
				Events.CLOUD_SYNC_ERROR -> {
					AlertDialog.Builder(songList.context).apply {
						setMessage(
							BeatPrompter.appResources.getString(
								R.string.cloudSyncErrorMessage,
								msg.obj as String
							)
						)
						setTitle(BeatPrompter.appResources.getString(R.string.cloudSyncErrorTitle))
						setPositiveButton("OK") { dialog, _ -> dialog.cancel() }
						create().apply {
							setCanceledOnTouchOutside(true)
							show()
						}
					}
				}

				Events.MIDI_PROGRAM_CHANGE -> {
					val bytes = msg.obj as ByteArray
					songList.startSongViaMidiProgramChange(bytes[0], bytes[1], bytes[2], bytes[3])
				}

				Events.MIDI_SONG_SELECT -> songList.startSongViaMidiSongSelect(msg.arg1.toByte())
				Events.CACHE_UPDATED -> {
					BeatPrompter.addDebugMessage("CACHE_UPDATED received")
					val cache = msg.obj as CachedCloudCollection
					songList.onCacheUpdated(cache)
				}

				Events.CONNECTION_ADDED -> {
					Toast.makeText(
						songList.context,
						BeatPrompter.appResources.getString(R.string.connection_added, msg.obj.toString()),
						Toast.LENGTH_LONG
					).show()
					songList.updateBluetoothIcon()
				}

				Events.CONNECTION_LOST -> {
					Logger.log("Lost connection to device.")
					Toast.makeText(
						songList.context,
						BeatPrompter.appResources.getString(R.string.connection_lost, msg.obj.toString()),
						Toast.LENGTH_LONG
					).show()
					songList.updateBluetoothIcon()
				}

				Events.SONG_LOAD_CANCELLED -> {
					if (!SongLoadQueueWatcherTask.isLoadingASong && !SongLoadQueueWatcherTask.hasASongToLoad)
						songList.showLoadingProgressUI(false)
				}

				Events.SONG_LOAD_FAILED -> {
					songList.showLoadingProgressUI(false)
					Toast.makeText(songList.context, msg.obj.toString(), Toast.LENGTH_LONG).show()
				}

				Events.SONG_LOAD_COMPLETED -> {
					Logger.logLoader({ "Song ${msg.obj} was fully loaded successfully." })
					songList.showLoadingProgressUI(false)
					// No point starting up the activity if there are songs in the load queue
					if (SongLoadQueueWatcherTask.hasASongToLoad || SongLoadQueueWatcherTask.isLoadingASong)
						Logger.logLoader("Abandoning loaded song: there appears to be another song incoming.")
					else
						songList.startSongActivity(msg.obj as UUID)
				}

				Events.SONG_LOAD_LINE_PROCESSED -> songList.updateLoadingProgress(msg.arg1, msg.arg2)

				Events.CACHE_CLEARED -> songList.onCacheCleared(msg.obj as Boolean)
				Events.TEMPORARY_SET_LIST_CLEARED -> songList.onTemporarySetListCleared()

				Events.DATABASE_READ_ERROR, Events.DATABASE_WRITE_ERROR -> {
					Toast.makeText(
						songList.context,
						if (msg.what == Events.DATABASE_READ_ERROR) BeatPrompter.appResources.getString(R.string.database_read_error) else BeatPrompter.appResources.getString(
							R.string.database_write_error
						),
						Toast.LENGTH_LONG
					).show()
				}
			}
		}
	}
}
