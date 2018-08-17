package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import com.stevenfrew.beatprompter.midi.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.util.ArrayList

class MIDIAliasFile : CachedCloudFile {

    var mAliasSet: AliasSet
    // The errors that were found in the file.
    var mErrors = ArrayList<FileParseError>()

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: SuccessfulCloudDownloadResult) : super(result.mDownloadedFile, result.mCloudFileInfo) {
        mAliasSet = readAliasFile(this, mID, mErrors)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(element: Element) : super(element) {
        mAliasSet = readAliasFile(this, mID, mErrors)
    }

    override fun writeToXML(d: Document, element: Element) {
        val aliasFileElement = d.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME)
        super.writeToXML(aliasFileElement)
        element.appendChild(aliasFileElement)
    }

    companion object {
        const val MIDIALIASFILE_ELEMENT_TAG_NAME = "midialiases"

        @Throws(InvalidBeatPrompterFileException::class)
        private fun readAliasFile(file:MIDIAliasFile, filename: String, midiParsingErrors: MutableList<FileParseError>): AliasSet {
            val parsingState= MIDIAliasParsingState(file)
            try {
                BufferedReader(InputStreamReader(FileInputStream(file.mFile))).use {
                    var line: String?
                    var lineNumber = 0
                    do {
                        line = it.readLine()
                        if(line!=null) {
                            val fileLine= MIDIAliasFileLine(line, ++lineNumber,parsingState)
                            if(fileLine.isComment)
                                continue
                        }
                    } while(line!=null)
                    parsingState.finishCurrentAlias()
                }
                val aliasSet=parsingState.getAliasSet()
                midiParsingErrors.addAll(parsingState.mErrors)
                return aliasSet
            } catch (ioe: IOException) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            }
        }
    }
}
