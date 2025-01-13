# Version History

Versions prior to 1.70 are not listed.

## 1.77

- You can now jump to an upcoming on-screen {beatstart} section by tapping the screen, just like starting a song (previously, this was only possible with a pedal or Bluetooth keyboard).
- No longer replaces accidentals in unrecognized chords.
- Added new preferences for trimming trailing punctuation, or using Unicode ellipses.
- Added new song tags for `{year}` and `{icon}`, both sortable and displayable.
  - The `{year:nnnn}` tag allows you to specify the year that a song was released.
  - The `{icon:filename}` tag allows you to specify the filename of an icon image that should be shown in a user-defined position (left/right/etc) in the list. The icon images must be present in your cache of files.
- Removed the “Show song title” preference in favour of a new “Beat counter text overlay” preference. In this preference, you can choose what text to display over the top of the beat counter. Options are “Nothing” (default), “Song Title”, or “Current Time”. **If you have previously used the “show song title” preference, you will need to reconfigure this setting.**
- You can now launch a text editor directly from the long-press menu. Only available for cloud storage systems that have built-in editing facilities (currently Google Drive).
- Updated launcher icon.
- Improvements to file handling and synchronization:
  - The song database update operation is now more atomic, so there is less chance of data corruption if the app closes during an update.
  - Even if the song database is corrupted, the synchronization operation will now check for existing files left behind in app storage that match checksums of remote files, and will use them (without downloading again) if found.
  - Added a “stop” button to the synchronization dialog. If pressed, the database will be updated with everything that has been downloaded so far (so if you have a huge amount of files, you can now download chunks incrementally).
  - Google Drive sync now ignores shortcut links to files that no longer exist.

## 1.75

- More variation support
  - Added “Preferred Variation” preference
  - Added variation support to set lists
  - If a `{varstart}` or `{varxstart}` tag contains any variation names that were not defined in the `{variations}` tag, an error will now be shown when the song is loaded.
- Added `{no_chords}` tag
- Fixed rendering of `{pause}` progress bar
- Improved scrollbeat indicator position calculation
- Improved chord parsing
- USB device connection/disconnection message now shows device name.
- Audio tags can now define an absolute volume, not relative to default volume preference.
- Fixed inexact ExoPlayer volume setting.
- The color of comments can now be defined on a per-comment basis. **Also, be aware that the background color of in-song comments has been changed to match the screen background color.**
- When visible, the volume indicator and manual-mode “end song” messages are now positioned at the foot of the display, rather than the middle.
- Fixed the somewhat-neglected smooth-scrolling mode.

## 1.74

- Multiple tag filters can now be selected, applying on top of whatever the currently-selected “main” filter (e.g. folder, setlist, etc) is. So, for example, you can now choose a folder, and then show the songs that are tagged with “80s” or “90s” within that folder. Selecting a new “main” filter will, by default, clear the selected tag filters (configurable via preferences).
- Fixed `{beatstart}` bug
- Fixed bug when using {pause} with irregular `{scrollbeat}`
- Fixed page-down indicator color.
- No longer restarts when a Bluetooth keyboard (e.g. page-turner pedal) is connected or disconnected.
- Added song variations feature, and variation inclusion/exclusion sections (new “variations”, “varstart/varxstart”, and “varstop/varxstop” tags in documentation). Slight redesign of “Play Song” dialog as a consequence.
  - **BREAKING CHANGE:** In existing song files, if multiple `{audio}` tags are used to define alternative audio files that can be played, those tags must all now be on the same line. Audio files are now implicitly associated with variations.
- Added dark mode option (only affects song list, settings, and suchlike ... colors used by the song display screen are still determined entirely by preferences).
- Now shows a progress dialog while performing the first-time read of the database (instead of a blank screen), and won’t re-read the database every time you navigate away then back to the app.
- Greatly improved startup time (especially noticeable when you have hundreds of songs, audio files, etc). First launch of this new version will cache more information about your files. Subsequent launches will use that cached information.
- Changed BuyMeACoffee link on the About dialog from a text link to a nicer-looking icon, which will obviously be far more tempting to click!
- Added audio latency setting. This is also a new data item that is included in the Bluetooth message that band leaders send, so if you are using this version, ensure all band members are using it too, otherwise there will (probably) be communication errors.
- First-draft implementation of Bluetooth MIDI (with new Bluetooth MIDI Devices preference, allowing you to choose which paired devices to send MIDI messages to).
- The MIDI Connection Type setting is now multi-value. You can select multiple connection types if you are using them. **This is effectively a new preference that you may have to configure again if you are using MIDI.** However, the default value is “all connection types”, so you should not see any strange behavior unless you are physically connecting to MIDI-compatible devices but deliberately not using them.
- Fixed OneDrive “getting root folder” problem.
- Preferences will now persist between install/uninstall (dependent on periodic Google cloud backup, device-dependent).
- When files are being synchronized, failed downloads will now be retried up to a maximum of three times before the entire operation fails. This helps in situations where you experience momentary WiFi dropout, or random cloud API errors.
- If a “.filter_only” file is found in the root synchronization folder, it is ignored.
- Refreshing a file while a filter is selected will no longer jump back to the “All Songs” filter.
- The file synchronization will now take notice of renamed supporting files (audio/image/etc).
- Added new `{with_midi_start/continue/stop}` tags.
- Various MIDI fixes:
  - Fixed bug where MIDI tags on the same line as a `{beatstart}` tag would be ignored.
  - Fixed USB MIDI bug where some messages were not being padded to four bytes.
  - Fixed MIDI alias overloading.
  - Native MIDI receiver thread now stops correctly after device disconnection, allowing successful reconnection.
- Added new chord features
  - `{transpose}` and `{chord_map}` tags
  - Transpose option on long-press play dialog. This is also a new data item that is included in the Bluetooth message that band leaders send, so if you are using this version, ensure all band members are using it too, otherwise there will (probably) be communication errors.
  - New chord display preferences (“Always Use Sharps” and “Display Unicode Accidentals”)

## 1.71

- Fixed MIDI
- **Potential breaking change**: Any MIDI tags on the first line of the song will now be sent when the song starts, rather than when it is loaded.

## 1.70

- Housekeeping: all deprecated `AsyncTask`s replaced by Kotlin coroutines.
- Added interim progress dialog to cloud folder selection UI, as OneDrive can take a while to obtain the root path.
- Added new sortable `{rating}` tag, allowing you to order your song list with your favorites at the top.
- Last line in a beat-mode song is now highlighted correctly (if “Highlight Current Line” is enabled).
- Some small beat-calculation fixes:
  - Changed precedence of `{bpl}`, `{b}` and comma-shorthand tags (note that this is a **potential breaking change** if any of your song files have more than one of these tags on the same line). Commas take precedence, then `{b}`, then `{bpl}` (though, of course, `{bpl}` is a lasting change that affects any later lines).
  - Fixed buggy scroll indicator size change when `{bpb}` changes during scrollbeat changes.
