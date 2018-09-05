package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.ImageScalingMode
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.*
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.splitAndTrim
import java.io.File

@OncePerLine
@TagName("image")
@TagType(Type.Directive)
/**
 * Tag that defines an image to use for the current line instead of text.
 */
class ImageTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): ValueTag(name,lineNumber,position,value) {
    val mFilename:String
    val mImageScalingMode:ImageScalingMode

    init {
        val bits=value.splitAndTrim(":")
        mFilename = File(bits[0]).name
        mImageScalingMode=if(bits.size>1) parseImageScalingMode(bits[1]) else ImageScalingMode.Stretch
    }

    companion object {
        @Throws(MalformedTagException::class)
        fun parseImageScalingMode(value:String):ImageScalingMode {
            try {
                return ImageScalingMode.valueOf(value.toLowerCase().capitalize())
            }
            catch(e:Exception) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unknown_image_scaling_mode))
            }
        }
    }
}