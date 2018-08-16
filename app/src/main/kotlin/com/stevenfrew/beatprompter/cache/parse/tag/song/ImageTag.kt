package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.ImageFile
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import java.io.File

class ImageTag internal constructor(name:String,lineNumber:Int,position:Int,value:String,sourceFile:File,tempImageFileCollection: List<ImageFile>): Tag(name,lineNumber,position) {
    val mImageFile:ImageFile

    init {
        var imageName = value
        val colonindex = imageName.indexOf(":")
        if (colonindex != -1 && colonindex < imageName.length - 1)
            imageName = imageName.substring(0, colonindex)
        val image = File(imageName).name
        val imageFile: File
        val mappedImage = SongList.mCachedCloudFiles.getMappedImageFilename(image, tempImageFileCollection)
        if (mappedImage == null)
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image))
        else {
            imageFile = File(sourceFile.parent, mappedImage.mFile.name)
            if (!imageFile.exists())
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile, image))
        }
        mImageFile = mappedImage
    }
}