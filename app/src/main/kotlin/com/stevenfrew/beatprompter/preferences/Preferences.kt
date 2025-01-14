package com.stevenfrew.beatprompter.preferences

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.audio.AudioPlayerType
import com.stevenfrew.beatprompter.cache.parse.ShowBPMContext
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.midi.ConnectionType
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.ui.BeatCounterTextOverlay
import com.stevenfrew.beatprompter.ui.SongIconDisplayPosition
import com.stevenfrew.beatprompter.ui.SongView
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.ui.pref.SortingPreference

interface Preferences {
	val midiConnectionTypes: Set<ConnectionType>
	val alwaysDisplaySharpChords: Boolean
	val displayUnicodeAccidentals: Boolean
	val bluetoothMidiDevices: Set<String>
	var darkMode: Boolean
	val defaultTrackVolume: Int
	val defaultMIDIOutputChannel: Int
	val defaultHighlightColor: Int
	val bandLeaderDevice: String
	val preferredVariation: String
	val bluetoothMode: BluetoothMode
	val incomingMIDIChannels: Int
	var cloudDisplayPath: String
	var cloudPath: String
	val includeSubFolders: Boolean
	var firstRun: Boolean
	val manualMode: Boolean
	val mute: Boolean
	var sorting: Array<out SortingPreference>
	val defaultCountIn: Int
	val audioLatency: Int
	val sendMIDIClock: Boolean
	val customCommentsUser: String
	val showChords: Boolean
	val showKey: Boolean
	val showBPMContext: ShowBPMContext
	val sendMIDITriggerOnStart: TriggerOutputContext
	val metronomeContext: MetronomeContext
	val lyricColor: Int
	val chordColor: Int
	val chorusHighlightColor: Int
	val annotationColor: Int
	val largePrint: Boolean
	val proximityScroll: Boolean
	val anyOtherKeyPageDown: Boolean
	var storageSystem: StorageType
	val onlyUseBeatFontSizes: Boolean
	val minimumBeatFontSize: Int
	val maximumBeatFontSize: Int
	val minimumSmoothFontSize: Int
	val maximumSmoothFontSize: Int
	val minimumManualFontSize: Int
	val maximumManualFontSize: Int
	val useExternalStorage: Boolean
	val mimicBandLeaderDisplay: Boolean
	val playNextSong: String
	val showBeatStyleIcons: Boolean
	val showKeyInSongList: Boolean
	val showRatingInSongList: Boolean
	val showYearInSongList: Boolean
	val songIconDisplayPosition: SongIconDisplayPosition
	val showMusicIcon: Boolean
	val screenAction: SongView.ScreenAction
	val audioPlayer: AudioPlayerType
	val showScrollIndicator: Boolean
	val beatCounterTextOverlay: BeatCounterTextOverlay
	val commentDisplayTime: Int
	val midiTriggerSafetyCatch: SongView.TriggerSafetyCatch
	val highlightCurrentLine: Boolean
	val showPageDownMarker: Boolean
	val clearTagsOnFolderChange: Boolean
	val highlightBeatSectionStart: Boolean
	val beatCounterColor: Int
	val commentColor: Int
	val scrollIndicatorColor: Int
	val beatSectionStartHighlightColor: Int
	val currentLineHighlightColor: Int
	val pageDownMarkerColor: Int
	val pulseDisplay: Boolean
	val backgroundColor: Int
	val pulseColor: Int
	var dropboxAccessToken: String
	var dropboxRefreshToken: String
	var dropboxExpiryTime: Long
	val trimTrailingPunctuation: Boolean
	val useUnicodeEllipsis: Boolean

	fun getStringPreference(key: String, default: String): String
	fun getStringSetPreference(key: String, default: Set<String>): Set<String>

	fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
	fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
}