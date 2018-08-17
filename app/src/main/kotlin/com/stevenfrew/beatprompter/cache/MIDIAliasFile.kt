package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileLine
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cache.parse.SongParsingState
import com.stevenfrew.beatprompter.cache.parse.tag.MIDITag
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasInstructionTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasesTag
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
        mAliasSet = readAliasFile(mFile, mID, mErrors)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(element: Element) : super(element) {
        mAliasSet = readAliasFile(mFile, mID, mErrors)
    }

    override fun writeToXML(d: Document, element: Element) {
        val aliasFileElement = d.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME)
        super.writeToXML(aliasFileElement)
        element.appendChild(aliasFileElement)
    }

    companion object {
        const val MIDIALIASFILE_ELEMENT_TAG_NAME = "midialiases"

        @Throws(InvalidBeatPrompterFileException::class)
        private fun readAliasFile(file:File, filename: String, midiParsingErrors: MutableList<FileParseError>): AliasSet {
            val parsingState=SongParsingState()
            try {
                var currentAliasName: String? = null
                var currentAliasComponents: MutableList<AliasComponent> = mutableListOf()
                val aliases = ArrayList<Alias>()
                var aliasFilename: String? = null
                BufferedReader(InputStreamReader(FileInputStream(file))).use {
                    var line: String?
                    var lineNumber = 0
                    var isMidiAliasFile = false
                    do {
                        line = it.readLine()
                        if(line!=null) {
                            val fileLine= FileLine(line, ++lineNumber, file,parsingState)
                            if(fileLine.isComment)
                                continue

                            if (fileLine.isEmpty) {
                                if (currentAliasName != null) {
                                    aliases.add(Alias(currentAliasName!!, currentAliasComponents))
                                    currentAliasName = null
                                    currentAliasComponents = ArrayList()
                                }
                            }
                            if (!isMidiAliasFile) {
                                val aliasesTag= fileLine.mTags.filterIsInstance<MIDIAliasesTag>().firstOrNull()
                                        ?: throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
                                aliasFilename=aliasesTag.mAliasSetName
                                isMidiAliasFile = true
                            } else {
                                // OK, we have a line of content, and we're definitely in a MIDI alias file.
                                if (currentAliasName != null) {
                                    currentAliasComponents.addAll(fileLine.mTags.filterIsInstance<MIDIAliasInstructionTag>().map {tag->createAliasComponent(tag)})
                                } else {
                                    val aliasTag=fileLine.mTags.filterIsInstance<MIDIAliasTag>().firstOrNull()
                                    if (aliasTag != null)
                                        currentAliasName=aliasTag.mAliasName
                                }
                            }
                        }
                    } while(line!=null)
                }
                if (currentAliasName != null)
                    aliases.add(Alias(currentAliasName!!, currentAliasComponents))
                midiParsingErrors.addAll(parsingState.mErrors)
                return if (aliasFilename != null)
                    AliasSet(aliasFilename!!, aliases)
                else
                    throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            } catch (ioe: IOException) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            }
        }

        @Throws(MalformedTagException::class)
        fun createAliasComponent(tag:MIDIAliasInstructionTag):AliasComponent
        {
            val instructions=tag.mInstructions
            val name=tag.mName
            val componentArgs = ArrayList<Value>()
            val paramBits = instructions.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for ((paramCounter, paramBit) in paramBits.withIndex()) {
                val aliasValue = MIDITag.parseValue(paramBit, paramCounter, paramBits.size)
                componentArgs.add(aliasValue)
            }
            return if (name.equals("midi_send", ignoreCase = true))
                SimpleAliasComponent(componentArgs)
            else
                RecursiveAliasComponent(name, componentArgs)
        }
    }
}
