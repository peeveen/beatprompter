package com.stevenfrew.beatprompter

import android.content.SharedPreferences
import android.graphics.Color
import com.stevenfrew.beatprompter.cache.parse.ShowBPMContext
import com.stevenfrew.beatprompter.comm.bluetooth.BluetoothMode
import com.stevenfrew.beatprompter.comm.midi.ConnectionType
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.ui.SongView
import com.stevenfrew.beatprompter.ui.pref.MetronomeContext
import com.stevenfrew.beatprompter.ui.pref.SortingPreference

object Preferences {
    val midiConnectionType: ConnectionType
        get() = try {
            ConnectionType.valueOf(getStringPreference(R.string.pref_midiConnectionType_key, R.string.pref_midiConnectionType_defaultValue)!!)
        } catch (e: Exception) {
            // Backwards compatibility with old shite values from previous app versions.
            ConnectionType.USBOnTheGo
        }

    val defaultTrackVolume: Int
        get() = getIntPreference(R.string.pref_defaultTrackVolume_key, R.string.pref_defaultTrackVolume_default, 1)

    val defaultMIDIOutputChannel: Int
        get() = getIntPreference(R.string.pref_defaultMIDIOutputChannel_key, R.string.pref_defaultMIDIOutputChannel_default, 0)

    val defaultHighlightColor: Int
        get() = getColorPreference(R.string.pref_highlightColor_key, R.string.pref_highlightColor_default)

    val bandLeaderDevice: String
        get() = getStringPreference(R.string.pref_bandLeaderDevice_key, "") ?: ""

    val bluetoothMode: BluetoothMode
        get() = try {
            BluetoothMode.valueOf(getStringPreference(R.string.pref_bluetoothMode_key, R.string.pref_bluetoothMode_defaultValue)!!)
        } catch (e: Exception) {
            // Backwards compatibility with old shite values from previous app versions.
            BluetoothMode.None
        }

    val incomingMIDIChannels: Int
        get() = getIntPreference(R.string.pref_midiIncomingChannels_key, 65535)

    var cloudDisplayPath: String?
        get() = getStringPreference(R.string.pref_cloudDisplayPath_key, null)
        set(value) = setStringPreference(R.string.pref_cloudDisplayPath_key, value)

    var cloudPath: String?
        get() = getStringPreference(R.string.pref_cloudPath_key, null)
        set(value) = setStringPreference(R.string.pref_cloudPath_key, value)

    val includeSubFolders: Boolean
        get() = getBooleanPreference(R.string.pref_includeSubfolders_key, false)

    var firstRun: Boolean
        get() = getBooleanPreference(R.string.pref_firstRun_key, true)
        set(value) = setBooleanPreference(R.string.pref_firstRun_key, value)

    val manualMode: Boolean
        get() = getBooleanPreference(R.string.pref_manualMode_key, false)

    val mute: Boolean
        get() = getBooleanPreference(R.string.pref_mute_key, false)

    var sorting: SortingPreference
        get() = try {
            SortingPreference.valueOf(getStringPreference(R.string.pref_sorting_key, SortingPreference.Title.name)!!)
        } catch (ignored: Exception) {
            // backward compatibility with old shite values.
            SortingPreference.Title
        }
        set(value) = setStringPreference(R.string.pref_sorting_key, value.toString())

    val defaultCountIn: Int
        get() = getIntPreference(R.string.pref_countIn_key, R.string.pref_countIn_default, 0)

    val sendMIDIClock: Boolean
        get() = getBooleanPreference(R.string.pref_sendMidi_key, false)

    val customCommentsUser: String
        get() = getStringPreference(R.string.pref_customComments_key, R.string.pref_customComments_defaultValue)
                ?: ""

    val showChords: Boolean
        get() = getBooleanPreference(R.string.pref_showChords_key, R.string.pref_showChords_defaultValue)

    val showKey: Boolean
        get() = getBooleanPreference(R.string.pref_showSongKey_key, R.string.pref_showSongKey_defaultValue)

