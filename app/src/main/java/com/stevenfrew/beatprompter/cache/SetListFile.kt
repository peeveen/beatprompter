package com.stevenfrew.beatprompter.cache

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

class SetListFile : CachedCloudFile {

    @JvmField var mSongTitles: MutableList<String> = ArrayList()
    @JvmField var mSetTitle=""

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: SuccessfulCloudDownloadResult) : super(result.mDownloadedFile, result.mCloudFileInfo) {
        parseSetListFileInfo()
    }

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(element: Element) : super(element) {
        parseSetListFileInfo()
    }

    private fun getSetNameFromLine(line: String, lineNumber: Int): String? {
        return getTokenValue(line, lineNumber, "set")
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun parseSetListFileInfo() {
        var br: BufferedReader? = null
        try {
            br = BufferedReader(InputStreamReader(FileInputStream(mFile)))
            var setTitle: String? = null
            var line: String?
            var lineNumber = 0
            do {
                line = br.readLine()
                if(line!=null) {
                    line = line.trim { it <= ' ' }
                    if (line.startsWith("#"))
                        continue
                    if (setTitle == null || setTitle.isEmpty())
                        setTitle = getSetNameFromLine(line, lineNumber)
                    else
                        mSongTitles.add(line)
                    ++lineNumber
                }
            } while(line!=null)

            if (setTitle == null || setTitle.isEmpty())
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_set_list, mID))
            else
                mSetTitle = setTitle
        } catch (ioe: IOException) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_set_list, mID))
        } finally {
            try {
                br?.close()
            } catch (ioe: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close set list file", ioe)
            }

        }
    }

    override fun writeToXML(d: Document, element: Element) {
        val setListFileElement = d.createElement(SETLISTFILE_ELEMENT_TAG_NAME)
        super.writeToXML(setListFileElement)
        setListFileElement.setAttribute(SET_TITLE_ATTRIBUTE_NAME, mSetTitle)
        element.appendChild(setListFileElement)
    }

    companion object {
        const val SETLISTFILE_ELEMENT_TAG_NAME = "set"
        private const val SET_TITLE_ATTRIBUTE_NAME = "title"
    }
}
