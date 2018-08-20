package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.*

@CacheXmlTag("song")
class SongFile constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mLines:Int, val mTitle:String, val mArtist:String, val mKey:String, val mBPM:Double, val mDuration:Long, val mAudioFiles:List<String>, val mImageFiles:List<String>,val mTags:Set<String>, val mProgramChangeTrigger:SongTrigger, val mSongSelectTrigger:SongTrigger, errors:List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors) {
    val mSortableArtist=sortableString(mArtist)
    val mSortableTitle=sortableString(mTitle)
    val mIsSmoothScrollable=mDuration>0
    val mIsBeatScrollable=mBPM>0.0

/*
      @Throws(IOException::class)
    fun getTimePerLineAndBar(chosenTrack: AudioFile?): SmoothScrollingTimings {
        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
        try {
            var songTime: Long = 0
            var songMilli = 0
            var pauseTime: Long = 0
            var realLineCount = 0
            var realBarCount = 0
            val parsingState=SongParsingState(ScrollingMode.Beat,this)
            var totalPauseTime: Long = 0//defaultPausePref*1000;
            var line: String?
            var lineImage: ImageFile? = null
            var lineNumber = 0
            val errors = ArrayList<FileParseError>()
            do {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= SongFileLine(line, ++lineNumber,parsingState)
                    // Ignore comments.
                    if (!fileLine.isComment) {
                        val strippedLine = fileLine.mTaglessLine
                        val chordsFound = fileLine.chordTags.isNotEmpty()
                        val bars=fileLine.mBeatInfo.mBPL
                        // Not bothered about chords at the moment.
                        fileLine.mTags.filterNot { it is ChordTag }.forEach{
                            if(it is TimeTag)
                                songTime=it.mDuration
                            else if(it is PauseTag)
                                pauseTime = it.mDuration
                            else if(it is ImageTag) {
                                if (lineImage != null)
                                    errors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                                else
                                    lineImage=it.mImageFile
                            }
                            else if(it is AudioTag) {
                                if (songMilli == 0 && (chosenTrack == null || it.mAudioFile == chosenTrack)) {
                                    try {
                                        val mmr = MediaMetadataRetriever()
                                        mmr.setDataSource(it.mAudioFile.mFile.absolutePath)
                                        val data = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                        if (data != null)
                                            songMilli = Integer.parseInt(data)
                                    } catch (e: Exception) {
                                        Log.e(BeatPrompterApplication.TAG, "Failed to extract duration metadata from media file.", e)
                                    }
                                }
                            }
                        }

                        // Contains only tags? Or contains nothing? Don't use it as a blank line.
                        if (strippedLine.trim().isNotEmpty() || chordsFound || pauseTime > 0 || lineImage != null) {
                            // removed lineImage=null from here
                            totalPauseTime += pauseTime
                            pauseTime = 0
                            // Line could potentially have been "{sometag} # comment"?
                            if (lineImage != null || chordsFound || !strippedLine.trim().startsWith("#")) {
                                realBarCount += bars
                                realLineCount++
                            }
                        }
                    }
                    ++lineNumber
                }
            } while(line!=null)

            if (songTime == Utils.TRACK_AUDIO_LENGTH_VALUE)
                songTime = songMilli.toLong()

            var negateResult = false
            if (totalPauseTime > songTime) {
                negateResult = true
                totalPauseTime = 0
            }
            var lineresult = (Utils.milliToNano(songTime - totalPauseTime).toDouble() / realLineCount.toDouble()).toLong()
            var barresult = (Utils.milliToNano(songTime - totalPauseTime).toDouble() / realBarCount.toDouble()).toLong()
            val trackresult = Utils.milliToNano(songMilli)
            if (negateResult) {
                lineresult = -lineresult
                barresult = -barresult
            }
            return SmoothScrollingTimings(lineresult, barresult, trackresult)
        } finally {
            try {
                br.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close song file.", ioe)
            }

        }
    }

    @Throws(IOException::class)
    fun parse(loadingSongFile: SongLoadInfo, cancelEvent: CancelEvent, songLoadHandler: Handler, registered: Boolean): Song {
        val parsingState= SongParsingState(loadingSongFile.scrollMode,this)
        val initialMIDIMessages = mutableListOf<OutgoingMessage>()
        var stopAddingStartupItems = false

        val chosenTrack = loadingSongFile.track
        val sst = getTimePerLineAndBar(chosenTrack)
        val timePerLine = sst.timePerLine
        val timePerBar = sst.timePerBar

        if (timePerLine < 0 || timePerBar < 0) {
            parsingState.mErrors.add(FileParseError(null, BeatPrompterApplication.getResourceString(R.string.pauseLongerThanSong)))
            sst.timePerLine = -timePerLine
            sst.timePerBar = -timePerBar
        }

        val sharedPrefs=BeatPrompterApplication.preferences
        val ignoreColorInfo = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue).toBoolean())
        var sendMidiClock = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false)
        val countInPref = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)))
        val metronomeContext = MetronomeContext.getMetronomeContextPreference(sharedPrefs)
        var backgroundColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)))
        var pulseColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_default)))
        var beatCounterColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)))
        val scrollMarkerColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_default)))
        var lyricColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_default)))
        var chordColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_default)))
        val annotationColour = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_default)))
        val customCommentsUser = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue))
        val showChords = sharedPrefs.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue).toBoolean())
        val triggerContext = TriggerOutputContext.valueOf(sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key), BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue))!!)
        val defaultMIDIOutputChannelPrefValue = sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)))
        val defaultMIDIOutputChannel = Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue)

        val br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
        try {
            var line: String?=""

            val tagsSet = HashSet<String>()

            // ONE SHOT
            var chosenAudioFile: AudioFile? = null
            var chosenAudioVolume = 100
            var count = countInPref
            var trackOffset: Long = 0
            // TIME
            var beatsToAdjust = 0
            val rolloverBeats = mutableListOf<BeatEvent>()
            var pauseTime: Long
            // COMMENT
            val comments = mutableListOf<Comment>()
            var lineImage: ImageFile? = null

            var metronomeOn = metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.On
            if (metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.OnWhenNoTrack && chosenTrack!=null)
                metronomeOn = true

            var currentBeat = 0
            var nanosecondsPerBeat: Long

            var imageScalingMode = ImageScalingMode.Stretch
            var currentTime: Long = 0
            var midiBeatCounter = 0
            var firstEvent: BaseEvent? = null
            var lastEvent: BaseEvent? = null
            var firstLine: Line? = null

            // There must ALWAYS be a style and time event at the start, before any line events.
            // Create them from defaults even if there are no relevant tags in the file.
            var createColorEvent = true
            var lineCounter = 0
            var displayLineCounter = 0
            songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED, 0, mLines).sendToTarget()
            while (line!=null && !cancelEvent.isCancelled) {
                line = br.readLine()
                if(line!=null) {
                    val fileLine= SongFileLine(line, ++lineCounter, parsingState)
                    pauseTime = 0
                    // Ignore comments.
                    if(fileLine.isComment)
                        continue

                    fileLine.mTags.filterNot{it is ChordTag }.forEach {
                        if (it.isColorTag && !ignoreColorInfo)
                            createColorEvent = true
                        if (it.isOneShotTag && tagsSet.contains(it.mName))
                            parsingState.mErrors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.oneShotTagDefinedTwice, it.mName)))

                        if(it is ImageTag)
                        {
                            if (lineImage != null)
                                parsingState.mErrors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)))
                            else {
                                lineImage = it.mImageFile
                                imageScalingMode=it.mImageScalingMode
                            }
                        }
                        else if(it is AudioTag)
                        {
                            chosenAudioFile = it.mAudioFile
                            chosenAudioVolume = it.mVolume
                        }
                        else if(it is SendMIDIClockTag)
                            sendMidiClock=true
                        else if(it is CountTag)
                            count=it.mCount
                        else if(it is BackgroundColorTag)
                            backgroundColour=it.mColor
                        else if(it is PulseColorTag)
                            pulseColour=it.mColor
                        else if(it is LyricsColorTag)
                            lyricColour=it.mColor
                        else if(it is ChordsColorTag)
                            chordColour=it.mColor
                        else if(it is BeatCounterColorTag)
                            beatCounterColour=it.mColor
                        else if(it is CommentTag)
                        {
                            val comment = Comment(it.mComment,it.mAudience)
                            if (stopAddingStartupItems) {
                                val ce = CommentEvent(currentTime, comment)
                                if (firstEvent == null)
                                    firstEvent = ce
                                else
                                    lastEvent!!.add(ce)
                                lastEvent = ce
                            } else
                                if (comment.isIntendedFor(customCommentsUser))
                                    comments.add(comment)
                        }
                        else if(it is PauseTag)
                            pauseTime=it.mDuration
                        else if(it is MIDIEventTag)
                        {
                            if (displayLineCounter < DEMO_LINE_COUNT || registered)
                            {
                                val midiEvent=it.mEvent
                                if (stopAddingStartupItems) {
                                    if (firstEvent == null)
                                        firstEvent = midiEvent
                                    else
                                        lastEvent!!.add(midiEvent)
                                    lastEvent = midiEvent
                                } else {
                                    initialMIDIMessages.addAll(midiEvent.mMessages)
                                    if (midiEvent.mOffset != EventOffset.NoOffset)
                                        parsingState.mErrors.add(FileParseError(it, BeatPrompterApplication.getResourceString(R.string.midi_offset_before_first_line)))
                                }
                            }
                        }
                        tagsSet.add(it.mName)
                    }
                    val chordTags=fileLine.chordTags
                    val nonChordTags=fileLine.nonChordTags
                    val chordsFound = showChords && !chordTags.isEmpty()
                    val chordsFoundButNotShowingThem=!showChords && chordsFound
                    val tagsToProcess=if (chordsFoundButNotShowingThem) nonChordTags else fileLine.mTags
                    val createLine= (fileLine.mTaglessLine.isNotEmpty() || chordsFoundButNotShowingThem || chordsFound || lineImage != null)

                    // Contains only tags? Or contains nothing? Don't use it as a blank line.
                    if (createLine || pauseTime > 0) {
                        // We definitely have a line event!
                        // Deal with style/time/comment events now.
                        if (createColorEvent) {
                            val styleEvent = ColorEvent(currentTime, backgroundColour, pulseColour, lyricColour, chordColour, annotationColour, beatCounterColour, scrollMarkerColour)
                            if (currentTime == 0L && firstEvent != null) {
                                // First event should ALWAYS be a color event.
                                val oldFirstEvent = firstEvent!!
                                styleEvent.add(oldFirstEvent)
                                firstEvent = styleEvent
                            } else {
                                if (firstEvent == null)
                                    firstEvent = styleEvent
                                else
                                    lastEvent!!.add(styleEvent)
                                lastEvent = styleEvent
                            }
                            createColorEvent = false
                        }

                        if (lastEvent!!.mPrevLineEvent == null) {
                            // There haven't been any line events yet.
                            // So the comments that have been gathered up until now
                            // can just be shown on the song startup screen.
                            stopAddingStartupItems = true
                        }

                        val bpbThisLine = fileLine.mBeatInfo.mBPB
                        val bpmThisLine = fileLine.mBeatInfo.mBPM
                        val scrollBeatThisLine = fileLine.mBeatInfo.mScrollBeat
                        val scrollBeatOffsetThisLine = fileLine.mBeatInfo.mScrollBeatOffset
                        val bars = fileLine.mBeatInfo.mBPL

                        var displayLine=fileLine.mTaglessLine
                        if (lineImage != null && (displayLine.isNotEmpty() || chordsFound))
                            parsingState.mErrors.add(FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.text_found_with_image)))
                        if (displayLine.isEmpty() && !chordsFound)
                            displayLine = "▼"

                        if (createLine) {
                            displayLineCounter++

                            // Won't pay? We'll take it away!
                            if (displayLineCounter > DEMO_LINE_COUNT && !registered) {
                                displayLine = BeatPrompterApplication.getResourceString(R.string.please_buy)
                                lineImage = null
                            }

                            var lastLine: Line? = null
                            if (lastEvent!!.mPrevLineEvent != null)
                                lastLine = lastEvent!!.mPrevLineEvent!!.mLine
                            var lastScrollBeatOffset = 0
                            var lastBPB = bpbThisLine
                            var lastScrollBeat = scrollBeatThisLine
                            if (lastLine != null) {
                                lastBPB = lastLine.mBeatInfo.mBPB
                                lastScrollBeatOffset = lastLine.mBeatInfo.mScrollBeatOffset
                                lastScrollBeat = lastLine.mBeatInfo.mScrollBeat
                            }
                            val scrollBeatDifference = scrollBeatThisLine - bpbThisLine - (lastScrollBeat - lastBPB)

                            var beatsForThisLine = bpbThisLine * bars
                            val simpleBeatsForThisLine = beatsForThisLine
                            beatsForThisLine += scrollBeatOffsetThisLine
                            beatsForThisLine += scrollBeatDifference
                            beatsForThisLine -= lastScrollBeatOffset

                            nanosecondsPerBeat = if (bpmThisLine > 0)
                                Utils.nanosecondsPerBeat(bpmThisLine)
                            else
                                0

                            var totalLineTime: Long
                            totalLineTime = if (pauseTime > 0)
                                Utils.milliToNano(pauseTime)
                            else
                                beatsForThisLine * nanosecondsPerBeat
                            if (totalLineTime == 0L || parsingState.mBeatInfo.mScrollingMode === ScrollingMode.Smooth)
                                totalLineTime = timePerBar * bars

                            val lineObj: Line
                            if (lineImage != null) {
                                lineObj = ImageLine(currentTime, totalLineTime, lineImage!!, imageScalingMode, lastEvent!!.mPrevColorEvent!!,fileLine.mBeatInfo)
                                lineImage = null
                            } else
                                lineObj = TextLine(currentTime, totalLineTime, displayLine, tagsToProcess, lastEvent!!.mPrevColorEvent!!, fileLine.mBeatInfo)

                            lastLine?.insertAfter(lineObj)
                            val lineEvent = lineObj.mLineEvent
                            if (firstLine == null)
                                firstLine = lineObj
                            lastEvent!!.insertEvent(lineEvent)

                            // generate beats ...

                            // if a pause is specified on a line, it replaces the actual beats for that line.
                            if (pauseTime > 0) {
                                currentTime = generatePause(pauseTime, lastEvent, currentTime)
                                lastEvent = lastEvent!!.lastEvent
                                lineEvent.mLine.mYStartScrollTime = currentTime - nanosecondsPerBeat
                                lineEvent.mLine.mYStopScrollTime = currentTime
                            } else if (bpmThisLine > 0 && parsingState.mBeatInfo.mScrollingMode !== com.stevenfrew.beatprompter.ScrollingMode.Smooth) {
                                var finished = false
                                var beatThatWeWillScrollOn = 0
                                val rolloverBeatCount = rolloverBeats.size
                                val beatsToAdjustCount = beatsToAdjust
                                if (beatsToAdjust > 0) {
                                    // We have N beats to adjust.
                                    // For the previous N beatevents, set the BPB to the new BPB.
                                    var lastBeatEvent = lastEvent!!.mPrevBeatEvent
                                    while (lastBeatEvent != null && beatsToAdjust > 0) {
                                        lastBeatEvent.mBPB = bpbThisLine
                                        beatsToAdjust--
                                        lastBeatEvent = if (lastBeatEvent.mPrevEvent != null)
                                            lastBeatEvent.mPrevEvent!!.mPrevBeatEvent
                                        else
                                            null
                                    }
                                    beatsToAdjust = 0
                                }

                                var currentBarBeat = 0
                                while (!finished && currentBarBeat < beatsForThisLine) {
                                    val beatsRemaining = beatsForThisLine - currentBarBeat
                                    beatThatWeWillScrollOn = if (beatsRemaining > bpbThisLine)
                                        -1
                                    else
                                        (currentBeat + (beatsRemaining - 1)) % bpbThisLine
                                    val beatEvent: BeatEvent
                                    var rolloverBPB = 0
                                    var rolloverBeatLength: Long = 0
                                    if (rolloverBeats.isEmpty())
                                        beatEvent = BeatEvent(currentTime, bpmThisLine, bpbThisLine, bars, currentBeat, metronomeOn, beatThatWeWillScrollOn)
                                    else {
                                        beatEvent = rolloverBeats[0]
                                        beatEvent.mWillScrollOnBeat = beatThatWeWillScrollOn
                                        rolloverBPB = beatEvent.mBPB
                                        rolloverBeatLength = Utils.nanosecondsPerBeat(beatEvent.mBPM)
                                        rolloverBeats.removeAt(0)
                                    }
                                    lastEvent!!.insertEvent(beatEvent)
                                    val beatTimeLength = if (rolloverBeatLength == 0L) nanosecondsPerBeat else rolloverBeatLength
                                    val nanoPerBeat = beatTimeLength / 4.0
                                    // generate MIDI beats.
                                    if (lastBeatBlock == null || nanoPerBeat != lastBeatBlock.nanoPerBeat) {
                                        lastBeatBlock = BeatBlock(beatEvent.mEventTime, midiBeatCounter++, nanoPerBeat)
                                        val beatBlock = lastBeatBlock
                                        beatBlocks.add(beatBlock)
                                    }

                                    if (currentBarBeat == beatsForThisLine - 1) {
                                        lineEvent.mLine.mYStartScrollTime = if (parsingState.mBeatInfo.mScrollingMode === ScrollingMode.Smooth) lineEvent.mEventTime else currentTime
                                        lineEvent.mLine.mYStopScrollTime = currentTime + nanosecondsPerBeat
                                        finished = true
                                    }
                                    currentTime += beatTimeLength
                                    currentBeat++
                                    if (currentBeat == (if (rolloverBPB > 0) rolloverBPB else bpbThisLine))
                                        currentBeat = 0
                                    ++currentBarBeat
                                }

                                beatsForThisLine -= rolloverBeatCount
                                beatsForThisLine += beatsToAdjustCount
                                if (beatsForThisLine > simpleBeatsForThisLine) {
                                    // We need to store some information so that the next line can adjust the rollover beats.
                                    beatsToAdjust = beatsForThisLine - simpleBeatsForThisLine
                                } else if (beatsForThisLine < simpleBeatsForThisLine) {
                                    // We need to generate a few beats to store for the next line to use.
                                    rolloverBeats.clear()
                                    var rolloverCurrentBeat = currentBeat
                                    var rolloverCurrentTime = currentTime
                                    for (f in beatsForThisLine until simpleBeatsForThisLine) {
                                        rolloverBeats.add(BeatEvent(rolloverCurrentTime, bpmThisLine, bpbThisLine, bars, rolloverCurrentBeat++, metronomeOn, beatThatWeWillScrollOn))
                                        rolloverCurrentTime += nanosecondsPerBeat
                                        if (rolloverCurrentBeat == bpbThisLine)
                                            rolloverCurrentBeat = 0
                                    }
                                }
                            } else {
                                lineEvent.mLine.mYStartScrollTime = currentTime
                                currentTime += totalLineTime
                                lineEvent.mLine.mYStopScrollTime = currentTime
                            }
                        } else if (pauseTime > 0)
                            currentTime = generatePause(pauseTime, lastEvent, currentTime)

                        lastEvent = lastEvent!!.lastEvent
                    }
                }
                songLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_READ, lineCounter, mLines).sendToTarget()
            }

            var countTime: Long = 0
            // Create count events
            if (firstLine?.mBeatInfo?.mScrollingMode==ScrollingMode.Beat) {
                val firstBeatEvent = firstEvent!!.firstBeatEvent
                val countbpm = firstBeatEvent?.mBPM ?: 120.0
                val countbpb = firstBeatEvent?.mBPB ?: 4
                val countbpl = firstBeatEvent?.mBPL ?: 1
                var insertAfterEvent = firstEvent
                if (count > 0) {
                    val nanoPerBeat = Utils.nanosecondsPerBeat(countbpm)
                    for (f in 0 until count)
                        for (g in 0 until countbpb) {
                            val countEvent = BeatEvent(countTime, countbpm, countbpb, countbpl, g, metronomeContext === com.stevenfrew.beatprompter.MetronomeContext.DuringCountIn || metronomeOn, if (f == count - 1) countbpb - 1 else -1)
                            insertAfterEvent!!.insertAfter(countEvent)
                            insertAfterEvent = countEvent
                            countTime += nanoPerBeat
                        }
                    insertAfterEvent!!.offsetLaterEvents(countTime)
                } else {
                    val baseBeatEvent = BeatEvent(0, countbpm, countbpb, countbpl, countbpb, false, -1)
                    firstEvent!!.insertAfter(baseBeatEvent)
                }
            }
            var trackEvent: AudioEvent? = null
            if (chosenAudioFile != null) {
                trackOffset = Utils.milliToNano(trackOffset.toInt()) // milli to nano
                trackOffset += countTime
                val eventBefore = firstEvent!!.findEventOnOrBefore(trackOffset)
                trackEvent = AudioEvent(if (trackOffset < 0) 0 else trackOffset)
                eventBefore!!.insertAfter(trackEvent)
                if (trackOffset < 0)
                    trackEvent.offsetLaterEvents(Math.abs(trackOffset))
            }
            if (firstLine?.lastLine!!.mBeatInfo.mScrollingMode === ScrollingMode.Beat) {
                // Last Y scroll should never happen. No point scrolling last line offscreen.
                val mLastLine = firstLine!!.lastLine
                mLastLine.mYStopScrollTime = Long.MAX_VALUE
                mLastLine.mYStartScrollTime = mLastLine.mYStopScrollTime
            }

            // Nothing at all in the song file? We at least want the colors set right.
            if (firstEvent == null)
                firstEvent = ColorEvent(currentTime, backgroundColour, pulseColour, lyricColour, chordColour, annotationColour, beatCounterColour, scrollMarkerColour)

            val reallyTheLastEvent = firstEvent!!.lastEvent
            // In beat mode, or in any other mode where we're using a backing track, let's have an end event.
            if (trackEvent != null || parsingState.mBeatInfo.mScrollingMode !== com.stevenfrew.beatprompter.ScrollingMode.Manual) {
                var trackEndTime: Long = 0
                if (trackEvent != null)
                    trackEndTime = trackEvent.mEventTime + sst.trackLength
                // The end event will be where the final beat occurs.
                // But there is a chance that the audio track is longer than that.
                val endEvent = EndEvent(Math.max(currentTime, trackEndTime))
                reallyTheLastEvent.add(endEvent)
            }

            if (triggerContext === com.stevenfrew.beatprompter.midi.TriggerOutputContext.Always || triggerContext === com.stevenfrew.beatprompter.midi.TriggerOutputContext.ManualStartOnly && !loadingSongFile.startedByMIDITrigger) {
                if (mProgramChangeTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mProgramChangeTrigger.getMIDIMessages(defaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }

                if (mSongSelectTrigger.isSendable())
                    try {
                        initialMIDIMessages.addAll(mSongSelectTrigger.getMIDIMessages(defaultMIDIOutputChannel))
                    } catch (re: ResolutionException) {
                        parsingState.mErrors.add(FileParseError(lineCounter, re.message))
                    }
            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(firstEvent, parsingState.mErrors)

            val song = Song(this, chosenAudioFile, chosenAudioVolume, comments, firstEvent!!, firstLine!!, parsingState.mErrors, loadingSongFile.scrollMode, sendMidiClock, loadingSongFile.startedByBandLeader, loadingSongFile.nextSong, loadingSongFile.sourceDisplaySettings.mOrientation, initialMIDIMessages, beatBlocks, firstLine.mBeatInfo.mBPB, count)
            song.doMeasurements(Paint(), cancelEvent, songLoadHandler, loadingSongFile.nativeDisplaySettings, loadingSongFile.sourceDisplaySettings)
            return song
        } finally {
            try {
                br.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close song file.", ioe)
            }
        }
    }
    */

    fun matchesTrigger(trigger: SongTrigger): Boolean {
        return mSongSelectTrigger == trigger || mProgramChangeTrigger == trigger
    }

    companion object {
        private var thePrefix=BeatPrompterApplication.getResourceString(R.string.lowerCaseThe)+" "

        fun sortableString(inStr:String?):String
        {
                val str=inStr?.toLowerCase()
                if(str!=null)
                {
                    return if(str.startsWith(thePrefix))
                        str.substring(thePrefix.length)
                    else
                        str
                }
                return ""
        }
    }
}