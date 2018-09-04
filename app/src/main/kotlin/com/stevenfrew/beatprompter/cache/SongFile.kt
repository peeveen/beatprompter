package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.*

@CacheXmlTag("song")
class SongFile constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mLines:Int, val mBars:Int, val mTitle:String, val mArtist:String, val mKey:String, val mBPM:Double, val mDuration:Long, val mMixedMode:Boolean, val mTotalPauses:Long, val mAudioFiles:List<String>, val mImageFiles:List<String>,val mTags:Set<String>, private val mProgramChangeTrigger:SongTrigger, private val mSongSelectTrigger:SongTrigger, val mFilterOnly:Boolean, errors:List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors) {
    val mNormalizedArtist=mArtist.normalize()
    val mNormalizedTitle=mTitle.normalize()
    val mSortableArtist=sortableString(mArtist)
    val mSortableTitle=sortableString(mTitle)
    val isSmoothScrollable
        get() = mDuration>0
    val isBeatScrollable
        get() = mBPM>0.0
    val bestScrollingMode
        get() = if (isBeatScrollable) ScrollingMode.Beat else if (isSmoothScrollable) ScrollingMode.Smooth else ScrollingMode.Manual

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