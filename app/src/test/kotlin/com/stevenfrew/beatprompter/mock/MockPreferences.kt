package com.stevenfrew.beatprompter.mock

import android.content.SharedPreferences
import com.stevenfrew.beatprompter.audio.AudioPlayerType
import com.stevenfrew.beatprompter.cache.parse.ShowBPMContext
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.midi.ConnectionType
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.preferences.Preferences
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.ui.SongIconDisplayPosition
import com.stevenfrew.beatprompter.ui.SongView
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.ui.pref.SortingPreference

class MockPreferences(
	override val midiConnectionTypes: Set<ConnectionType> = setOf(),
	override val alwaysDisplaySharpChords: Boolean = true,
	override val displayUnicodeAccidentals: Boolean = true,
	override val bluetoothMidiDevices: Set<String> = setOf(),
	override var darkMode: Boolean = true,
	override val defaultTrackVolume: Int = 66,
	override val defaultMIDIOutputChannel: Int = 1,
	override val defaultHighlightColor: Int = 0x00445566,
	override val bandLeaderDevice: String = "",
	override val preferredVariation: String = "",
	override val bluetoothMode: BluetoothMode = BluetoothMode.None,
	override val incomingMIDIChannels: Int = 65535,
	override var cloudDisplayPath: String = "/",
	override var cloudPath: String = "/",
	override val includeSubFolders: Boolean = true,
	override var firstRun: Boolean = false,
	override val manualMode: Boolean = false,
	override val mute: Boolean = false,
	override var sorting: Array<out SortingPreference> = arrayOf(SortingPreference.Title),
	override val defaultCountIn: Int = 0,
	override val audioLatency: Int = 0,
	override val sendMIDIClock: Boolean = false,
	override val customCommentsUser: String = "",
	override val showChords: Boolean = true,
	override val showKey: Boolean = false,
	override val showBPMContext: ShowBPMContext = ShowBPMContext.No,
	override val sendMIDITriggerOnStart: TriggerOutputContext = TriggerOutputContext.Never,
	override val metronomeContext: MetronomeContext = MetronomeContext.Off,
	override val lyricColor: Int = 0x00000000,
	override val chordColor: Int = 0x00ff0000,
	override val chorusHighlightColor: Int = 0x00000000,
	override val annotationColor: Int = 0x00000000,
	override val largePrint: Boolean = false,
	override val proximityScroll: Boolean = false,
	override val anyOtherKeyPageDown: Boolean = false,
	override var storageSystem: StorageType = StorageType.Local,
	override val onlyUseBeatFontSizes: Boolean = false,
	override val minimumBeatFontSize: Int = 8,
	override val maximumBeatFontSize: Int = 80,
	override val minimumSmoothFontSize: Int = 8,
	override val maximumSmoothFontSize: Int = 80,
	override val minimumManualFontSize: Int = 8,
	override val maximumManualFontSize: Int = 80,
	override val useExternalStorage: Boolean = false,
	override val mimicBandLeaderDisplay: Boolean = true,
	override val playNextSong: String = "",
	override val showBeatStyleIcons: Boolean = true,
	override val showKeyInSongList: Boolean = true,
	override val showRatingInSongList: Boolean = true,
	override val showYearInSongList: Boolean = true,
	override val songIconDisplayPosition: SongIconDisplayPosition = SongIconDisplayPosition.Left,
	override val showMusicIcon: Boolean = true,
	override val screenAction: SongView.ScreenAction = SongView.ScreenAction.Scroll,
	override val audioPlayer: AudioPlayerType = AudioPlayerType.ExoPlayer,
	override val showScrollIndicator: Boolean = true,
	override val showSongTitle: Boolean = false,
	override val commentDisplayTime: Int = 3,
	override val midiTriggerSafetyCatch: SongView.TriggerSafetyCatch = SongView.TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine,
	override val highlightCurrentLine: Boolean = true,
	override val showPageDownMarker: Boolean = true,
	override val clearTagsOnFolderChange: Boolean = true,
	override val highlightBeatSectionStart: Boolean = true,
	override val beatCounterColor: Int = 0x0000ff00,
	override val commentColor: Int = 0x00000000,
	override val scrollIndicatorColor: Int = 0x00000000,
	override val beatSectionStartHighlightColor: Int = 0x00000000,
	override val currentLineHighlightColor: Int = 0x00000000,
	override val pageDownMarkerColor: Int = 0x00000000,
	override val pulseDisplay: Boolean = true,
	override val backgroundColor: Int = 0x00000000,
	override val pulseColor: Int = 0x00000000,
	override var dropboxAccessToken: String = "",
	override var dropboxRefreshToken: String = "",
	override var dropboxExpiryTime: Long = Long.MAX_VALUE,
	override val trimTrailingPunctuation: Boolean = false,
	override val useUnicodeEllipsis: Boolean = false
) : Preferences {
	override fun getStringPreference(key: String, default: String): String {
		TODO("Not yet implemented")
	}

	override fun getStringSetPreference(key: String, default: Set<String>): Set<String> {
		TODO("Not yet implemented")
	}

	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		TODO("Not yet implemented")
	}

	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
		TODO("Not yet implemented")
	}
}