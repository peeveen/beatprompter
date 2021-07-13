package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.util.normalize

@CacheXmlTag("song")
/**
 * A song file in the cache.
 */
class SongFile(cachedFile: CachedFile,
               val mLines: Int,
               val mBars: Int,
               val mTitle: String,
               val mArtist: String,
               val mKey: String,
               val mBPM: Double,
               val mDuration: Long,
               val mMixedMode: Boolean,
               val mTotalPauses: Long,
               val mAudioFiles: List<String>,
               val mImageFiles: List<String>,
               val mTags: Set<String>,
               val mProgramChangeTrigger: SongTrigger,
               val mSongSelectTrigger: SongTrigger,
               val mFilterOnly: Boolean,
               errors: List<FileParseError>)
    : CachedTextFile(cachedFile, errors) {
    val mNormalizedArtist = mArtist.normalize()
    val mNormalizedTitle = mTitle.normalize()
    val mSortableArtist = sortableString(mArtist)
    val mSortableTitle = sortableString(mTitle)
    val isSmoothScrollable
        get() = mDuration > 0
    val isBeatScrollable
        get() = mBPM > 0.0
    val bestScrollingMode
        get() = when {
            isBeatScrollable -> ScrollingMode.Beat
            isSmoothScrollable -> ScrollingMode.Smooth
            else -> ScrollingMode.Manual
        }

    fun matchesTrigger(trigger: SongTrigger): Boolean {
        return mSongSelectTrigger == trigger || mProgramChangeTrigger == trigger
    }

    companion object {
        private var thePrefix = "${BeatPrompter.getResourceString(R.string.lowerCaseThe)} "

        fun sortableString(inStr: String?): String {
            return inStr?.lowercase()?.removePrefix(thePrefix) ?: ""
        }
    }
}