package com.stevenfrew.beatprompter.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Point
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
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.BuildConfig
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.CachedCloudCollection
import com.stevenfrew.beatprompter.cache.MidiAliasFile
import com.stevenfrew.beatprompter.cache.ReadCacheTask
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.ContentParsingError
import com.stevenfrew.beatprompter.chord.ChordMap
import com.stevenfrew.beatprompter.chord.KeySignature
import com.stevenfrew.beatprompter.chord.KeySignatureDefinition
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.midi.Midi
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.Rect
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.midi.CommandTrigger
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.set.Playlist
import com.stevenfrew.beatprompter.set.PlaylistNode
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.song.UltimateGuitarSongInfo
import com.stevenfrew.beatprompter.song.load.SongChoiceInfo
import com.stevenfrew.beatprompter.song.load.SongInterruptResult
import com.stevenfrew.beatprompter.song.load.SongLoadInfo
import com.stevenfrew.beatprompter.song.load.SongLoadJob
import com.stevenfrew.beatprompter.song.load.SongLoadQueueWatcherTask
import com.stevenfrew.beatprompter.storage.EditableStorage
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.ui.filter.AllSongsFilter
import com.stevenfrew.beatprompter.ui.filter.Filter
import com.stevenfrew.beatprompter.ui.filter.FilterComparator
import com.stevenfrew.beatprompter.ui.filter.FolderFilter
import com.stevenfrew.beatprompter.ui.filter.MidiAliasFilesFilter
import com.stevenfrew.beatprompter.ui.filter.MidiCommandsFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFileFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFilter
import com.stevenfrew.beatprompter.ui.filter.SongFilter
import com.stevenfrew.beatprompter.ui.filter.TagFilter
import com.stevenfrew.beatprompter.ui.filter.TemporarySetListFilter
import com.stevenfrew.beatprompter.ui.filter.UltimateGuitarFilter
import com.stevenfrew.beatprompter.ui.filter.VariationFilter
import com.stevenfrew.beatprompter.ui.pref.SettingsActivity
import com.stevenfrew.beatprompter.ui.pref.SortingPreference
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.bestScrollingMode
import com.stevenfrew.beatprompter.util.execute
import com.stevenfrew.beatprompter.util.flattenAll
import com.stevenfrew.beatprompter.util.normalize
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

	private var menu: Menu? = null

	private var playlist = Playlist()
	private var nowPlayingNode: PlaylistNode? = null
	private var searchText = ""
	private var maintainedListPositions: Pair<Int, Int>? = null
	private var filters = listOf<Filter>()
	private val selectedTagFilters = mutableListOf<TagFilter>()
	private val selectedVariationFilters = mutableListOf<VariationFilter>()
	private var selectedFilter: Filter = AllSongsFilter(mutableListOf())
	private var imageDictionary: Map<String, Bitmap> = mapOf()
	private var missingIconBitmap: android.graphics.Bitmap? = null

	private val songLauncher: ActivityResultLauncher<Intent> =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK)
				startNextSong()
		}

	private fun triggerMidiCommands(commandTrigger: CommandTrigger) =
		Cache.cachedCloudItems.midiAliasSets.flatMap { set ->
			set.aliases.filter { alias ->
				alias.triggers.any {
					it == commandTrigger
				}
			}
		}.forEach {
			executeMidiCommand(it)
		}

	private fun executeMidiCommand(alias: Alias) {
		val (messages, _) = alias.resolve(
			Cache.cachedCloudItems.defaultMidiAliasSet,
			Cache.cachedCloudItems.midiAliasSets,
			byteArrayOf(),
			MidiMessage.getChannelFromBitmask(BeatPrompter.preferences.defaultMIDIOutputChannel)
		)
		Midi.putMessages(messages)
	}

	override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		val adapter = parent.adapter as ArrayAdapter<*>
		if (selectedFilter is MidiAliasFilesFilter) {
			val maf = adapter.getItem(position) as MidiAliasFile
			if (maf.errors.isNotEmpty())
				showMIDIAliasErrors(maf.errors)
		} else if (selectedFilter is MidiCommandsFilter) {
			val alias = adapter.getItem(position) as Alias
			executeMidiCommand(alias)
			Toast.makeText(
				context,
				BeatPrompter.appResources.getString(
					R.string.executed_midi_command,
					alias.commandName ?: alias.name
				),
				Toast.LENGTH_SHORT
			).show()
		} else if (selectedFilter is UltimateGuitarFilter) {
			val songToLoad = adapter.getItem(position) as PlaylistNode
			if (songToLoad.songInfo is UltimateGuitarSongInfo && !SongLoadQueueWatcherTask.isAlreadyLoadingSong(
					songToLoad.songInfo
				)
			)
				playPlaylistNode(songToLoad, false)

		} else {
			val songToLoad = adapter.getItem(position) as PlaylistNode
			if (!SongLoadQueueWatcherTask.isAlreadyLoadingSong(songToLoad.songInfo))
				playPlaylistNode(songToLoad, false)
		}
	}

	internal fun startSongActivity(loadID: UUID) {
		val intent = Intent(context, SongDisplayActivity::class.java)
		intent.putExtra("loadID", ParcelUuid(loadID))
		Logger.logLoader({ "Starting SongDisplayActivity for $loadID!" })
		songLauncher.launch(intent)
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
		val bluetoothMode = BeatPrompter.preferences.bluetoothMode
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
		if (BeatPrompter.preferences.clearTagsOnFolderChange) {
			selectedTagFilters.clear()
			selectedVariationFilters.clear()
		}
		applyFileFilter(filters[position])
		maintainedListPositions?.also {
			val listView = requireView().findViewById<ListView>(R.id.listView)
			listView.setSelectionFromTop(it.first, it.second)
		}
		maintainedListPositions = null
	}

	private fun applyFileFilter(filter: Filter) {
		val sameFilterAsBefore = selectedFilter.name == filter.name
		selectedFilter = filter
		playlist = if (filter is SongFilter) {
			val isAllSongsFilter = filter is AllSongsFilter
			val tagFiltersSelected = selectedTagFilters.isNotEmpty()
			val variationFiltersSelected = selectedVariationFilters.isNotEmpty()
			Playlist(filter.songs.filter {
				val songInfo = it.first
				if (tagFiltersSelected || variationFiltersSelected)
					(tagFiltersSelected && selectedTagFilters.any { filter -> filter.songs.contains(it) }) ||
						(variationFiltersSelected && selectedVariationFilters.any { filter ->
							filter.songs.contains(it)
						})
				else if (isAllSongsFilter && songInfo is SongFile)
					!Cache.cachedCloudItems.isFilterOnly(songInfo)
				else true
			})
		} else
			Playlist()
		sortSongList()
		if (sameFilterAsBefore)
			maintainListPosition {
				buildListAdapter()
			}
		else
			updateListView(buildListAdapter())
		showSetListMissingSongs()
	}

	override fun onNothingSelected(parent: AdapterView<*>) {
		//applyFileFilter(null)
	}

	private fun maintainListPosition(buildAdapterFn: () -> BaseAdapter) {
		val maintainedPositions = getListPositions()
		buildAdapterFn().also {
			updateListView(it).setSelectionFromTop(maintainedPositions.first, maintainedPositions.second)
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration) =
		maintainListPosition {
			super.onConfigurationChanged(newConfig)
			registerEventHandler()
			buildListAdapter()
		}

	private fun updateListView(baseAdapter: BaseAdapter): ListView =
		requireView().findViewById<ListView>(R.id.listView).apply {
			onItemClickListener = this@SongListFragment
			onItemLongClickListener = this@SongListFragment
			adapter = baseAdapter
		}

	private fun setFilters(initialSelection: Int = 0) {
		val spinnerLayout = menu?.findItem(R.id.tagspinnerlayout)?.actionView as? LinearLayout
		spinnerLayout?.findViewById<Spinner>(R.id.tagspinner)?.apply {
			onItemSelectedListener = this@SongListFragment
			adapter = FilterListAdapter(
				filters,
				selectedTagFilters,
				selectedVariationFilters,
				requireActivity()
			) {
				applyFileFilter(selectedFilter)
			}
			setSelection(initialSelection)
		}
	}

	private fun startSongViaMidiSongTrigger(mst: SongTrigger) {
		for (node in playlist.nodes)
			if (node.songInfo.matchesTrigger(mst)) {
				Logger.log({ "Found trigger match: '${node.songInfo.title}'." })
				playPlaylistNode(node, true)
				return
			}
		// Otherwise, it might be a song that is not currently onscreen.
		// Still play it though!
		for (sf in Cache.cachedCloudItems.songFiles)
			if (sf.matchesTrigger(mst)) {
				Logger.log({ "Found trigger match: '${sf.title}'." })
				playSong(sf, PlaylistNode(sf), true)
			}
	}

	private fun playPlaylistNode(node: PlaylistNode, startedByMidiTrigger: Boolean) {
		val selectedSong = node.songInfo
		playSong(selectedSong, node, startedByMidiTrigger)
	}

	private fun playSong(
		selectedSong: SongInfo,
		node: PlaylistNode,
		startedByMidiTrigger: Boolean
	) {
		val mute = BeatPrompter.preferences.mute
		val manualMode = BeatPrompter.preferences.manualMode
		val mode =
			if (manualMode)
				ScrollingMode.Manual
			else
				selectedSong.bestScrollingMode
		val sds = getSongDisplaySettings(mode)
		val noAudioWhatsoever = manualMode || mute
		playSong(
			node,
			mode,
			startedByMidiTrigger,
			sds,
			sds,
			noAudioWhatsoever,
			0
		)
	}

	private fun shouldPlayNextSong(): Boolean =
		when (BeatPrompter.preferences.playNextSong) {
			getString(R.string.playNextSongAlwaysValue) -> true
			getString(R.string.playNextSongSetListsOnlyValue) -> selectedFilter is SetListFilter
			else -> false
		}

	private fun getSongDisplaySettings(songScrollMode: ScrollingMode): DisplaySettings {
		val onlyUseBeatFontSizes = BeatPrompter.preferences.onlyUseBeatFontSizes

		val minimumFontSizeBeat = BeatPrompter.preferences.minimumBeatFontSize
		val maximumFontSizeBeat = BeatPrompter.preferences.maximumBeatFontSize
		val minimumFontSizeSmooth =
			if (onlyUseBeatFontSizes) minimumFontSizeBeat else BeatPrompter.preferences.minimumSmoothFontSize
		val maximumFontSizeSmooth =
			if (onlyUseBeatFontSizes) maximumFontSizeBeat else BeatPrompter.preferences.maximumSmoothFontSize
		val minimumFontSizeManual =
			if (onlyUseBeatFontSizes) minimumFontSizeBeat else BeatPrompter.preferences.minimumManualFontSize
		val maximumFontSizeManual =
			if (onlyUseBeatFontSizes) maximumFontSizeBeat else BeatPrompter.preferences.maximumManualFontSize

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
		scrollMode: ScrollingMode,
		startedByMidiTrigger: Boolean,
		nativeSettings: DisplaySettings,
		sourceSettings: DisplaySettings,
		noAudio: Boolean,
		transposeShift: Int
	) {
		showLoadingProgressUI(true)
		nowPlayingNode = selectedNode

		val nextSongName =
			if (selectedNode.nextSong != null && shouldPlayNextSong()) selectedNode.nextSong.songInfo.title else ""
		val songLoadInfo = SongLoadInfo(
			selectedNode.songInfo,
			if (selectedNode.variation.isNullOrBlank()) selectedNode.songInfo.defaultVariation else selectedNode.variation,
			scrollMode,
			nativeSettings,
			sourceSettings,
			nextSongName,
			false,
			startedByMidiTrigger,
			noAudio,
			BeatPrompter.preferences.audioLatency,
			transposeShift
		)
		val songLoadJob = SongLoadJob(songLoadInfo)
		SongLoadQueueWatcherTask.loadSong(songLoadJob)
	}

	private fun addToTemporarySet(song: SongInfo) {
		filters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()?.addSong(song)
		try {
			Cache.initialiseTemporarySetListFile(false, requireContext())
			Utils.appendToTextFile(Cache.temporarySetListFile!!, SetListEntry(song).toString())
		} catch (ioe: IOException) {
			Toast.makeText(context, ioe.message, Toast.LENGTH_LONG).show()
		}
	}

	private fun copyUltimateGuitarChordProToClipboard(songInfo: UltimateGuitarSongInfo) =
		Thread(
			UltimateGuitarChordProContentCopyToClipboardTask(
				getSystemService(
					requireContext(),
					ClipboardManager::class.java
				), songInfo
			)
		).start()

	private fun onSongListLongClick(position: Int, parentAdapterView: AdapterView<*>) {
		val adapter = parentAdapterView.adapter
		val selectedNode = adapter.getItem(position) as PlaylistNode
		val selectedSongInfo = selectedNode.songInfo
		val selectedSet =
			if (selectedFilter is SetListFileFilter) (selectedFilter as SetListFileFilter).setListFile else null
		val tempSetListFilter =
			filters.asSequence().filterIsInstance<TemporarySetListFilter>().firstOrNull()
		val isUgSearchNode = selectedSongInfo is UltimateGuitarSearchStatusNode

		val addAllowed = !isUgSearchNode &&
			if (tempSetListFilter != null)
				if (selectedFilter !== tempSetListFilter)
					!tempSetListFilter.containsSong(selectedSongInfo)
				else
					false
			else
				true
		val includeRefreshSet = selectedSet != null && selectedFilter !== tempSetListFilter
		val includeClearSet = selectedFilter === tempSetListFilter

		val options = mutableListOf<Pair<Int, () -> Unit>>()
		if (!isUgSearchNode)
			options.add(R.string.play_submenu to { showPlayDialog(selectedNode, selectedSongInfo) })

		if (selectedSongInfo is SongFile) {
			options.add(R.string.force_refresh to {
				Cache.performCloudSync(selectedSongInfo, false, this@SongListFragment)
			})
			options.add(R.string.force_refresh_with_dependencies to {
				Cache.performCloudSync(selectedSongInfo, true, this@SongListFragment)
			})
			if (includeRefreshSet)
				options.add(R.string.force_refresh_set to {
					Cache.performCloudSync(selectedSet, false, this@SongListFragment)
				})
		} else if (selectedSongInfo is UltimateGuitarSongInfo)
			options.add(R.string.copy_ug_to_clipboard to {
				copyUltimateGuitarChordProToClipboard(selectedSongInfo)
			})
		if (includeClearSet)
			options.add(R.string.clear_set to { Cache.clearTemporarySetList(requireContext()) })
		if (addAllowed)
			options.add(R.string.add_to_temporary_set to { addToTemporarySet(selectedSongInfo) })
		val storage = Storage.getInstance(BeatPrompter.preferences.storageSystem, this)
		if (selectedSongInfo is SongFile) {
			if (storage is EditableStorage) {
				options.add(R.string.edit_file to { startActivity(storage.getEditIntent(selectedSongInfo.id)) })
				if (selectedSet != null)
					options.add(R.string.edit_set_file to { startActivity(storage.getEditIntent(selectedSet.id)) })
			}
		}

		if (options.any()) {
			val optionStrings = options.map { BeatPrompter.appResources.getString(it.first) }
			AlertDialog.Builder(context).apply {
				setTitle(R.string.song_options)
				setItems(optionStrings.toTypedArray()) { _, which -> options[which].second() }
				create().apply {
					setCanceledOnTouchOutside(true)
					show()
				}
			}
		}
	}

	private fun showPlayDialog(
		selectedNode: PlaylistNode,
		selectedSong: SongInfo
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
		val transposeOptions =
			TransposeOption.getTransposeOptions(selectedSong.keySignature, selectedSong.firstChord)

		val transposeSpinner = view
			.findViewById<Spinner>(R.id.transposeSpinner)
		val noTranspose = transposeOptions.isEmpty() || !BeatPrompter.preferences.showChords
		if (noTranspose) {
			view.findViewById<TextView>(R.id.transposeLabel).visibility = View.GONE
			transposeSpinner.visibility = View.GONE
		} else {
			val transposeSpinnerAdapter = ArrayAdapter(
				requireContext(),
				android.R.layout.simple_spinner_item,
				transposeOptions
			)
			transposeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
			transposeSpinner.adapter = transposeSpinnerAdapter
		}
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
			transposeSpinner.setSelection(ChordMap.NUMBER_OF_KEYS - 1)
			// Add action buttons
			setPositiveButton(R.string.play) { _, _ ->
				val selectedVariation = variationSpinner.selectedItem as String
				val selectedTranspose =
					if (noTranspose) null else transposeSpinner.selectedItem as TransposeOption
				val noAudio = noAudioCheckbox.isChecked
				val mode =
					when {
						beatButton.isChecked -> ScrollingMode.Beat
						smoothButton.isChecked -> ScrollingMode.Smooth
						else -> ScrollingMode.Manual
					}
				val sds = getSongDisplaySettings(mode)
				playSong(
					PlaylistNode(selectedNode.songInfo, selectedVariation, selectedNode.nextSong),
					mode,
					false,
					sds,
					sds,
					noAudio,
					selectedTranspose?.offset ?: 0
				)
			}
			setNegativeButton(R.string.cancel) { _, _ -> }
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun onMIDIAliasListLongClick(position: Int, parentAdapterView: AdapterView<*>) {
		val adapter = parentAdapterView.adapter
		val maf = adapter.getItem(position) as MidiAliasFile
		val showErrors = maf.errors.isNotEmpty()

		val options = mutableListOf<Pair<Int, () -> Unit>>()
		options.add(R.string.force_refresh_midi_alias to {
			Cache.performCloudSync(maf, false, this@SongListFragment)
		})
		if (showErrors)
			options.add(R.string.show_midi_alias_errors to { showMIDIAliasErrors(maf.errors) })
		val storage = Storage.getInstance(BeatPrompter.preferences.storageSystem, this)
		if (storage is EditableStorage)
			options.add(R.string.edit_file to { startActivity(storage.getEditIntent(maf.id)) })

		val optionStrings = options.map { BeatPrompter.appResources.getString(it.first) }
		AlertDialog.Builder(context).apply {
			setTitle(R.string.midi_alias_list_options)
			setItems(optionStrings.toTypedArray()) { _, which -> options[which].second() }
			create().apply {
				setCanceledOnTouchOutside(true)
				show()
			}
		}
	}

	private fun showMIDIAliasErrors(errors: List<ContentParsingError>) {
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
		if (selectedFilter is MidiAliasFilesFilter)
			onMIDIAliasListLongClick(position, parent)
		else if (selectedFilter !is MidiCommandsFilter)
			onSongListLongClick(position, parent)
		return true
	}

	private fun registerEventHandler() {
		mSongListInstance = this
		mSongListEventHandler = SongListEventHandler(this)
		// Now ready to receive events.
		EventRouter.addSongListEventHandler(tag!!, mSongListEventHandler!!)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val activity = requireActivity()
		activity.addMenuProvider(MenuProvider())
		missingIconBitmap = BitmapFactory.decodeResource(
			activity.resources,
			R.drawable.ic_missing
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		registerEventHandler()

		BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(this)

		Cache.initialiseLocalStorage(requireContext())

		val firstRun = BeatPrompter.preferences.firstRun
		if (firstRun) {
			BeatPrompter.preferences.firstRun = false
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
			BeatPrompter.preferences.storageSystem = StorageType.Demo
			BeatPrompter.preferences.cloudPath = "/"
			Cache.performFullCloudSync(this)
		}
	}

	internal fun onCacheUpdated(cache: CachedCloudCollection) {
		maintainedListPositions = getListPositions()
		imageDictionary = buildImageDictionary(cache)
		playlist = Playlist()
		buildFilterList(cache)
	}

	override fun onDestroy() {
		BeatPrompter.preferences.unregisterOnSharedPreferenceChangeListener(this)
		EventRouter.removeSongListEventHandler(tag!!)
		super.onDestroy()
	}

	override fun onResume() {
		super.onResume()

		updateBluetoothIcon()

		val listView = requireView().findViewById<ListView>(R.id.listView)
		(listView?.adapter as? BaseAdapter)?.notifyDataSetChanged()

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
			val sorting = BeatPrompter.preferences.sorting
			sorting.forEach {
				playlist = when (it) {
					SortingPreference.Date -> playlist.sortByDateModified()
					SortingPreference.Artist -> playlist.sortByArtist()
					SortingPreference.Title -> playlist.sortByTitle()
					SortingPreference.Mode -> playlist.sortByMode()
					SortingPreference.Rating -> playlist.sortByRating()
					SortingPreference.Key -> playlist.sortByKey()
					SortingPreference.Year -> playlist.sortByYear()
					SortingPreference.Icon -> playlist.sortByIcon()
				}
			}
		}
	}

	private fun shuffleSongList() {
		playlist = playlist.shuffle()
		updateListView(buildListAdapter())
	}

	private fun buildListAdapter(): BaseAdapter =
		requireActivity().let { context ->
			when (selectedFilter) {
				is MidiAliasFilesFilter ->
					MidiAliasListAdapter(
						Cache.cachedCloudItems.midiAliasFiles.filter {
							it.file != Cache.defaultMidiAliasesFile &&
								(searchText.isBlank() || it.normalizedName.contains(searchText))
						}.sortedBy { it.name },
						context
					)

				is MidiCommandsFilter ->
					MidiCommandListAdapter(
						Cache.cachedCloudItems.midiCommands.filter {
							searchText.isBlank() || it.commandName?.contains(searchText) == true
						}.sortedBy { it.name },
						context
					)

				is UltimateGuitarFilter ->
					UltimateGuitarListAdapter(
						searchText,
						mSongListEventHandler!!,
						mutableListOf(),
						context
					)

				else ->
					SongListAdapter(
						playlist.nodes.filter {
							searchText.isBlank() ||
								it.songInfo.normalizedArtist.contains(searchText) ||
								it.songInfo.normalizedTitle.contains(searchText)
						},
						imageDictionary,
						missingIconBitmap!!,
						context
					)
			}
		}

	private fun buildFilterList(cache: CachedCloudCollection) {
		Logger.log("Building tag list ...")
		val lastSelectedFilter = selectedFilter
		val songFilters = mutableListOf<Filter>()

		// Create filters from song tags, variations and sub-folders. Many songs can share the same
		// tag/variation/subfolder, so a bit of clever collection management is required here.
		val tagDictionaries = HashMap<String, MutableList<SongFile>>()
		cache.songFiles.forEach {
			it.tags.forEach { tag -> tagDictionaries.getOrPut(tag) { mutableListOf() }.add(it) }
		}

		val variationDictionaries = HashMap<String, MutableList<SongFile>>()
		val defaultVariationName = BeatPrompter.appResources.getString(R.string.defaultVariationName)
		if (BeatPrompter.preferences.includeVariationsInFilterList)
			cache.songFiles.forEach {
				val audioFilenamesLowerCase =
					it.audioFiles.values.flatten().map { filename -> filename.normalize() }.toSet()
				it.variations
					// Don't include variations that are just audio filenames
					// or the Default variation
					.filter { variation ->
						variation.isNotBlank() &&
							variation != defaultVariationName &&
							!audioFilenamesLowerCase.contains(variation.normalize())
					}
					.forEach { variation ->
						variationDictionaries.getOrPut(variation) { mutableListOf() }.add(it)
					}
			}

		val folderDictionaries = HashMap<String, List<SongFile>>()
		cache.folders.forEach {
			cache.getSongsInFolder(it).let { songList ->
				if (songList.isNotEmpty())
					folderDictionaries[it.name] = songList
			}
		}

		tagDictionaries.forEach {
			songFilters.add(TagFilter(it.key, it.value))
		}
		variationDictionaries.forEach {
			songFilters.add(VariationFilter(it.key, it.value))
		}
		folderDictionaries.forEach {
			songFilters.add(FolderFilter(it.key, it.value))
		}
		songFilters.addAll(
			cache.setListFiles.mapNotNull {
				if (it.file != Cache.temporarySetListFile)
					SetListFileFilter(it, cache.songFiles)
				else
					null
			}
		)
		songFilters.sortBy { it.name.lowercase() }

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
				MidiAliasFilesFilter()
			else
				null

		// Same thing for MIDI commands ... if there aren't any, don't bother creating a filter.
		val midiCommandsFilter =
			if (cache.midiCommands.isNotEmpty())
				MidiCommandsFilter()
			else
				null

		// Add the UG filter.
		val ultimateGuitarFilter = UltimateGuitarFilter()

		// Now bundle them altogether into one list.
		filters = listOf(
			allSongsFilter,
			tempSetListFilter,
			songFilters,
			midiAliasFilesFilter,
			midiCommandsFilter,
			ultimateGuitarFilter
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

	private fun createAllSongsFilter(cache: CachedCloudCollection): Filter = AllSongsFilter(
		cache.songFiles.toList()
	)

	private fun findFilter(filter: Filter, cache: CachedCloudCollection): Filter =
		filters.find { it == filter } ?: filters.find { it is AllSongsFilter } ?: createAllSongsFilter(
			cache
		)

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
					getString(R.string.byYear),
					getString(R.string.byIcon),
				)
				setItems(items) { d, n ->
					d.dismiss()
					BeatPrompter.preferences.sorting = arrayOf(
						when (n) {
							1 -> SortingPreference.Artist
							2 -> SortingPreference.Date
							3 -> SortingPreference.Key
							4 -> SortingPreference.Mode
							5 -> SortingPreference.Rating
							6 -> SortingPreference.Year
							7 -> SortingPreference.Icon
							else -> SortingPreference.Title
						}
					)
					sortSongList()
					updateListView(buildListAdapter())
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
		val browserIntent = Intent(Intent.ACTION_VIEW, getString(uriResource).toUri())
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

		val mimicDisplay =
			scrollingMode === ScrollingMode.Manual && BeatPrompter.preferences.mimicBandLeaderDisplay

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
					nativeSettings,
					sourceSettings,
					"",
					wasStartedByBandLeader = true,
					wasStartedByMidiTrigger = false,
					choiceInfo.noAudio,
					choiceInfo.audioLatency,
					choiceInfo.transposeShift
				)
				val songLoadJob = SongLoadJob(songLoadInfo)
				if (SongDisplayActivity.interruptCurrentSong(songLoadJob) == SongInterruptResult.NoSongToInterrupt)
					playSong(
						PlaylistNode(sf, choiceInfo.variation),
						scrollingMode,
						true,
						nativeSettings,
						sourceSettings,
						choiceInfo.noAudio,
						choiceInfo.transposeShift
					)
				break
			}
	}

	private fun buildImageDictionary(cache: CachedCloudCollection): Map<String, Bitmap> =
		BeatPrompter.platformUtils.bitmapFactory.let { factory ->
			cache.imageFiles.mapNotNull {
				try {
					it.name to factory.createBitmap(it.file.path)
				} catch (_: Exception) {
					null
				}
			}.toMap()
		}

	private fun getListPositions(): Pair<Int, Int> {
		val listView = requireView().findViewById<ListView>(R.id.listView)
		val index = listView.firstVisiblePosition
		val v = listView.getChildAt(0)
		val offset = if (v == null) 0 else v.top - listView.paddingTop
		return index to offset
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
		when (key) {
			getString(R.string.pref_storageLocation_key),
			getString(R.string.pref_useExternalStorage_key) -> Cache.initialiseLocalStorage(requireContext())

			getString(R.string.pref_largePrintList_key),
			getString(R.string.pref_showBeatStyleIcons_key),
			getString(
				R.string.pref_showMusicIcon_key
			),
			getString(R.string.pref_showKeyInList_key),
			getString(R.string.pref_songIconDisplayPosition_key),
			getString(R.string.pref_showYearInList_key) -> updateListView(buildListAdapter())

			getString(R.string.pref_includeVariationsInFilterList_key) -> buildFilterList(Cache.cachedCloudItems)

			else -> {}
		}
	}

	override fun onQueryTextSubmit(searchText: String?): Boolean = true

	private val queryDebouncer = Debouncer(lifecycleScope)
	override fun onQueryTextChange(searchText: String?): Boolean {
		queryDebouncer.debounce(300L) {
			this.searchText = searchText?.lowercase() ?: ""
			maintainListPosition {
				buildListAdapter()
			}
		}
		return true
	}

	companion object {
		var mSongListEventHandler: SongListEventHandler? = null
		var mSongEndedNaturally = false

		lateinit var mSongListInstance: SongListFragment
	}

	inner class MenuProvider :
		androidx.core.view.MenuProvider {
		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			// Inflate the menu; this adds items to the action bar if it is present.
			menuInflater.inflate(R.menu.songlistmenu, menu)
			if (!BuildConfig.DEBUG) {
				menu.findItem(R.id.debug_log).apply {
					isVisible = false
				}
			}
			(menu.findItem(R.id.search).actionView as SearchView).apply {
				setOnQueryTextListener(this@SongListFragment)
				isSubmitButtonEnabled = false
			}
			this@SongListFragment.menu = menu
			setFilters()
			updateBluetoothIcon()
		}

		override fun onPrepareMenu(menu: Menu) {
			menu.findItem(R.id.sort_songs)?.isEnabled = selectedFilter.canSort
			menu.findItem(R.id.shuffle)?.isEnabled = selectedFilter.canSort
			menu.findItem(R.id.synchronize)?.isEnabled = Cache.canPerformCloudSync()
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
			when (menuItem.itemId) {
				R.id.synchronize -> Cache.performFullCloudSync(this@SongListFragment)
				R.id.shuffle -> shuffleSongList()
				R.id.sort_songs -> showSortDialog()
				R.id.settings -> startActivity(Intent(context, SettingsActivity::class.java))
				R.id.manual -> openManualURL()
				R.id.privacy_policy -> openPrivacyPolicyURL()
				R.id.buy_me_a_coffee -> openBuyMeACoffeeURL()
				R.id.debug_log -> showDebugLog()
				R.id.about -> showAboutDialog()
			}
			return true
		}
	}

	class TransposeOption(val offset: Int, val key: KeySignature?) {
		companion object {
			fun getTransposeOptions(key: String?, firstChord: String?): List<TransposeOption> {
				val keySignature = key?.let { KeySignatureDefinition.getKeySignature(key, firstChord) }
				val options = mutableListOf<TransposeOption>()
				for (offset in -(ChordMap.NUMBER_OF_KEYS - 1)..<ChordMap.NUMBER_OF_KEYS)
					keySignature?.shift(offset)?.also {
						options.add(TransposeOption(offset, it))
					}
				return options
			}
		}

		override fun toString(): String {
			val offsetSign = if (offset > 0) "+" else ""
			if (offset == 0) BeatPrompter.appResources.getString(R.string.none) else "$offset"
			val newKey =
				key?.let { " (${it.getDisplayString(BeatPrompter.preferences.displayUnicodeAccidentals)})" }
					?: ""
			return "$offsetSign$offset$newKey"
		}
	}

	class UltimateGuitarChordProContentCopyToClipboardTask(
		private val clipboardManager: ClipboardManager?,
		private val songInfo: UltimateGuitarSongInfo
	) : Task(true, true) {
		override fun doWork() {
			try {
				if (clipboardManager != null) {
					val content = songInfo.songContentProvider.getContent()
					val clip = ClipData.newPlainText("UG-ChordPro", content)
					clipboardManager.setPrimaryClip(clip)
				} else throw Exception("No clipboard functionality available.")
				EventRouter.sendEventToSongList(Events.COPY_TO_CLIPBOARD_SUCCEEDED)
			} catch (e: Exception) {
				EventRouter.sendEventToSongList(Events.COPY_TO_CLIPBOARD_FAILED, e.message ?: e.toString())
			}
		}
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

				Events.MIDI_CONTROL_CHANGE -> {
					val bytes = msg.obj as ByteArray
					songList.triggerMidiCommands(CommandTrigger(bytes[0], bytes[1], bytes[2]))
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

				Events.COPY_TO_CLIPBOARD_SUCCEEDED ->
					Toast.makeText(
						songList.context,
						BeatPrompter.appResources.getString(R.string.copied),
						Toast.LENGTH_SHORT
					).show()

				Events.COPY_TO_CLIPBOARD_FAILED ->
					Toast.makeText(songList.context, msg.obj.toString(), Toast.LENGTH_LONG).show()

				Events.SONG_LOAD_COMPLETED -> {
					Logger.logLoader({ "Song ${msg.obj} was fully loaded successfully." })
					songList.showLoadingProgressUI(false)
					// No point starting up the activity if there are songs in the load queue
					if (SongLoadQueueWatcherTask.hasASongToLoad)
						Logger.logLoader("Abandoning loaded song: there appears to be another song incoming.")
					else if (SongLoadQueueWatcherTask.isLoadingASong)
						Logger.logLoader("Abandoning loaded song: there appears to be another song already loading.")
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

				Events.SEARCH_ERROR ->
					Toast.makeText(
						songList.context,
						msg.obj.toString(),
						Toast.LENGTH_LONG
					).show()
			}
		}
	}
}
