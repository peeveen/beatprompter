package com.stevenfrew.beatprompter.cache.parse

import android.graphics.Color
import android.os.Handler
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.event.BaseEvent
import com.stevenfrew.beatprompter.event.BeatEvent
import com.stevenfrew.beatprompter.event.MIDIEvent
import com.stevenfrew.beatprompter.event.PauseEvent
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.midi.Message
import com.stevenfrew.beatprompter.midi.TriggerOutputContext
import com.stevenfrew.beatprompter.songload.CancelEvent

class SongParser constructor(val mSongFile: SongFile, val mCancelEvent: CancelEvent, val mSongLoadHander: Handler, val mRegistered:Boolean):SongFileParser<Song>(mSongFile) {
    private val mCountInPref:Int
    private val mMetronomeContext:MetronomeContext
    private val mCustomCommentsUser:String
    private val mShowChords:Boolean
    private val mTriggerContext: TriggerOutputContext
    private var mBeatInfo:BeatInfo=BeatInfo()

    private var mSendMidiClock:Boolean=false
    private var mCurrentHighlightColor:Int
    private var mSongTime:Long=0
    private var mDefaultMIDIOutputChannel:Byte

    init
    {
        val sharedPrefs=BeatPrompterApplication.preferences
        mSendMidiClock = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        mCountInPref = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        mMetronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPrefs)
        mCurrentHighlightColor = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default)))
        mCustomCommentsUser = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))?:""
        mShowChords = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        mTriggerContext = TriggerOutputContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        val defaultMIDIOutputChannelPrefValue = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        mDefaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)
    }

    override fun parseLine(line: TextFileLine<Song>) {
        // Bars can be defined by commas ....
        var commaBars:Int?=null
        // TODO: Deep clone of tags
        val tags=line.mTags.toList()

        var workLine=line.mTaglessLine
        while (workLine.startsWith(",")) {
            if(commaBars==null)
                commaBars=0
            workLine = workLine.substring(1)
            tags.forEach{it.retreatFrom(0)}
            commaBars++
        }

        var scrollBeatOffset=0
        while (workLine.endsWith(">") || workLine.endsWith("<")) {
            if (workLine.endsWith(">"))
                scrollBeatOffset++
            else if (workLine.endsWith("<"))
                scrollBeatOffset--
            workLine = workLine.substring(0, workLine.length - 1)
            tags.forEach{it.retreatFrom(workLine.length)}
        }

        // TODO: dynamic BPB changing

        // ... or by a tag (which overrides commas)
        val barsTag=tags.filterIsInstance<BarsTag>().firstOrNull()
        val barsPerLineTag=tags.filterIsInstance<BarsPerLineTag>().firstOrNull()
        val beatsPerBarTag=tags.filterIsInstance<BeatsPerBarTag>().firstOrNull()
        val beatsPerMinuteTag=tags.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val scrollBeatTag=tags.filterIsInstance<ScrollBeatTag>().firstOrNull()

        val beatStartTags=tags.filterIsInstance<BeatStartTag>()
        val beatStopTags=tags.filterIsInstance<BeatStopTag>()
        val beatModeTags=listOf(beatStartTags,beatStopTags).flatMap { it }

        val barsInThisLine=barsTag?.mBars?:barsPerLineTag?.mBPL?:commaBars?:mBeatInfo.mBPL
        val beatsPerBarInThisLine=beatsPerBarTag?.mBPB?:mBeatInfo.mBPB
        val beatsPerMinuteInThisLine=beatsPerMinuteTag?.mBPM?:mBeatInfo.mBPM
        var scrollBeatInThisLine=scrollBeatTag?.mScrollBeat?:mBeatInfo.mScrollBeat
        var modeOnThisLine=mBeatInfo.mScrollingMode

        // Multiple beatstart or beatstop tags on the same line are nonsensical
        if(beatModeTags.size>1)
            mErrors.add(FileParseError(beatModeTags.first(), BeatPrompterApplication.getResourceString(R.string.multiple_beatstart_beatstop_same_line)))
        else if(beatModeTags.size==1)
            if(beatStartTags.isNotEmpty())
                if(beatsPerMinuteInThisLine==0.0)
                    mErrors.add(FileParseError(beatStartTags.first(), BeatPrompterApplication.getResourceString(R.string.beatstart_with_no_bpm)))
                else
                    modeOnThisLine= ScrollingMode.Beat
            else
                modeOnThisLine = ScrollingMode.Manual

        val previousBeatsPerBar=mBeatInfo.mBPB
        val previousScrollBeat=mBeatInfo.mScrollBeat
        // If the beats-per-bar have changed, and there is no indication of what the new scrollbeat should be,
        // set the new scrollbeat to have the same "difference" as before. For example, if the old BPB was 4,
        // and the scrollbeat was 3 (one less than BPB), a new BPB of 6 should have a scrollbeat of 5 (one
        // less than BPB)
        if((beatsPerBarInThisLine!=previousBeatsPerBar)&&(scrollBeatTag==null))
        {
            val prevScrollBeatDiff = previousBeatsPerBar - previousScrollBeat
            if(beatsPerBarInThisLine-prevScrollBeatDiff>0)
                scrollBeatInThisLine=beatsPerBarInThisLine-prevScrollBeatDiff
        }

        if(scrollBeatInThisLine>beatsPerBarInThisLine)
            scrollBeatInThisLine=beatsPerBarInThisLine

        mBeatInfo= BeatInfo(barsPerLineTag?.mBPL?:mBeatInfo.mBPL,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)

        if ((beatsPerBarInThisLine!=0)&&(scrollBeatOffset < -beatsPerBarInThisLine || scrollBeatOffset >= beatsPerBarInThisLine)) {
            mErrors.add(FileParseError(line.mLineNumber, BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)))
            scrollBeatOffset = 0
        }

        val lineBeatInfo= BeatInfo(barsInThisLine,beatsPerBarInThisLine,beatsPerMinuteInThisLine,scrollBeatInThisLine,scrollBeatOffset,modeOnThisLine)

        // TODO: when creating image line, expect exception from bad images.
        //         errors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mImageFile.mName))
    }

    override fun getResult(): Song {
        throw NotImplementedError("£")
        //return Song(mCachedCloudFileDescriptor.mID,mSongFile.mTitle,mSongFile.mArtist,mSongFile.mKey,mSongFile.mBPM,null,100,listOf(),)
    }

    override fun createSongTag(name:String,lineNumber:Int,position:Int,value:String): Tag
    {
        when(name)
        {
            "bpm", "metronome", "beatsperminute"->return BeatsPerMinuteTag(name, lineNumber, position, value)
            "b","bars"->return BarsTag(name, lineNumber, position, value)
            "bpb", "beatsperbar"->return BeatsPerBarTag(name, lineNumber, position, value)
            "bpl", "barsperline"->return BarsPerLineTag(name, lineNumber, position, value)
            "scrollbeat", "sb"->return ScrollBeatTag(name, lineNumber, position, value)
            "beatstart"->return BeatStartTag(name, lineNumber, position)
            "beatstop"->return BeatStopTag(name, lineNumber, position)

            "image"->return ImageTag(name, lineNumber, position, value)
            "pause"->return PauseTag(name, lineNumber, position, value)
            "send_midi_clock"->return SendMIDIClockTag(name, lineNumber, position)
            "comment", "c", "comment_box", "cb", "comment_italic", "ci"->return CommentTag(name, lineNumber, position, value)
            "count","countin"->return CountTag(name,lineNumber,position,value)

            "soh"->return StartOfHighlightTag(name, lineNumber, position, value, mCurrentHighlightColor)
            "eoh"->return EndOfHighlightTag(name, lineNumber, position)

            // Unused ChordPro tags
            "start_of_chorus", "end_of_chorus", "start_of_tab", "end_of_tab", "soc", "eoc", "sot", "eot", "define", "textfont", "tf", "textsize", "ts", "chordfont", "cf", "chordsize", "cs", "no_grid", "ng", "grid", "g", "titles", "new_page", "np", "new_physical_page", "npp", "columns", "col", "column_break", "colb", "pagetype", "capo", "zoom-android", "zoom", "tempo", "tempo-android", "instrument", "tuning" -> return ChordProTag(name,lineNumber,position)

            else->return MIDIEventTag(name, lineNumber, position, value, mSongTime, mDefaultMIDIOutputChannel)
        }
    }

    companion object {
        private const val DEMO_LINE_COUNT = 15
        // Every beatstart/beatstop block has events that are offset by this amount (one year).
        // If you left the app running for a year, it would eventually progress. WHO WOULD DO SUCH A THING?
        private val BEAT_MODE_BLOCK_TIME_CHUNK_NANOSECONDS = Utils.milliToNano(1000 * 60 * 24 * 365)

        private fun offsetMIDIEvents(firstEvent: BaseEvent?, errors: MutableList<FileParseError>) {
            var event = firstEvent
            while (event != null) {
                if (event is MIDIEvent) {
                    val midiEvent = event
                    if (midiEvent.mOffset.mAmount != 0) {
                        // OK, this event needs moved.
                        var newTime: Long = -1
                        if (midiEvent.mOffset.mOffsetType === EventOffsetType.Milliseconds) {
                            val offset = Utils.milliToNano(midiEvent.mOffset.mAmount)
                            newTime = midiEvent.mEventTime + offset
                        } else {
                            // Offset by beat count.
                            var beatCount = midiEvent.mOffset.mAmount
                            var currentEvent: BaseEvent = midiEvent
                            while (beatCount != 0) {
                                val beatEvent: BeatEvent = (if (beatCount > 0)
                                    currentEvent.nextBeatEvent
                                else if (currentEvent is BeatEvent && currentEvent.mPrevEvent != null)
                                    currentEvent.mPrevEvent!!.mPrevBeatEvent
                                else
                                    currentEvent.mPrevBeatEvent) ?: break
                                if (beatEvent.mEventTime != midiEvent.mEventTime) {
                                    beatCount -= beatCount / Math.abs(beatCount)
                                    newTime = beatEvent.mEventTime
                                }
                                currentEvent = beatEvent
                            }
                        }
                        if (newTime < 0) {
                            errors.add(FileParseError(midiEvent.mOffset.mSourceTag, BeatPrompterApplication.getResourceString(R.string.midi_offset_is_before_start_of_song)))
                            newTime = 0
                        }
                        val newMIDIEvent = MIDIEvent(newTime, midiEvent.mMessages)
                        midiEvent.insertEvent(newMIDIEvent)
                        event = midiEvent.mPrevEvent
                        midiEvent.remove()
                    }
                }
                event = event!!.mNextEvent
            }
        }

        private fun generatePause(pauseTime: Long, lastEvent: BaseEvent?, currentTime: Long): Long {
            var vLastEvent = lastEvent
            var vCurrentTime = currentTime
            // pauseTime is in milliseconds.
            // We don't want to generate thousands of events, so let's say every 1/10th of a second.
            val deciSeconds = Math.ceil(pauseTime.toDouble() / 100.0).toInt()
            val remainder = Utils.milliToNano(pauseTime) - Utils.milliToNano(deciSeconds * 100)
            val oneDeciSecondInNanoseconds = Utils.milliToNano(100)
            vCurrentTime += remainder
            for (f in 0 until deciSeconds) {
                val pauseEvent = PauseEvent(vCurrentTime, deciSeconds, f)
                vLastEvent!!.insertEvent(pauseEvent)
                vLastEvent = vLastEvent.lastEvent
                vCurrentTime += oneDeciSecondInNanoseconds
            }
            return vCurrentTime
        }
    }
}