    val showBPMContext: ShowBPMContext
        get() = try {
            ShowBPMContext.valueOf(getStringPreference(R.string.pref_showSongBPM_key, R.string.pref_showSongBPM_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            ShowBPMContext.No
        }

    val sendMIDITriggerOnStart: TriggerOutputContext
        get() = try {
            TriggerOutputContext.valueOf(getStringPreference(R.string.pref_sendMidiTriggerOnStart_key, R.string.pref_sendMidiTriggerOnStart_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            TriggerOutputContext.ManualStartOnly
        }

    val metronomeContext: MetronomeContext
        get() = try {
            MetronomeContext.valueOf(getStringPreference(R.string.pref_metronome_key, R.string.pref_metronome_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            MetronomeContext.Off
        }

    val lyricColor: Int
        get() = getColorPreference(R.string.pref_lyricColor_key, R.string.pref_lyricColor_default)

    val chordColor: Int
        get() = getColorPreference(R.string.pref_chordColor_key, R.string.pref_chordColor_default)

    val chorusHighlightColor: Int
        get() = getColorPreference(R.string.pref_chorusSectionHighlightColor_key, R.string.pref_chorusSectionHighlightColor_default)

    val annotationColor: Int
        get() = getColorPreference(R.string.pref_annotationColor_key, R.string.pref_annotationColor_default)

    val largePrint: Boolean
        get() = getBooleanPreference(R.string.pref_largePrintList_key, R.string.pref_largePrintList_defaultValue)

    val proximityScroll: Boolean
        get() = getBooleanPreference(R.string.pref_proximityScroll_key, false)

    val anyOtherKeyPageDown: Boolean
        get() = getBooleanPreference(R.string.pref_anyOtherKeyPageDown_key, false)

    var storageSystem: StorageType
        get() = try {
            StorageType.valueOf(getStringPreference(R.string.pref_cloudStorageSystem_key, R.string.pref_cloudStorageSystem_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            StorageType.GoogleDrive
        }
        set(value) = setStringPreference(R.string.pref_cloudStorageSystem_key, value.name)

    val onlyUseBeatFontSizes: Boolean
        get() = getBooleanPreference(R.string.pref_alwaysUseBeatFontPrefs_key, R.string.pref_alwaysUseBeatFontPrefs_defaultValue)

    private val minimumFontSizeOffset = Integer.parseInt(BeatPrompter.getResourceString(R.string.fontSizeMin))

    val minimumBeatFontSize: Int
        get() = getIntPreference(R.string.pref_minFontSize_key, R.string.pref_minFontSize_default, minimumFontSizeOffset)

    val maximumBeatFontSize: Int
        get() = getIntPreference(R.string.pref_maxFontSize_key, R.string.pref_maxFontSize_default, minimumFontSizeOffset)

    val minimumSmoothFontSize: Int
        get() = getIntPreference(R.string.pref_minFontSizeSmooth_key, R.string.pref_minFontSizeSmooth_default, minimumFontSizeOffset)

    val maximumSmoothFontSize: Int
        get() = getIntPreference(R.string.pref_maxFontSizeSmooth_key, R.string.pref_maxFontSizeSmooth_default, minimumFontSizeOffset)

    val minimumManualFontSize: Int
        get() = getIntPreference(R.string.pref_minFontSizeManual_key, R.string.pref_minFontSizeManual_default, minimumFontSizeOffset)

    val maximumManualFontSize: Int
        get() = getIntPreference(R.string.pref_maxFontSizeManual_key, R.string.pref_maxFontSizeManual_default, minimumFontSizeOffset)

    val useExternalStorage: Boolean
        get() = getBooleanPreference(R.string.pref_useExternalStorage_key, false)

    val mimicBandLeaderDisplay: Boolean
        get() = getBooleanPreference(R.string.pref_mimicBandLeaderDisplay_key, true)

    val playNextSong: String
        get() = getStringPreference(R.string.pref_automaticallyPlayNextSong_key, R.string.pref_automaticallyPlayNextSong_defaultValue)!!

    val showBeatStyleIcons: Boolean
        get() = getBooleanPreference(R.string.pref_showBeatStyleIcons_key, R.string.pref_showBeatStyleIcons_defaultValue)

    val showKeyInSongList: Boolean
        get() = getBooleanPreference(R.string.pref_showKeyInList_key, R.string.pref_showKeyInList_defaultValue)

    val showMusicIcon: Boolean
        get() = getBooleanPreference(R.string.pref_showMusicIcon_key, R.string.pref_showMusicIcon_defaultValue)

    val screenAction: SongView.ScreenAction
        get() = try {
            SongView.ScreenAction.valueOf(getStringPreference(R.string.pref_screenAction_key, R.string.pref_screenAction_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            SongView.ScreenAction.Scroll
        }

    val showScrollIndicator: Boolean
        get() = getBooleanPreference(R.string.pref_showScrollIndicator_key, R.string.pref_showScrollIndicator_defaultValue)

    val showSongTitle: Boolean
        get() = getBooleanPreference(R.string.pref_showSongTitle_key, R.string.pref_showSongTitle_defaultValue)

    private val commentDisplayTimeOffset = Integer.parseInt(BeatPrompter.getResourceString(R.string.pref_commentDisplayTime_offset))

    val commentDisplayTime: Int
        get() = getIntPreference(R.string.pref_commentDisplayTime_key, R.string.pref_commentDisplayTime_default, commentDisplayTimeOffset)

    val midiTriggerSafetyCatch: SongView.TriggerSafetyCatch
        get() = try {
            SongView.TriggerSafetyCatch.valueOf(getStringPreference(R.string.pref_midiTriggerSafetyCatch_key, R.string.pref_midiTriggerSafetyCatch_defaultValue)!!)
        } catch (e: Exception) {
            // backward compatibility with old shite values.
            SongView.TriggerSafetyCatch.WhenAtTitleScreenOrPausedOrLastLine
        }

    val highlightCurrentLine: Boolean
        get() = getBooleanPreference(R.string.pref_highlightCurrentLine_key, R.string.pref_highlightCurrentLine_defaultValue)

    val showPageDownMarker: Boolean
        get() = getBooleanPreference(R.string.pref_highlightPageDownLine_key, R.string.pref_highlightPageDownLine_defaultValue)

    val highlightBeatSectionStart: Boolean
        get() = getBooleanPreference(R.string.pref_highlightBeatSectionStart_key, R.string.pref_highlightBeatSectionStart_defaultValue)

    val beatCounterColor: Int
        get() = getColorPreference(R.string.pref_beatCounterColor_key, R.string.pref_beatCounterColor_default)

    val commentColor: Int
        get() = getColorPreference(R.string.pref_commentTextColor_key, R.string.pref_commentTextColor_default)

    val scrollIndicatorColor: Int
        get() = getColorPreference(R.string.pref_scrollMarkerColor_key, R.string.pref_scrollMarkerColor_default)

    val beatSectionStartHighlightColor: Int
        get() = getColorPreference(R.string.pref_beatSectionStartHighlightColor_key, R.string.pref_beatSectionStartHighlightColor_default)

    val currentLineHighlightColor: Int
        get() = getColorPreference(R.string.pref_currentLineHighlightColor_key, R.string.pref_currentLineHighlightColor_default)

    val pageDownMarkerColor: Int
        get() = getColorPreference(R.string.pref_pageDownScrollHighlightColor_key, R.string.pref_pageDownScrollHighlightColor_default)

    val pulseDisplay: Boolean
        get() = getBooleanPreference(R.string.pref_pulse_key, R.string.pref_pulse_defaultValue)

    val backgroundColor: Int
        get() = getColorPreference(R.string.pref_backgroundColor_key, R.string.pref_backgroundColor_default)

    val pulseColor: Int
        get() = getColorPreference(R.string.pref_pulseColor_key, R.string.pref_pulseColor_default)

    var dropboxAccessToken: String?
        get() = getPrivateStringPreference(R.string.pref_dropboxAccessToken_key, null)
        set(value) = setPrivateStringPreference(R.string.pref_dropboxAccessToken_key, value)

    private fun getIntPreference(prefResourceString: Int, prefDefaultResourceString: Int, offset: Int): Int {
        return getIntPreference(prefResourceString,
                BeatPrompter.getResourceString(prefDefaultResourceString).toInt()) + offset
    }

    private fun getIntPreference(prefResourceString: Int, default: Int): Int {
        return BeatPrompter
                .preferences
                .getInt(BeatPrompter.getResourceString(prefResourceString), default)
    }

    fun getStringPreference(key: String, default: String?): String? {
        return BeatPrompter
                .preferences
                .getString(key, default)
    }

    @Suppress("SameParameterValue")
    private fun getPrivateStringPreference(prefResourceString: Int, default: String?): String? {
        return BeatPrompter
                .privatePreferences
                .getString(BeatPrompter.getResourceString(prefResourceString),
                        default)
    }

    private fun getStringPreference(prefResourceString: Int, default: String?): String? {
        return BeatPrompter
                .preferences
                .getString(BeatPrompter.getResourceString(prefResourceString),
                        default)
    }

    private fun getStringPreference(prefResourceString: Int, prefDefaultResourceString: Int): String? {
        return getStringPreference(prefResourceString,
                BeatPrompter.getResourceString(prefDefaultResourceString))
    }

    private fun getColorPreference(prefResourceString: Int, prefDefaultResourceString: Int): Int {
        return BeatPrompter
                .preferences
                .getInt(BeatPrompter.getResourceString(prefResourceString),
                        Color.parseColor(BeatPrompter.getResourceString(prefDefaultResourceString)))
    }

    private fun getBooleanPreference(prefResourceString: Int, default: Boolean): Boolean {
        return BeatPrompter
                .preferences
                .getBoolean(BeatPrompter.getResourceString(prefResourceString), default)
    }

    @Suppress("SameParameterValue")
    private fun setBooleanPreference(prefResourceString: Int, value: Boolean) {
        BeatPrompter
                .preferences
                .edit()
                .putBoolean(BeatPrompter.getResourceString(prefResourceString), value)
                .apply()
    }

    private fun getBooleanPreference(prefResourceString: Int, prefDefaultResourceString: Int): Boolean {
        return getBooleanPreference(prefResourceString,
                BeatPrompter.getResourceString(prefDefaultResourceString).toBoolean())
    }

    private fun setStringPreference(prefResourceString: Int, value: String?) {
        BeatPrompter
                .preferences
                .edit()
                .putString(BeatPrompter.getResourceString(prefResourceString), value)
                .apply()
    }

    @Suppress("SameParameterValue")
    private fun setPrivateStringPreference(prefResourceString: Int, value: String?) {
        BeatPrompter
                .privatePreferences
                .edit()
                .putString(BeatPrompter.getResourceString(prefResourceString), value)
                .apply()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        BeatPrompter.preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}