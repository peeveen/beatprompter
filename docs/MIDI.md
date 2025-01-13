# BeatPrompter MIDI Documentation

## MIDI Settings

These are the settings pertaining to MIDI functionality that you can change via the “Settings ...” menu.

| Setting                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Connection Type                     | Tell the app what type of MIDI connection you want to use:<br/><br/>**USB On-The-Go**: This will send MIDI signals via the USB socket on your phone. You will probably need a USB OTG adapter connected to some kind of MIDI cable to use this. This is the only option that will work for pre-Android-6.0 devices. If you have a relatively modern device, you will probably not need this, and should not enable it.<br/>**Native (recommended)**: Android 6.0 introduced native MIDI support, without the need for an intermediary adapter.<br/>**Bluetooth**: Use Bluetooth MIDI to communicate.<br/><br/>Changing this setting will reset all existing connections.            |
| Active incoming MIDI channels       | This allows you to specify what MIDI channels BeatPrompter will listen to. Messages received on unselected channels are immediately ignored.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Default Output MIDI channel         | BeatPrompter can send messages to any MIDI channel, but when a particular channel is not specified by a MIDI message tag, this will be the channel that BeatPrompter uses.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| Send MIDI clock signals             | If a song has beat timings encoded in it, then BeatPrompter can output MIDI start/stop/clock timing signals at the appropriate rate so that you can control external MIDI devices. You can enable/disable this functionality using this preference. Individual songs can override this preference using the `{send_midi_clock}` tag within their files. See the `{send_midi_clock}` tag documentation later for more details.                                                                                                                                                                                                                                                       |
| Start/stop/continue on MIDI signals | If a MIDI input is connected to the app, then any MIDI start/stop/continue signals that it receives will cause the song to start/stop/continue.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Output MIDI Trigger                 | If a song contains a MIDI trigger (either a ProgramChange or SongSelect message that instructs BeatPrompter to automatically load that song), then it can optionally send that message back out once it has loaded. The value of this setting will determine whether that happens:<br/><br/>**Always** = trigger message is sent out<br/>**Never** = trigger message is not sent out<br/>**When Started Manually** = trigger message will be sent only if the song was started manually, i.e. not by an incoming MIDI message.<br/><br/>Note that if the MIDI trigger data contained in the song has wildcards in it, it will not be sent, regardless of the value of this setting. |
| Trigger safety catch                | BeatPrompter is always listening to the MIDI messages that are coming in. This setting controls whether the app should obey a MIDI trigger if a song is already loaded. The available options are:<br/><br/>• Never<br/>• When at title screen<br/>• When at title screen, or paused<br/>• When at title screen, or paused, or on last line of song<br/>• Always                                                                                                                                                                                                                                                                                                                    |

## MIDI Trigger tags

These are the tags that can be added to song files to trigger their automatic loading when a particular MIDI message is received.

| Tag                                                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `{midi_program_change_trigger:pc,msb,lsb,channel}` | If you insert this tag into a song file, then BeatPrompter can automatically load the song if it receives a matching MIDI signal. For the song to be loaded, it would need to receive two Control Change MIDI signals that match the `msb` and `lsb` values, followed by a Program Change signal that matches the value of “pc”. The “channel” value is optional, but if specified, it means that the song will only be loaded if the signals are received on that channel.<br/><br/>NOTE: A value of `*` can be used as a wildcard for any of these values.<br/><br/>Example: `{midi_program_change_trigger:16,0,112}`<br/><br/>This would load the song automatically if it receives two Control Change values of MSB=0 and LSB=112 followed by a Program Change value of 16. In this example, because no explicit channel number has been specified, these signals can be received on any of the channels that BeatPrompter is listening to. |
| `{midi_song_select_trigger:n}`                     | Very similar to the tag above, this one will automatically load the song if BeatPrompter receives a Song Select MIDI signal with the given value. Song Select messages are broadcast across all channels, so there is no need to specify a channel with this tag.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

## MIDI Message tags

The following tags instruct BeatPrompter to send out MIDI messages.

- If the tag is defined before the first line of lyrics or chords in a song file, then the MIDI message is sent as soon as the song is loaded.
- If the tag is defined on a line of lyrics or chords, then the MIDI message is sent as soon as that line begins.
- For any tag that contains a `channel` parameter, this parameter is optional, but if provided, should be in the format “#N”, where N is the channel number, so, for example “#10” or “#1”. If omitted, then the default output MIDI channel (as defined in the app settings) is used.
- Parameter values are, by default, interpreted as normal decimal values, unless the parameter starts with “0x”, or ends with “h”, or just “looks like” hexadecimal characters. For example, “30” would be interpreted as decimal, “0x30” or “30h” would be interpreted as hexadecimal, and “1E” or “3f” would be interpreted as hexadecimal.
- All values should be in the range 0-127.
- Channel values should be in the range 1-16.
- If you want to send a Program Change or Song Select message when the song is loaded, and also use the same values as a trigger, you only need to define the trigger tag and choose the relevant value for the “Output MIDI Trigger” preference (though this functionality will not work if the trigger contains wildcards).
- Although not listed below, all these tags can have an extra parameter: a timing offset. If you don’t want the MIDI message to be sent as soon as the line begins, you can offset it by a certain number of beats, or an exact number of milliseconds. To include this parameter, put it at the end of the tag, separated from the rest of the tag by a semi-colon, e.g. `{midi_program_change:14,0,112;>>}` will make this message be sent two beats later than usual, whereas `{midi_continue;-750}` will make this message be sent be sent 750 milliseconds before the line is reached. NOTE: If your offset moves the MIDI message to before the start of the song, you will be shown an error message on the song title screen. The maximum offset is 16 beats or 10 seconds.

