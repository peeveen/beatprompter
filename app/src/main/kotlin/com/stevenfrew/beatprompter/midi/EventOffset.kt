package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class EventOffset {
    val mAmount:Int
    val mOffsetType:EventOffsetType
    val mSourceTag: Tag?

    constructor(offsetString: String?, sourceTag: Tag, errors: MutableList<FileParseError>)
    {
        var amount=0
        var offsetType:EventOffsetType=EventOffsetType.Milliseconds
        var str = offsetString
        if (str != null) {
            str = str.trim()
            if (!str.isEmpty()) {
                try {
                    amount = Integer.parseInt(str)
                    offsetType = EventOffsetType.Milliseconds
                } catch (e: Exception) {
                    // Might be in the beat format
                    var diff = 0
                    var bErrorAdded = false
                    str.toCharArray().forEach{
                        if (it == '<')
                            --diff
                        else if (it == '>')
                            ++diff
                        else if (!bErrorAdded) {
                            bErrorAdded = true
                            errors.add(FileParseError(sourceTag, BeatPrompterApplication.getResourceString(R.string.non_beat_characters_in_midi_offset)))
                        }
                    }
                    amount = diff
                    offsetType = EventOffsetType.Beats
                }

                if (Math.abs(amount) > 16 && offsetType == EventOffsetType.Beats) {
                    amount = Math.abs(amount) / amount * 16
                    errors.add(FileParseError(sourceTag, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)))
                } else if (Math.abs(amount) > 10000 && offsetType == EventOffsetType.Milliseconds) {
                    amount = Math.abs(amount) / amount * 10000
                    errors.add(FileParseError(sourceTag, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)))
                }
            }
        }
        mAmount=amount
        mOffsetType=offsetType
        mSourceTag=sourceTag
    }

    private constructor(amount:Int,offsetType:EventOffsetType)
    {
        mAmount=amount
        mOffsetType=offsetType
        mSourceTag=null
    }

    companion object
    {
        val NoOffset=EventOffset(0,EventOffsetType.Milliseconds)
    }
}