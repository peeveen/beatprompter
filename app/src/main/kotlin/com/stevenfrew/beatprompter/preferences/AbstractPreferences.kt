package com.stevenfrew.beatprompter.preferences

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.stevenfrew.beatprompter.R
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
import com.stevenfrew.beatprompter.util.GlobalAppResources

abstract class AbstractPreferences(
	private val appResources: GlobalAppResources
) : Preferences {

	override val midiConnectionTypes: Set<ConnectionType>
		get() = try {
			getStringSetPreference(
				R.string.pref_midiConnectionTypes_key,
				appResources.getStringSet(R.array.pref_midiConnectionTypes_defaultValues)
			).map { ConnectionType.valueOf(it) }.toSet()
		} catch (e: Exception) {
			// Backwards compatibility with old shite values from previous app versions.
			setOf(ConnectionType.USBOnTheGo)
		}

	override val alwaysDisplaySharpChords: Boolean
		get() = getBooleanPreference(
			R.string.pref_alwaysDisplaySharpChords_key,
			false
		)

	override val displayUnicodeAccidentals: Boolean
		get() = getBooleanPreference(
			R.string.pref_displayUnicodeAccidentals_key,
			false
		)

	override val bluetoothMidiDevices: Set<String>
		get() = getStringSetPreference(
			R.string.pref_bluetoothMidiDevices_key,
			appResources.getStringSet(R.array.pref_bluetoothMidiDevices_defaultValues)
		).toSet()

	override var darkMode: Boolean
		get() = getBooleanPreference(
			R.string.pref_darkMode_key,
			false
		)
		set(value) {
			setBooleanPreference(R.string.pref_darkMode_key, value)
			AppCompatDelegate.setDefaultNightMode(if (value) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
		}

	override val defaultTrackVolume: Int
		get() = getIntPreference(
			R.string.pref_defaultTrackVolume_key,
			R.string.pref_defaultTrackVolume_default,
			1
		)

	override val defaultMIDIOutputChannel: Int
		get() = getIntPreference(
			R.string.pref_defaultMIDIOutputChannel_key,
			R.string.pref_defaultMIDIOutputChannel_default,
			0
		)

	override val defaultHighlightColor: Int
		get() = getColorPreference(
			R.string.pref_highlightColor_key,
			R.string.pref_highlightColor_default
		)

	override val bandLeaderDevice: String
		get() = getStringPreference(R.string.pref_bandLeaderDevice_key, "")

	override val preferredVariation: String
		get() = getStringPreference(R.string.pref_preferredVariation_key, "")

	override val bluetoothMode: BluetoothMode
		get() = try {
			BluetoothMode.valueOf(
				getStringPreference(
					R.string.pref_bluetoothMode_key,
					R.string.pref_bluetoothMode_defaultValue
				)
			)
		} catch (e: Exception) {
			// Backwards compatibility with old shite values from previous app versions.
			BluetoothMode.None
		}

	override val incomingMIDIChannels: Int
		get() = getIntPreference(R.string.pref_midiIncomingChannels_key, 65535)

	override var cloudDisplayPath: String
		get() = getStringPreference(R.string.pref_cloudDisplayPath_key, "")
		set(value) = setStringPreference(R.string.pref_cloudDisplayPath_key, value)

	override var cloudPath: String
		get() = getStringPreference(R.string.pref_cloudPath_key, "")
		set(value) = setStringPreference(R.string.pref_cloudPath_key, value)

	override val includeSubFolders: Boolean
		get() = getBooleanPreference(R.string.pref_includeSubfolders_key, false)

	override var firstRun: Boolean
		get() = getBooleanPreference(R.string.pref_firstRun_key, true)
		set(value) = setBooleanPreference(R.string.pref_firstRun_key, value)

	override val manualMode: Boolean
		get() = getBooleanPreference(R.string.pref_manualMode_key, false)

	override val mute: Boolean
		get() = getBooleanPreference(R.string.pref_mute_key, false)

	override var sorting: Array<out SortingPreference>
		get() = try {
			val stringPref = getStringPreference(
				R.string.pref_sorting_key,
				SortingPreference.Title.name
			)
			stringPref.split(",").map { SortingPreference.valueOf(it) }.toTypedArray()
		} catch (ignored: Exception) {
			// backward compatibility with old shite values.
			arrayOf(SortingPreference.Title)
		}
		set(value) {
			val newList = sorting.filterNot { value.contains(it) }.toMutableList()
			newList.addAll(value)
			val newString = newList.joinToString(",")
			setStringPreference(R.string.pref_sorting_key, newString)
		}

	override val defaultCountIn: Int
		get() = getIntPreference(R.string.pref_countIn_key, R.string.pref_countIn_default, 0)

	override val audioLatency: Int
		get() = getIntPreference(R.string.pref_audioLatency_key, R.string.pref_countIn_default, 0)

	override val sendMIDIClock: Boolean
		get() = getBooleanPreference(R.string.pref_sendMidi_key, false)

	override val customCommentsUser: String
		get() = getStringPreference(
			R.string.pref_customComments_key,
			R.string.pref_customComments_defaultValue
		)

	override val showChords: Boolean
		get() = getBooleanPreference(
			R.string.pref_showChords_key,
			R.string.pref_showChords_defaultValue
		)

	override val showKey: Boolean
		get() = getBooleanPreference(
			R.string.pref_showSongKey_key,
			R.string.pref_showSongKey_defaultValue
		)

	override val showBPMContext: ShowBPMContext
		get() = try {
			ShowBPMContext.valueOf(
				getStringPreference(
					R.string.pref_showSongBPM_key,
					R.string.pref_showSongBPM_defaultValue
				)
			)
		} catch (_: Exception) {
			// backward compatibility with old shite values.
			ShowBPMContext.No
		}

	override val sendMIDITriggerOnStart: TriggerOutputContext
		get() = try {
			TriggerOutputContext.valueOf(
				getStringPreference(
					R.string.pref_sendMidiTriggerOnStart_key,
					R.string.pref_sendMidiTriggerOnStart_defaultValue
				)
			)
		} catch (_: Exception) {
			// backward compatibility with old shite values.
			TriggerOutputContext.ManualStartOnly
		}

	override val metronomeContext: MetronomeContext
		get() = try {
			MetronomeContext.valueOf(
				getStringPreference(
					R.string.pref_metronome_key,
					R.string.pref_metronome_defaultValue
				)
			)
		} catch (_: Exception) {
			// backward compatibility with old shite values.
			MetronomeContext.Off
		}

	override val lyricColor: Int
		get() = getColorPreference(R.string.pref_lyricColor_key, R.string.pref_lyricColor_default)

	override val chordColor: Int
		get() = getColorPreference(R.string.pref_chordColor_key, R.string.pref_chordColor_default)

	override val chorusHighlightColor: Int
		get() = getColorPreference(
			R.string.pref_chorusSectionHighlightColor_key,
			R.string.pref_chorusSectionHighlightColor_default
		)

	override val annotationColor: Int
		get() = getColorPreference(
			R.string.pref_annotationColor_key,
			R.string.pref_annotationColor_default
		)

	override val largePrint: Boolean
		get() = getBooleanPreference(
			R.string.pref_largePrintList_key,
			R.string.pref_largePrintList_defaultValue
		)

	override val proximityScroll: Boolean
		get() = getBooleanPreference(R.string.pref_proximityScroll_key, false)

	override val anyOtherKeyPageDown: Boolean
		get() = getBooleanPreference(R.string.pref_anyOtherKeyPageDown_key, false)

	override var storageSystem: StorageType
		get() = try {
			StorageType.valueOf(
				getStringPreference(
					R.string.pref_cloudStorageSystem_key,
					R.string.pref_cloudStorageSystem_defaultValue
				)
			)
		} catch (e: Exception) {
			// backward compatibility with old shite values.
			StorageType.GoogleDrive
		}
		set(value) = setStringPreference(R.string.pref_cloudStorageSystem_key, value.name)

	override val onlyUseBeatFontSizes: Boolean
		get() = getBooleanPreference(
			R.string.pref_alwaysUseBeatFontPrefs_key,
			R.string.pref_alwaysUseBeatFontPrefs_defaultValue
		)

	private val minimumFontSizeOffset =
		Integer.parseInt(appResources.getString(R.string.fontSizeMin))

	override val minimumBeatFontSize: Int
		get() = getIntPreference(
			R.string.pref_minFontSize_key,
			R.string.pref_minFontSize_default,
			minimumFontSizeOffset
		)

	override val maximumBeatFontSize: Int
		get() = getIntPreference(
			R.string.pref_maxFontSize_key,
			R.string.pref_maxFontSize_default,
			minimumFontSizeOffset
		)

	override val minimumSmoothFontSize: Int
		get() = getIntPreference(
			R.string.pref_minFontSizeSmooth_key,
			R.string.pref_minFontSizeSmooth_default,
			minimumFontSizeOffset
		)

	override val maximumSmoothFontSize: Int
		get() = getIntPreference(
			R.string.pref_maxFontSizeSmooth_key,
			R.string.pref_maxFontSizeSmooth_default,
			minimumFontSizeOffset
		)

	override val minimumManualFontSize: Int
		get() = getIntPreference(
			R.string.pref_minFontSizeManual_key,
			R.string.pref_minFontSizeManual_default,
			minimumFontSizeOffset
		)

	override val maximumManualFontSize: Int
		get() = getIntPreference(
			R.string.pref_maxFontSizeManual_key,
			R.string.pref_maxFontSizeManual_default,
			minimumFontSizeOffset
		)

	override val useExternalStorage: Boolean
		get() = getBooleanPreference(R.string.pref_useExternalStorage_key, false)

	override val mimicBandLeaderDisplay: Boolean
		get() = getBooleanPreference(R.string.pref_mimicBandLeaderDisplay_key, true)

	override val playNextSong: String
		get() = getStringPreference(
			R.string.pref_automaticallyPlayNextSong_key,
			R.string.pref_automaticallyPlayNextSong_defaultValue
		)

	override val showBeatStyleIcons: Boolean
		get() = getBooleanPreference(
			R.string.pref_showBeatStyleIcons_key,
			R.string.pref_showBeatStyleIcons_defaultValue
		)

	override val showKeyInSongList: Boolean
		get() = getBooleanPreference(
			R.string.pref_showKeyInList_key,
			R.string.pref_showKeyInList_defaultValue
		)

	override val showRatingInSongList: Boolean
		get() = getBooleanPreference(
			R.string.pref_showRatingInList_key,
			R.string.pref_showRatingInList_defaultValue
		)

	override val showYearInSongList: Boolean
		get() = getBooleanPreference(
			R.string.pref_showYearInList_key,
			R.string.pref_showYearInList_defaultValue
		)

	override val songIconDisplayPosition: SongIconDisplayPosition
		get() = try {
			SongIconDisplayPosition.valueOf(
				getStringPreference(
					R.string.pref_songIconDisplayPosition_key,
					R.string.pref_songIconDisplayPosition_defaultValue
				)
			)
		} catch (_: Exception) {
			// Default
			SongIconDisplayPosition.Left
		}

	override val showMusicIcon: Boolean
		get() = getBooleanPreference(
			R.string.pref_showMusicIcon_key,
			R.string.pref_showMusicIcon_defaultValue
		)

	override val screenAction: SongView.ScreenAction
		get() = try {
			SongView.ScreenAction.valueOf(
				getStringPreference(
					R.string.pref_screenAction_key,
					R.string.pref_screenAction_defaultValue
				)
			)
		} catch (e: Exception) {
			// backward compatibility with old shite values.
			SongView.ScreenAction.Scroll
		}

	override val audioPlayer: AudioPlayerType
		get() = try {
			AudioPlayerType.valueOf(
				getStringPreference(
					R.string.pref_audioplayer_key,
					R.string.pref_audioplayer_defaultValue
				)
			)
		} catch (e: Exception) {
			// backward compatibility with old shite values.
			AudioPlayerType.MediaPlayer
		}

	override val showScrollIndicator: Boolean
		get() = getBooleanPreference(
			R.string.pref_showScrollIndicator_key,
			R.string.pref_showScrollIndicator_defaultValue
		)

	override val beatCounterTextOverlay: BeatCounterTextOverlay
		get() = try {
			BeatCounterTextOverlay.valueOf(
				getStringPreference(
					R.string.pref_beatCounterTextOverlay_key,
					R.string.pref_beatCounterTextOverlay_defaultValue
				)
			)
		} catch (e: Exception) {
			// backward compatibility with old shite values.
			BeatCounterTextOverlay.Nothing
		}

	private val commentDisplayTimeOffset =
		Integer.parseInt(appResources.getString(R.string.pref_commentDisplayTime_offset))

	override val commentDisplayTime: Int
		get() = getIntPreference(
			R.string.pref_commentDisplayTime_key,
			R.string.pref_commentDisplayTime_default,
			commentDisplayTimeOffset
		)

	override val midiTriggerSafetyCatch: SongView.TriggerSafetyCatch
		get() = try {
			SongView.TriggerSafetyCatch.valueOf(
				getStringPreference(
					R.string.pref_midiTriggerSafetyCatch_key,
					R.string.pref_midiTriggerSafetyCatch_defaultValue
				)
			)
		} catch (e: Exception) {
			// backward compatibility with old shite values.
			SongView.TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine
		}

	override val highlightCurrentLine: Boolean
		get() = getBooleanPreference(
			R.string.pref_highlightCurrentLine_key,
			R.string.pref_highlightCurrentLine_defaultValue
		)

	override val showPageDownMarker: Boolean
		get() = getBooleanPreference(
			R.string.pref_highlightPageDownLine_key,
			R.string.pref_highlightPageDownLine_defaultValue
		)

	override val clearTagsOnFolderChange: Boolean
		get() = getBooleanPreference(
			R.string.pref_clearTagFilterOnFolderChange_key,
			R.string.pref_highlightPageDownLine_defaultValue
		)

	override val highlightBeatSectionStart: Boolean
		get() = getBooleanPreference(
			R.string.pref_highlightBeatSectionStart_key,
			R.string.pref_highlightBeatSectionStart_defaultValue
		)

	override val beatCounterColor: Int
		get() = getColorPreference(
			R.string.pref_beatCounterColor_key,
			R.string.pref_beatCounterColor_default
		)

	override val commentColor: Int
		get() = getColorPreference(
			R.string.pref_commentTextColor_key,
			R.string.pref_commentTextColor_default
		)

	override val scrollIndicatorColor: Int
		get() = getColorPreference(
			R.string.pref_scrollMarkerColor_key,
			R.string.pref_scrollMarkerColor_default
		)

	override val beatSectionStartHighlightColor: Int
		get() = getColorPreference(
			R.string.pref_beatSectionStartHighlightColor_key,
			R.string.pref_beatSectionStartHighlightColor_default
		)

	override val currentLineHighlightColor: Int
		get() = getColorPreference(
			R.string.pref_currentLineHighlightColor_key,
			R.string.pref_currentLineHighlightColor_default
		)

	override val pageDownMarkerColor: Int
		get() = getColorPreference(
			R.string.pref_pageDownScrollHighlightColor_key,
			R.string.pref_pageDownScrollHighlightColor_default
		)

	override val pulseDisplay: Boolean
		get() = getBooleanPreference(R.string.pref_pulse_key, R.string.pref_pulse_defaultValue)

	override val backgroundColor: Int
		get() = getColorPreference(
			R.string.pref_backgroundColor_key,
			R.string.pref_backgroundColor_default
		)

	override val pulseColor: Int
		get() = getColorPreference(R.string.pref_pulseColor_key, R.string.pref_pulseColor_default)

	override var dropboxAccessToken: String
		get() = getPrivateStringPreference(R.string.pref_dropboxAccessToken_key, "")
		set(value) = setPrivateStringPreference(R.string.pref_dropboxAccessToken_key, value)

	override var dropboxRefreshToken: String
		get() = getPrivateStringPreference(R.string.pref_dropboxRefreshToken_key, "")
		set(value) = setPrivateStringPreference(R.string.pref_dropboxRefreshToken_key, value)

	override var dropboxExpiryTime: Long
		get() = getPrivateLongPreference(R.string.pref_dropboxExpiryTime_key, 0L)
		set(value) = setPrivateLongPreference(R.string.pref_dropboxExpiryTime_key, value)

	override val trimTrailingPunctuation: Boolean
		get() = getBooleanPreference(R.string.pref_trimTrailingPunctuation_key, true)

	override val useUnicodeEllipsis: Boolean
		get() = getBooleanPreference(R.string.pref_useUnicodeEllipsis_key, true)

	private fun getIntPreference(
		prefResourceString: Int,
		prefDefaultResourceString: Int,
		offset: Int
	): Int {
		return getIntPreference(
			prefResourceString,
			appResources.getString(prefDefaultResourceString).toInt()
		) + offset
	}

	protected abstract fun getIntPreference(prefResourceString: Int, default: Int): Int
	abstract override fun getStringPreference(key: String, default: String): String
	abstract override fun getStringSetPreference(key: String, default: Set<String>): Set<String>

	@Suppress("SameParameterValue")
	protected abstract fun getPrivateStringPreference(
		prefResourceString: Int,
		default: String
	): String

	protected abstract fun setStringPreference(prefResourceString: Int, value: String)

	@Suppress("SameParameterValue")
	protected abstract fun setPrivateStringPreference(prefResourceString: Int, value: String)

	@Suppress("SameParameterValue")
	protected abstract fun setPrivateLongPreference(prefResourceString: Int, value: Long)
	abstract override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
	abstract override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)

	@Suppress("SameParameterValue")
	protected abstract fun getPrivateLongPreference(prefResourceString: Int, default: Long): Long
	protected abstract fun getStringPreference(prefResourceString: Int, default: String): String
	protected abstract fun getStringSetPreference(
		prefResourceString: Int,
		default: Set<String>
	): Set<String>

	protected abstract fun getColorPreference(
		prefResourceString: Int,
		prefDefaultResourceString: Int
	): Int

	protected abstract fun getBooleanPreference(prefResourceString: Int, default: Boolean): Boolean

	@Suppress("SameParameterValue")
	protected abstract fun setBooleanPreference(prefResourceString: Int, value: Boolean)

	private fun getStringPreference(
		prefResourceString: Int,
		prefDefaultResourceString: Int
	): String = getStringPreference(
		prefResourceString,
		appResources.getString(prefDefaultResourceString)
	)

	private fun getBooleanPreference(
		prefResourceString: Int,
		prefDefaultResourceString: Int
	): Boolean =
		getBooleanPreference(
			prefResourceString,
			appResources.getString(prefDefaultResourceString).toBoolean()
		)
}