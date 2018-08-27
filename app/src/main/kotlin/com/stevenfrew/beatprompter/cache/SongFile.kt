package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.*

@CacheXmlTag("song")
class SongFile constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mLines:Int, val mTitle:String, val mArtist:String, val mKey:String, val mBPM:Double, duration:Long, val mAudioFiles:List<String>, val mImageFiles:List<String>,val mTags:Set<String>, private val mProgramChangeTrigger:SongTrigger, private val mSongSelectTrigger:SongTrigger, errors:List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors) {
    val mNormalizedArtist=mArtist.normalize()
    val mNormalizedTitle=mTitle.normalize()
    val mSortableArtist=sortableString(mArtist)
    val mSortableTitle=sortableString(mTitle)
    val mIsSmoothScrollable=duration>0
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
            val parsingState=SongParsingState(SongScrollingMode.Beat,this)
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