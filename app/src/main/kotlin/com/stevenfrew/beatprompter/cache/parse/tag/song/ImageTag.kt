package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.ImageScalingMode
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import java.io.File

@OncePerLine
class ImageTag internal constructor(name:String,lineNumber:Int,position:Int,value:String): ValueTag(name,lineNumber,position,value) {
    val mFilename:String
    val mImageScalingMode:ImageScalingMode

    init {
        var imageName = value
        val colonindex = imageName.indexOf(":")
        var imageScalingMode = ImageScalingMode.Stretch
        if (colonindex != -1 && colonindex < imageName.length - 1) {
            val strScalingMode = imageName.substring(colonindex + 1)
            imageName = imageName.substring(0, colonindex)
            try {
                imageScalingMode=ImageScalingMode.valueOf(strScalingMode.toLowerCase().capitalize())
            }
            catch(e:Exception) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.unknown_image_scaling_mode))
            }
        }
        mFilename = File(imageName).name
/*        val imageFile: File
        val mappedImage = SongList.mCachedCloudFiles.getMappedImageFile(image, tempImageFileCollection)
        if (mappedImage == null)
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image))
        else {
            imageFile = File(sourceFile.parent, mappedImage.mFile.name)
            if (!imageFile.exists())
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image))
        }
        mImageFile = mappedImage*/
        mImageScalingMode=imageScalingMode
    }
}