| Tag                                              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `{send_midi_clock}`                              | This tag can be defined anywhere in the song file. While playing songs that have beat timings encoded into them, BeatPrompter will output MIDI clock signals (0xF8) at the appropriate beats-per-minute rate, and will also output start (0xFA) and stop (0xFC) signals when the song starts or stops. It is possible to make use of these signals in a variety of ways. For example, if you have a looper pedal that can synchronize to beats, you can get an exact start/stop to your loop, snapped to the start and end of a bar, rather than having to gauge it manually. Or you could drive a drum machine, making it follow the speed of a song that changes speed throughout. Using this tag in a song overrides the value of the “Send MIDI Clock Signals” preference. |
| `{midi_program_change:pc,msb,lsb,channel}`       | This tag will make BeatPrompter send up to three MIDI signals to listening devices; two “bank select” Control Change messages containing the `msb` and `lsb` values, followed by a Program Change message containing the `pc` value. Only the `pc` value is required for this tag, the rest are optional, and if omitted, will simply result in fewer Control Change messages being sent.                                                                                                                                                                                                                                                                                                                                                                                      |
| `{midi_control_change:controller,value,channel}` | This tag will make the app send out a MIDI Control Change message, containing the given values.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `{midi_song_select:n}`                           | This tag will make the app send out a MIDI Song Select message containing the specified value.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `{midi_start}`                                   | This tag will make the app send out a MIDI Start signal to all listening devices.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| `{midi_stop}`                                    | This tag will make the app send out a MIDI Stop signal to all listening devices.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `{midi_continue}`                                | This tag will make the app send out a MIDI Continue signal to all listening devices.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `{midi_send:b1,b2,b3,channel}`                   | This is a low-level MIDI command, and is included only for flexibility. It will make the app send a MIDI message of up to three bytes (`b2` and `b3` are optional parameters). If this is a message targeted at a specific channel, the lower nibble of `b1` will be replaced by the `channel` value.                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |

## MIDI Aliases

If you make frequent use of MIDI commands in your song files, you can create a file of MIDI aliases to save you having to remember the numeric values all the time.

1. In your cloud storage, create a text document of any name, with `{midi_aliases:alias_set_name}` as the first line.
2. In the following lines, create a tag `{midi_alias:alias_name}`, putting the name of your choice in place of `alias_name`.
3. On the next lines, enter some MIDI message tags as usual (the usual hashtag comments are also allowed). You can parameterize your aliases using `?1`, `?2`, and `?3` to identify parameters. Also, any hexadecimal values that contain an underscore character will have this replaced by the optional channel parameter or the default output channel. See the [Built In MIDI Alias File](#builtInMidiAliasFile) section below for clarification.
4. The first blank line, end-of-file, or new `{midi_alias}` tag found after the current `{midi_alias}` tag denotes the end of that alias.
5. Now, whenever you want to use that sequence of aliased MIDI messages in your song file, simply put the tag `{alias_name}`. Obviously your alias name should not match any of the existing tags that BeatPrompter recognises!
6. An alias can refer to any alias defined **earlier** in the same file, but not to aliases defined in other files.
7. Multiple aliases can have the same name with different numbers of parameters. The appropriate one will be selected when they are executed.

### Example MIDI alias file

```
{midi_aliases:Guitar And Vocal Effects}

{midi_alias:distortion_on}
# This switches on the distortion in the guitar effects board.
{midi_control_change:16,127,#3}

{midi_alias:looper_on}
# This starts the vocal looper recording.
{midi_control_change:40,127,#4}
{midi_alias:looper_off}
# This ends the vocal looper recording and starts playback.
{midi_control_change:40,0,#4}
{midi_control_change:38,127,#4}
```

### <a name="builtInMidiAliasFile" /></a> Built-in MIDI alias file

This is the content of the built-in list of MIDI aliases, and is included here to demonstrate how the parameterization of aliases works.

```
{midi_aliases:Default}

{midi_alias:midi_start}
{midi_send:0xFA}

{midi_alias:midi_continue}
{midi_send:0xFB}

{midi_alias:midi_stop}
{midi_send:0xFC}

{midi_alias:midi_song_select}
{midi_send:0xF3,?1}

{midi_alias:midi_control_change}
{midi_send:0xB_,?1,?2}

{midi_alias:midi_msb_bank_select}
{midi_control_change:0x00,?1}

{midi_alias:midi_lsb_bank_select}
{midi_control_change:0x20,?1}

{midi_alias:midi_program_change}
{midi_send:0xC_,?1}

{midi_alias:midi_program_change}
{midi_msb_bank_select:?2}
{midi_program_change:?1}

{midi_alias:midi_program_change}
{midi_msb_bank_select:?2}
{midi_lsb_bank_select:?3}
{midi_program_change:?1}
```

### MIDI Start/Stop/Continue commands

When a song is started/resumed/stopped, BeatPrompter will send a MIDI Start/Continue/Stop command. If you add any of the following tags to an alias that does not use parameters, the commands defined by the alias will also be sent at these times.

```
{with_midi_start}
{with_midi_continue}
{with_midi_stop}
```

## MIDI Beat Clock Signals

This is obviously a timing-critical feature. Due to the nature of the operating system, the performance of this function can vary depending on how powerful your phone or tablet is, and what other processes are running in the background.

> Technical note: the reliability of this feature has been found to vary from device to device. Investigations have revealed that it is generally related to how frequently and how fast the JVM garbage collector runs during song playback. This in turn can depend on how “wasteful” the underlying system libraries are with regard to object allocation, particularly string allocation for logcat messages during screen rendering, regardless of whether the string is logged or not.