package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.cache.parse.tag.song.*
import com.stevenfrew.beatprompter.midi.SongTrigger

@ParseTags(TimeTag::class, ImageTag::class, MIDISongSelectTriggerTag::class, MIDIProgramChangeTriggerTag::class,
        TitleTag::class, ArtistTag::class, KeyTag::class, PauseTag::class, TagTag::class, FilterOnlyTag::class,
        BarMarkerTag::class, BarsTag::class, BeatsPerMinuteTag::class, BeatsPerBarTag::class, BarsPerLineTag::class,
        ScrollBeatModifierTag::class, ScrollBeatTag::class, BeatStartTag::class, BeatStopTag::class, AudioTag::class,
        ChordTag::class)
@IgnoreTags(LegacyTag::class, SendMIDIClockTag::class, CommentTag::class, CountTag::class,
        StartOfHighlightTag::class, EndOfHighlightTag::class)
/**
 * Song file parser. This returns ENOUGH information to display the songs in the song list.
 */
class SongInfoParser constructor(cachedCloudFileDescriptor: CachedFileDescriptor)
    : SongFileParser<SongFile>(cachedCloudFileDescriptor, ScrollingMode.Beat, false, false) {
    private var mTitle: String? = null
    private var mArtist: String? = null
    private var mKey: String? = null
    private var mFirstChord: String? = null
    private var mBPM: Double = 0.0
    private var mBars: Int = 0
    private var mBeats: Int = 0
    private var mTotalPause: Long = 0L
    private var mDuration: Long = 0L
    private val mAudioFiles = mutableListOf<String>()
    private val mImageFiles = mutableListOf<String>()
    private var mFilterOnly = false
    private val mTags = mutableListOf<String>()
    private var mMIDIProgramChangeTrigger: SongTrigger? = null
    private var mMIDISongSelectTrigger: SongTrigger? = null
    private var mMixedMode: Boolean = false
    private var mLines = 0

    override fun parseLine(line: TextFileLine<SongFile>) {
        super.parseLine(line)
        ++mLines

        val tagSequence = line.mTags.asSequence()
        val titleTag = tagSequence.filterIsInstance<TitleTag>().firstOrNull()
        val artistTag = tagSequence.filterIsInstance<ArtistTag>().firstOrNull()
        val keyTag = tagSequence.filterIsInstance<KeyTag>().firstOrNull()
        val chordTag = tagSequence.filterIsInstance<ChordTag>().firstOrNull()
        val midiSongSelectTriggerTag = tagSequence.filterIsInstance<MIDISongSelectTriggerTag>().firstOrNull()
        val midiProgramChangeTriggerTag = tagSequence.filterIsInstance<MIDIProgramChangeTriggerTag>().firstOrNull()
        val bpmTag = tagSequence.filterIsInstance<BeatsPerMinuteTag>().firstOrNull()
        val filterOnlyTag = tagSequence.filterIsInstance<FilterOnlyTag>().firstOrNull()
        val beatStartTag = tagSequence.filterIsInstance<BeatStartTag>().firstOrNull()
        val beatStopTag = tagSequence.filterIsInstance<BeatStopTag>().firstOrNull()
        val timeTag = tagSequence.filterIsInstance<TimeTag>().firstOrNull()
        val audioTags = tagSequence.filterIsInstance<AudioTag>()
        val imageTags = line.mTags.filterIsInstance<ImageTag>()
        val pauseTag = tagSequence.filterIsInstance<PauseTag>().firstOrNull()
        val tagTags = tagSequence.filterIsInstance<TagTag>()

        if (titleTag != null)
            mTitle = titleTag.mTitle

        if (artistTag != null)
            mArtist = artistTag.mArtist

        if (keyTag != null)
            mKey = keyTag.mKey

        if (filterOnlyTag != null)
            mFilterOnly = true

        if (chordTag != null)
            if (mFirstChord == null && chordTag.mValidChord)
                mFirstChord = chordTag.mName

        if (midiSongSelectTriggerTag != null)
            mMIDISongSelectTrigger = midiSongSelectTriggerTag.mTrigger

        if (midiProgramChangeTriggerTag != null)
            mMIDIProgramChangeTrigger = midiProgramChangeTriggerTag.mTrigger

        if (bpmTag != null)
            mBPM = bpmTag.mBPM

        if (pauseTag != null)
            mTotalPause += pauseTag.mDuration

        if (timeTag != null)
            mDuration = timeTag.mDuration

        if (beatStartTag != null || beatStopTag != null)
            mMixedMode = true

        if (!line.mLineWithNoTags.isBlank() || imageTags.isNotEmpty() || chordTag != null) {
            mBars += mCurrentLineBeatInfo.mBPL
            mBeats += mCurrentLineBeatInfo.mBeats
        }

        mAudioFiles.addAll(audioTags.map { it.mFilename })
        mImageFiles.addAll(imageTags.map { it.mFilename })
        mTags.addAll(tagTags.map { it.mTag })
    }

    override fun getResult(): SongFile {
        if (mTitle.isNullOrBlank())
            throw InvalidBeatPrompterFileException(R.string.noTitleFound, mCachedCloudFileDescriptor.mName)
        if (mArtist.isNullOrBlank())
            mArtist = ""
        val key =
                if (mKey.isNullOrBlank())
                    if (mFirstChord.isNullOrBlank())
                        ""
                    else
                        mFirstChord!!
                else
                    mKey!!

        return SongFile(mCachedCloudFileDescriptor, mLines, mBars, mTitle!!, mArtist!!, key, mBPM, mDuration, mMixedMode, mTotalPause, mAudioFiles, mImageFiles, mTags.toSet(), mMIDIProgramChangeTrigger
                ?: SongTrigger.DEAD_TRIGGER, mMIDISongSelectTrigger
                ?: SongTrigger.DEAD_TRIGGER, mFilterOnly, mErrors)
    }
}
