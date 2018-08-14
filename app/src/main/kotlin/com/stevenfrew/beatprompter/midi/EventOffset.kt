package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.FileParseError
import com.stevenfrew.beatprompter.cache.Tag
import java.util.ArrayList

class EventOffset(offsetString: String?, sourceTag: Tag, errors: ArrayList<FileParseError>) {
    var mSourceTag = sourceTag
    var mAmount: Int = 0
    var mOffsetType: EventOffsetType=EventOffsetType.Milliseconds

    init {
        var str = offsetString
        if (str != null) {
            str = str.trim { it <= ' ' }
            if (!str.isEmpty()) {
                try {
                    mAmount = Integer.parseInt(str)
                    mOffsetType = EventOffsetType.Milliseconds
                } catch (e: Exception) {
                    // Might be in the beat format
                    var diff = 0
                    var bErrorAdded = false
                    for (f in 0 until str.length) {
                        val c = str[f]
                        if (c == '<')
                            --diff
                        else if (c == '>')
                            ++diff
                        else if (!bErrorAdded) {
                            bErrorAdded = true
                            errors.add(FileParseError(mSourceTag, BeatPrompterApplication.getResourceString(R.string.non_beat_characters_in_midi_offset)))
                        }
                    }
                    mAmount = diff
                    mOffsetType = EventOffsetType.Beats
                }

                if (Math.abs(mAmount) > 16 && mOffsetType == EventOffsetType.Beats) {
                    mAmount = Math.abs(mAmount) / mAmount * 16
                    errors.add(FileParseError(mSourceTag, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)))
                } else if (Math.abs(mAmount) > 10000 && mOffsetType == EventOffsetType.Milliseconds) {
                    mAmount = Math.abs(mAmount) / mAmount * 10000
                    errors.add(FileParseError(mSourceTag, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)))
                }
            }
        }
    }
}