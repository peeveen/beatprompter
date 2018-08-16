package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.FileLine
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasTag
import com.stevenfrew.beatprompter.cache.parse.tag.midialias.MIDIAliasesTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.MIDIEventTag
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import com.stevenfrew.beatprompter.event.MIDIEvent
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
                            val fileLine= FileLine(line, ++lineNumber, file,midiParsingErrors)
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
                                    currentAliasComponents.addAll(fileLine.mTags.filterIsInstance<MIDIEventTag>().mapNotNull{createAliasComponent(it)})
                                    val component = parseAliasComponent(line, lineNumber, midiParsingErrors)
                                    if (component != null)
                                        currentAliasComponents.add(component)
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
                return if (aliasFilename != null)
                    AliasSet(aliasFilename!!, aliases)
                else
                    throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            } catch (ioe: IOException) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename))
            }
        }
    }

    @Throws(MalformedTagException::class)
    fun createAliasComponent(tag:MIDIEventTag):AliasComponent
    {
        val bracketStart = line.indexOf("{")
        if (bracketStart != -1) {
            val bracketEnd = line.indexOf("}", bracketStart)
            if (bracketEnd != -1) {
                val contents = line.substring(bracketStart + 1, bracketEnd - bracketStart).trim().toLowerCase()
                if (contents.isNotEmpty()) {
                    val bits = contents.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val componentArgs = ArrayList<Value>()
                    val tagName = bits[0].trim()
                    if (bits.size > 2)
                        throw MalformedTagException(this,BeatPrompterApplication.getResourceString(R.string.midi_alias_message_contains_more_than_two_parts))
                    else if (bits.size > 1) {
                        val params = bits[1].trim()
                        val paramBits = params.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        for ((paramCounter, paramBit) in paramBits.withIndex()) {
                            val aliasValue = parseValue(paramBit, paramCounter, paramBits.size)
                            componentArgs.add(aliasValue)
                        }
                    }

                    return if (tagName.equals("midi_send", ignoreCase = true))
                        SimpleAliasComponent(componentArgs)
                    else
                        RecursiveAliasComponent(tagName, componentArgs)
                } else
                    throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.empty_tag))
            } else
                throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag))
        } else
            throw MalformedTagException(this, BeatPrompterApplication.getResourceString(R.string.badly_formed_tag))
    }
}
